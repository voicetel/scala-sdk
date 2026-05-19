package com.voicetel.sdk

/** ErrorKind classifies a VoiceTel API error so callers can switch on it
  * without having to inspect HTTP status codes.
  */
enum ErrorKind:
  /** Catch-all for unmapped statuses or transport failures. */
  case Unknown
  /** HTTP 400, server-side validation failure. */
  case BadRequest
  /** HTTP 401, bearer token missing, expired, or invalid. */
  case Authentication
  /** HTTP 403, authenticated but not allowed. */
  case PermissionDenied
  /** HTTP 404, resource does not exist. */
  case NotFound
  /** HTTP 409, request conflicts with current state. */
  case Conflict
  /** HTTP 429, exceeded the 6/hour/IP cap on account endpoints. */
  case RateLimit
  /** Any HTTP 5xx. */
  case Server

object ErrorKind:
  /** Map an HTTP status code to a kind. */
  def fromStatus(status: Int): ErrorKind = status match
    case 400              => BadRequest
    case 401              => Authentication
    case 403              => PermissionDenied
    case 404              => NotFound
    case 409              => Conflict
    case 429              => RateLimit
    case s if s >= 500 && s < 600 => Server
    case _                => Unknown

/** ApiError is thrown whenever the VoiceTel API responds with a non-2xx status,
  * or when the transport itself fails (in which case statusCode is 0).
  *
  * The body field preserves the raw response payload as a JSON string — useful
  * for 409 conflicts where the server returns structured detail about partial
  * successes (see AclConflictData and AuthPutConflictData).
  */
final class ApiError(
    val kind: ErrorKind,
    val statusCode: Int,
    val code: Option[String],
    val body: Option[String],
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause):

  def this(kind: ErrorKind, statusCode: Int, message: String) =
    this(kind, statusCode, None, None, message, null)

  def this(kind: ErrorKind, statusCode: Int, code: Option[String], body: Option[String], message: String) =
    this(kind, statusCode, code, body, message, null)

  override def toString: String =
    val codeStr = code.map(c => s" $c").getOrElse("")
    s"voicetel: HTTP $statusCode$codeStr: $message"

object ApiError:
  /** Returns true when the error is an ApiError with kind RateLimit. */
  def isRateLimit(t: Throwable): Boolean = kindOf(t) == ErrorKind.RateLimit

  /** Returns true when the error is an ApiError with kind NotFound. */
  def isNotFound(t: Throwable): Boolean = kindOf(t) == ErrorKind.NotFound

  /** Returns true when the error is an ApiError with kind Authentication. */
  def isAuthentication(t: Throwable): Boolean = kindOf(t) == ErrorKind.Authentication

  /** Returns true when the error is an ApiError with kind Conflict. */
  def isConflict(t: Throwable): Boolean = kindOf(t) == ErrorKind.Conflict

  private def kindOf(t: Throwable): ErrorKind = t match
    case e: ApiError => e.kind
    case _           => ErrorKind.Unknown
