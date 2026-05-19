package com.voicetel.sdk

import com.voicetel.sdk.Models.AccountApiKeyData
import io.circe.Json
import sttp.client3.{HttpClientFutureBackend, SttpBackend}
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Configuration for VoiceTelClient.
  *
  * Construct with [[VoiceTelClient.apply]] or use the named-argument form.
  */
final case class VoiceTelClientConfig(
    baseURL: String = Version.DefaultBaseURL,
    apiKey: String = "",
    userAgent: String = Version.DefaultUserAgent,
    maxRetries: Int = 2,
    backend: Option[SttpBackend[Future, Any]] = None
)

/** Entry point for the VoiceTel API.
  *
  * Construct one with VoiceTelClient.apply and reach the API through its
  * resource fields — for example client.numbers.list().
  *
  * VoiceTelClient is thread-safe.
  */
final class VoiceTelClient private (
    val config: VoiceTelClientConfig,
    private val transport: Transport,
    private val ownsBackend: Boolean
):
  val account: AccountService               = AccountService(transport)
  val acl: AclService                       = AclService(transport)
  val authentication: AuthenticationService = AuthenticationService(transport)
  val e911: E911Service                     = E911Service(transport)
  val gateways: GatewaysService             = GatewaysService(transport)
  val iNumbering: INumberingService         = INumberingService(transport)
  val lookups: LookupsService               = LookupsService(transport)
  val messaging: MessagingService           = MessagingService(transport)
  val numbers: NumbersService               = NumbersService(transport)
  val support: SupportService               = SupportService(transport)

  /** Base URL this client is configured against. */
  def baseURL: String = transport.baseURL

  /** Currently installed bearer token. Empty before login. */
  def apiKey: String = transport.apiKey

  /** Exchange username + password for a 32-hex API key and install it.
    *
    * Counts against the 6 req/hour/IP rate limit shared by every `account`
    * endpoint (cdr, mrc, payments, registration, api-key).
    */
  def login(username: Int, password: String)(using
      ExecutionContext
  ): Future[String] =
    val body = Json.obj(
      "username" -> Json.fromInt(username),
      "password" -> Json.fromString(password)
    )
    transport
      .request(Method.POST, "/v2.2/account/api-key", body = Some(body), requireAuth = false)
      .flatMap { payload =>
        Future.fromTry(Transport.decodeRequired[AccountApiKeyData](payload)).map { data =>
          if data.apikey.isEmpty then
            throw ApiError(
              ErrorKind.Authentication,
              0,
              "api-key response did not contain data.apikey"
            )
          transport.setBearer(data.apikey)
          data.apikey
        }
      }

  /** Close the underlying HTTP backend if this client owns it. */
  def close(): Unit =
    if ownsBackend then
      try transport.backend.close()
      catch case _: Throwable => ()

object VoiceTelClient:
  /** Construct a VoiceTelClient with the supplied configuration. If no backend
    * is given, a default HttpClientFutureBackend is created and managed by the
    * client (released on `close()`).
    */
  def apply(config: VoiceTelClientConfig = VoiceTelClientConfig()): VoiceTelClient =
    val (backend, owns) = config.backend match
      case Some(b) => (b, false)
      case None    => (HttpClientFutureBackend(), true)
    val transport = Transport(
      baseURL = config.baseURL.stripSuffix("/"),
      backend = backend,
      userAgent = config.userAgent,
      maxRetries = config.maxRetries,
      initialApiKey = config.apiKey
    )
    new VoiceTelClient(config, transport, owns)

  /** Convenience: build a client with an explicit base URL and backend. Used in tests. */
  def withBackend(
      backend: SttpBackend[Future, Any],
      baseURL: String = Version.DefaultBaseURL,
      apiKey: String = "",
      maxRetries: Int = 2
  ): VoiceTelClient =
    apply(
      VoiceTelClientConfig(
        baseURL = baseURL,
        apiKey = apiKey,
        maxRetries = maxRetries,
        backend = Some(backend)
      )
    )
