package com.voicetel.sdk

import io.circe.{Decoder, Json, Printer}
import io.circe.parser.parse
import sttp.client3.*
import sttp.model.{Method, Uri}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}

/** A request body: JSON (the default) or already-encoded bytes with an
  * explicit content type — a multipart form built by [[Transport.Multipart]].
  */
private[sdk] sealed trait RequestBody:
  def contentType: String

private[sdk] object RequestBody:
  final case class JsonBody(json: Json) extends RequestBody:
    val contentType: String = "application/json"

  final case class RawBody(contentType: String, bytes: Array[Byte]) extends RequestBody

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
    attemptRequest(
      method,
      path,
      query,
      body.map(RequestBody.JsonBody(_)),
      idempotencyKey = None,
      requireAuth,
      decodeJsonSuccess
    )

  /** Perform an HTTP request whose body is already encoded — for example a
    * multipart form built by [[Transport.Multipart]]. The response is decoded
    * as a JSON envelope like [[request]].
    *
    * @param method         HTTP method
    * @param path           path beginning with `/`
    * @param contentType    full Content-Type header value, boundary included
    * @param bytes          the encoded body bytes
    * @param query          optional query params
    * @param idempotencyKey when set, replaces the auto-generated
    *                       Idempotency-Key and stays stable across retries
    * @param requireAuth    when false, skip the Authorization header
    * @param ec             ExecutionContext for Future composition
    */
  def requestWithRawBody(
      method: Method,
      path: String,
      contentType: String,
      bytes: Array[Byte],
      query: Seq[(String, String)] = Nil,
      idempotencyKey: Option[String] = None,
      requireAuth: Boolean = true
  )(using ec: ExecutionContext): Future[Option[Json]] =
    attemptRequest(
      method,
      path,
      query,
      Some(RequestBody.RawBody(contentType, bytes)),
      idempotencyKey,
      requireAuth,
      decodeJsonSuccess
    )

  /** Perform an HTTP request and return the raw response bytes — for binary
    * endpoints such as `GET /v2.2/faxes/{id}/document`. The body is returned
    * as-is (no envelope unwrapping); error responses still fail with an
    * ApiError carrying the parsed error envelope.
    *
    * @param method      HTTP method
    * @param path        path beginning with `/`
    * @param query       optional query params
    * @param requireAuth when false, skip the Authorization header
    * @param ec          ExecutionContext for Future composition
    */
  def requestBytes(
      method: Method,
      path: String,
      query: Seq[(String, String)] = Nil,
      requireAuth: Boolean = true
  )(using ec: ExecutionContext): Future[Array[Byte]] =
    attemptRequest(method, path, query, None, None, requireAuth, decodeRawSuccess)

  private def attemptRequest[A](
      method: Method,
      path: String,
      query: Seq[(String, String)],
      body: Option[RequestBody],
      idempotencyKey: Option[String],
      requireAuth: Boolean,
      decodeSuccessBody: (Int, Array[Byte]) => scala.util.Try[A]
  )(using ec: ExecutionContext): Future[A] =
    if requireAuth && apiKey.isEmpty then
      Future.failed(
        ApiError(
          ErrorKind.Authentication,
          0,
          "no api key set; call client.login or pass apiKey to VoiceTelClient"
        )
      )
    else
      Uri.parse(baseURL + path) match
        case Right(u) =>
          doAttempt(method, u.addParams(query*), body, idempotencyKey, requireAuth, attempt = 0, decodeSuccessBody)
        case Left(err) =>
          Future.failed(ApiError(ErrorKind.Unknown, 0, None, None, s"invalid URL: $err"))

  private def doAttempt[A](
      method: Method,
      uri: Uri,
      body: Option[RequestBody],
      idempotencyKeyOverride: Option[String],
      requireAuth: Boolean,
      attempt: Int,
      decodeSuccessBody: (Int, Array[Byte]) => scala.util.Try[A]
  )(using ec: ExecutionContext): Future[A] =
    val idempotencyKey =
      idempotencyKeyOverride.orElse(
        if method == Method.POST || method == Method.PUT || method == Method.PATCH then
          Some(java.util.UUID.randomUUID().toString)
        else None
      )

    var req = basicRequest
      .method(method, uri)
      .header("User-Agent", userAgent)
      .header("Accept", "application/json")
      .header("Accept-Encoding", "gzip")
      .response(asByteArrayAlways)

    body.foreach { rb =>
      req = req.header("Content-Type", rb.contentType)
      rb match
        case RequestBody.JsonBody(json) => req = req.body(jsonPrinter.print(json))
        case RequestBody.RawBody(_, bytes) => req = req.body(bytes)
    }
    if requireAuth then
      req = req.header("Authorization", s"Bearer $apiKey")
    idempotencyKey.foreach(k => req = req.header("Idempotency-Key", k))

    req.send(backend).flatMap { resp =>
      val rawBytes = decompressBytes(resp.body, resp.header("Content-Encoding"))
      val status   = resp.code.code
      if status >= 200 && status < 300 then
        Future.fromTry(decodeSuccessBody(status, rawBytes))
      else if retryableStatuses.contains(status) && attempt < maxRetries then
        val delay = backoffDelay(attempt, resp.header("Retry-After"))
        afterDelay(delay) {
          doAttempt(method, uri, body, idempotencyKeyOverride, requireAuth, attempt + 1, decodeSuccessBody)
        }
      else Future.failed(decodeError(status, new String(rawBytes, StandardCharsets.UTF_8)))
    }.recoverWith {
      case e: ApiError => Future.failed(e)
      case _: Throwable if attempt < maxRetries =>
        val delay = backoffDelay(attempt, None)
        afterDelay(delay) {
          doAttempt(method, uri, body, idempotencyKeyOverride, requireAuth, attempt + 1, decodeSuccessBody)
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

  private def decompressBytes(bytes: Array[Byte], encoding: Option[String]): Array[Byte] =
    if encoding.exists(_.equalsIgnoreCase("gzip")) then
      val gis = new GZIPInputStream(new ByteArrayInputStream(bytes))
      try gis.readAllBytes() finally gis.close()
    else bytes

  private def decodeJsonSuccess(status: Int, bytes: Array[Byte]): scala.util.Try[Option[Json]] =
    if bytes.isEmpty then scala.util.Success(None)
    else
      val raw = new String(bytes, StandardCharsets.UTF_8)
      parse(raw) match
        case Left(err) =>
          scala.util.Failure(
            ApiError(
              ErrorKind.Unknown,
              status,
              None,
              Some(raw),
              s"non-JSON response body (HTTP $status): ${err.getMessage}"
            )
          )
        case Right(json) =>
          scala.util.Success(Some(Transport.unwrapEnvelope(json)))

  private val decodeRawSuccess: (Int, Array[Byte]) => scala.util.Try[Array[Byte]] =
    (_, bytes) => scala.util.Success(bytes)

  private def decodeError(status: Int, raw: String): ApiError =
    val (code, message, bodyStr) = parse(raw) match
      case Right(json) =>
        val obj    = json.asObject
        val nested = obj.flatMap(_.apply("error")).flatMap(_.asObject)
        def asCode(j: Json): Option[String] =
          j.asString.orElse(j.asNumber.flatMap(_.toInt).map(_.toString))
        val c = nested.flatMap(_.apply("code")).flatMap(asCode).orElse(
          obj.flatMap(_.apply("code")).flatMap(asCode)
        ).orElse(
          obj.flatMap(_.apply("error")).flatMap(_.asString)
        )
        val m = nested.flatMap(_.apply("message")).flatMap(_.asString).orElse(
          obj.flatMap(_.apply("message")).flatMap(_.asString)
        ).orElse(
          obj.flatMap(_.apply("error")).flatMap(_.asString)
        )
        (c, m.getOrElse(s"HTTP $status"), Some(jsonPrinter.print(json)))
      case Left(_) =>
        (None, if raw.nonEmpty then raw else s"HTTP $status", Option(raw).filter(_.nonEmpty))
    ApiError(ErrorKind.fromStatus(status), status, code, bodyStr, message)

  private def backoffDelay(attempt: Int, retryAfter: Option[String]): FiniteDuration =
    retryAfter.flatMap(h => scala.util.Try(h.trim.toInt).toOption) match
      case Some(secs) if secs >= 0 =>
        val d = secs.seconds
        if d > 8.seconds then 8.seconds else d
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
  /** Percent-encode a single URL path segment. Uses form encoding with the
    * "+" (space) correction so the result is safe to embed in a path.
    */
  def encodePathSegment(s: String): String =
    java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

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

  /** Minimal RFC 7578 multipart/form-data encoder — the wire format for
    * `POST /v2.2/faxes`. Bodies are assembled by hand around an explicit
    * boundary; no third-party multipart machinery is pulled in.
    */
  private[sdk] object Multipart:

    /** One file part: form-field name, file name, media type, raw bytes. */
    final case class FilePart(
        name: String,
        filename: String,
        contentType: String,
        bytes: Array[Byte]
    )

    /** A fresh random boundary — ASCII letters, digits and dashes only, so it
      * is safe both in the Content-Type header and in the body.
      */
    def randomBoundary(): String =
      s"voicetel-${java.util.UUID.randomUUID().toString.replace("-", "")}"

    /** Encode text fields followed by file parts, closing the boundary.
      *
      * @return the full Content-Type header value (boundary included) and the body bytes
      */
    def build(
        fields: Seq[(String, String)],
        files: Seq[FilePart],
        boundary: String
    ): (String, Array[Byte]) =
      val out  = new java.io.ByteArrayOutputStream()
      val crlf = "\r\n".getBytes(StandardCharsets.US_ASCII)
      def ascii(s: String): Array[Byte] = s.getBytes(StandardCharsets.US_ASCII)
      def delimiter(): Unit =
        out.write("--".getBytes(StandardCharsets.US_ASCII))
        out.write(ascii(boundary))
        out.write(crlf)

      fields.foreach { (name, value) =>
        delimiter()
        out.write(ascii(s"""Content-Disposition: form-data; name="$name""""))
        out.write(crlf)
        out.write(crlf)
        out.write(value.getBytes(StandardCharsets.UTF_8))
        out.write(crlf)
      }
      files.foreach { p =>
        delimiter()
        out.write(ascii(s"""Content-Disposition: form-data; name="${p.name}"; filename="${sanitize(p.filename)}""""))
        out.write(crlf)
        out.write(ascii(s"Content-Type: ${p.contentType}"))
        out.write(crlf)
        out.write(crlf)
        out.write(p.bytes)
        out.write(crlf)
      }
      out.write("--".getBytes(StandardCharsets.US_ASCII))
      out.write(ascii(boundary))
      out.write("--".getBytes(StandardCharsets.US_ASCII))
      out.write(crlf)
      (s"multipart/form-data; boundary=$boundary", out.toByteArray)

    /** Keep the Content-Disposition line valid: strip quotes, backslashes and
      * line breaks from file names.
      */
    private def sanitize(filename: String): String =
      filename.replaceAll("[\"\\\\\r\n]", "_")

  end Multipart

  private val scheduler: java.util.concurrent.ScheduledExecutorService =
    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r =>
      val t = new Thread(r, "voicetel-retry-scheduler")
      t.setDaemon(true)
      t
    )
