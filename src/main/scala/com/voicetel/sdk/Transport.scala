package com.voicetel.sdk

import io.circe.{Decoder, Json, Printer}
import io.circe.parser.parse
import sttp.client3.*
import sttp.model.{Method, Uri}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}

/** Low-level HTTP transport used by every resource service.
  *
  * @param baseURL    base API URL with no trailing slash
  * @param backend    sttp backend whose effect type is `Future`
  * @param userAgent  User-Agent header value
  * @param maxRetries number of 429/5xx retries (total attempts = maxRetries + 1)
  */
final class Transport(
    val baseURL: String,
    val backend: SttpBackend[Future, Any],
    val userAgent: String,
    val maxRetries: Int,
    initialApiKey: String
):
  private val apiKeyRef: AtomicReference[String] = new AtomicReference[String](initialApiKey)

  /** Returns the currently installed bearer token. Empty string before login. */
  def apiKey: String = apiKeyRef.get()

  /** Installs a new bearer token. Used by Client.login. */
  def setBearer(key: String): Unit = apiKeyRef.set(key)

  private val retryableStatuses: Set[Int] = Set(429, 500, 502, 503, 504)
  private val jsonPrinter: Printer        = Printer.noSpaces.copy(dropNullValues = true)

  /** Perform an HTTP request and decode the response.
    *
    * @param method      HTTP method
    * @param path        path beginning with `/`
    * @param query       optional query params as a sequence of (name, value) pairs
    * @param body        optional JSON body (already encoded to a circe `Json`)
    * @param requireAuth when false, skip the Authorization header
    * @param ec          ExecutionContext for Future composition
    * @return a Future resolving to the inner data payload as raw JSON, or failing with ApiError
    */
  def request(
      method: Method,
      path: String,
      query: Seq[(String, String)] = Nil,
      body: Option[Json] = None,
      requireAuth: Boolean = true
  )(using ec: ExecutionContext): Future[Option[Json]] =
    if requireAuth && apiKey.isEmpty then
      Future.failed(
        ApiError(
          ErrorKind.Authentication,
          0,
          "no api key set; call client.login or pass apiKey to VoiceTelClient"
        )
      )
    else
      val parsed = Uri.parse(baseURL + path) match
        case Right(u) => u
        case Left(err) =>
          return Future.failed(
            ApiError(ErrorKind.Unknown, 0, None, None, s"invalid URL: $err")
          )
      val uri = parsed.addParams(query*)
      doAttempt(method, uri, body, requireAuth, attempt = 0)

  private def doAttempt(
      method: Method,
      uri: Uri,
      body: Option[Json],
      requireAuth: Boolean,
      attempt: Int
  )(using ec: ExecutionContext): Future[Option[Json]] =
    var req = basicRequest
      .method(method, uri)
      .header("User-Agent", userAgent)
      .header("Accept", "application/json")
      .response(asStringAlways)

    body.foreach { j =>
      req = req
        .header("Content-Type", "application/json")
        .body(jsonPrinter.print(j))
    }
    if requireAuth then
      req = req.header("Authorization", s"Bearer $apiKey")

    req.send(backend).flatMap { resp =>
      val status = resp.code.code
      if status >= 200 && status < 300 then
        Future.fromTry(decodeSuccess(status, resp.body))
      else if retryableStatuses.contains(status) && attempt < maxRetries then
        val delay = backoffDelay(attempt, resp.header("Retry-After"))
        afterDelay(delay) {
          doAttempt(method, uri, body, requireAuth, attempt + 1)
        }
      else Future.failed(decodeError(status, resp.body))
    }.recoverWith {
      case e: ApiError => Future.failed(e)
      case t: Throwable if attempt < maxRetries =>
        val delay = backoffDelay(attempt, None)
        afterDelay(delay) {
          doAttempt(method, uri, body, requireAuth, attempt + 1)
        }
      case t: Throwable =>
        Future.failed(
          ApiError(
            ErrorKind.Unknown,
            0,
            None,
            None,
            s"transport error after ${attempt + 1} attempt(s): ${t.getMessage}",
            t
          )
        )
    }

  private def decodeSuccess(@annotation.unused status: Int, raw: String): scala.util.Try[Option[Json]] =
    if raw == null || raw.isEmpty then scala.util.Success(None)
    else
      parse(raw) match
        case Left(err) =>
          // Non-JSON 2xx body — treat as no payload.
          scala.util.Success(None)
        case Right(json) =>
          scala.util.Success(Some(Transport.unwrapEnvelope(json)))

  private def decodeError(status: Int, raw: String): ApiError =
    val (code, message, bodyStr) = parse(raw) match
      case Right(json) =>
        val obj = json.asObject
        val c = obj.flatMap(_.apply("code")).flatMap(_.asString).orElse(
          obj.flatMap(_.apply("error")).flatMap(_.asString)
        )
        val m = obj.flatMap(_.apply("message")).flatMap(_.asString).orElse(
          obj.flatMap(_.apply("error")).flatMap(_.asString)
        )
        (c, m.getOrElse(s"HTTP $status"), Some(jsonPrinter.print(json)))
      case Left(_) =>
        (None, if raw.nonEmpty then raw else s"HTTP $status", Option(raw).filter(_.nonEmpty))
    ApiError(ErrorKind.fromStatus(status), status, code, bodyStr, message)

  private def backoffDelay(attempt: Int, retryAfter: Option[String]): FiniteDuration =
    retryAfter.flatMap(h => scala.util.Try(h.trim.toInt).toOption) match
      case Some(secs) if secs >= 0 => secs.seconds
      case _ =>
        val base = 500.millis
        val d    = base * (1L << attempt)
        if d > 8.seconds then 8.seconds else d

  private def afterDelay[A](delay: FiniteDuration)(thunk: => Future[A])(using
      @annotation.unused ec: ExecutionContext
  ): Future[A] =
    if delay <= Duration.Zero then thunk
    else
      val p = Promise[A]()
      Transport.scheduler.schedule(
        new Runnable {
          override def run(): Unit = p.completeWith(thunk)
        },
        delay.toMillis,
        java.util.concurrent.TimeUnit.MILLISECONDS
      )
      p.future

object Transport:
  /** Strip the `{"status":"success","data":...}` envelope when present. */
  def unwrapEnvelope(json: Json): Json =
    json.asObject match
      case Some(obj) if obj.contains("status") && obj.contains("data") =>
        obj("data").getOrElse(json)
      case _ => json

  /** Decode an unwrapped JSON value into A. Wraps decoding failures in ApiError. */
  def decode[A: Decoder](json: Json): scala.util.Try[A] =
    json.as[A] match
      case Right(a) => scala.util.Success(a)
      case Left(err) =>
        scala.util.Failure(
          ApiError(
            ErrorKind.Unknown,
            0,
            None,
            Some(json.noSpaces),
            s"decode response body: ${err.getMessage}"
          )
        )

  /** Decode an Option[Json] from Transport.request into A, requiring a payload. */
  def decodeRequired[A: Decoder](payload: Option[Json]): scala.util.Try[A] =
    payload match
      case Some(json) => decode[A](json)
      case None =>
        scala.util.Failure(
          ApiError(
            ErrorKind.Unknown,
            0,
            None,
            None,
            "expected JSON response body, got empty"
          )
        )

  private val scheduler: java.util.concurrent.ScheduledExecutorService =
    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r =>
      val t = new Thread(r, "voicetel-retry-scheduler")
      t.setDaemon(true)
      t
    )
