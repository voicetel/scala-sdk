package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import sttp.model.Method

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/** One document attached to an outbound fax: file name, raw bytes and media
  * type.
  *
  * The server validates content (magic bytes), not the extension — PDF, DOC
  * and DOCX are accepted, at most 8 MiB each and 8 MiB per fax.
  */
final case class FaxDocument(name: String, content: Array[Byte], contentType: String = "application/pdf")
object FaxDocument:
  /** A PDF document with the standard media type. */
  def pdf(name: String, content: Array[Byte]): FaxDocument =
    FaxDocument(name, content, "application/pdf")

/** Sending faxes from the account's fax numbers and reading their history
  * and documents.
  */
final class FaxService(transport: Transport):

  /** Every fax-capable number on the account, with the email address each
    * delivers to (destinationId 0 = delivers to the account email).
    */
  def numbers()(using ExecutionContext): Future[FaxNumbersData] =
    transport.request(Method.GET, "/v2.2/numbers/fax").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[FaxNumbersData](p))
    }

  /** Submit a fax for delivery (multipart form, HTTP 201, status queued).
    *
    * idempotencyKey is a client-generated UUID: resubmitting the same
    * documents with the same key returns the original job instead of sending
    * twice. It is generated when omitted — pass your own to make retries safe
    * across process restarts. Rate-limited to 10 sends per minute per
    * account.
    */
  def send(
      fromNumber: String,
      toNumber: String,
      documents: List[FaxDocument],
      idempotencyKey: Option[String] = None
  )(using ExecutionContext): Future[FaxSendData] =
    val key       = idempotencyKey.getOrElse(UUID.randomUUID().toString)
    val files     = documents.map(d =>
      Transport.Multipart.FilePart("documents", d.name, d.contentType, d.content)
    )
    val (contentType, body) = Transport.Multipart.build(
      fields = Seq("fromNumber" -> fromNumber, "toNumber" -> toNumber),
      files = files,
      boundary = Transport.Multipart.randomBoundary()
    )
    transport
      .requestWithRawBody(
        Method.POST,
        "/v2.2/faxes",
        contentType,
        body,
        idempotencyKey = Some(key)
      )
      .flatMap { p => Future.fromTry(Transport.decodeRequired[FaxSendData](p)) }

  /** One page of fax history for a number, newest first. opts.number is
    * required; opts.from/opts.to are unix seconds (from is the newer edge,
    * defaulting to the last 24 hours).
    */
  def history(opts: FaxHistoryOptions)(using ExecutionContext): Future[FaxHistoryData] =
    val q = Seq(
      Some("number" -> opts.number),
      opts.from.map(v => "from" -> v.toString),
      opts.to.map(v => "to" -> v.toString),
      opts.limit.map(v => "limit" -> v.toString),
      opts.cursor.map("cursor" -> _),
      opts.status.map("status" -> _)
    ).flatten
    transport.request(Method.GET, "/v2.2/faxes", query = q).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[FaxHistoryData](p))
    }

  /** One fax job — poll until status is sent or failed. */
  def get(id: String)(using ExecutionContext): Future[FaxJob] =
    transport
      .request(Method.GET, s"/v2.2/faxes/${Transport.encodePathSegment(id)}")
      .flatMap { p => Future.fromTry(Transport.decodeRequired[FaxJob](p)) }

  /** The PDF rendering of a fax as raw bytes (an attachment). Available when
    * the job reports documentAvailable true.
    */
  def document(id: String)(using ExecutionContext): Future[Array[Byte]] =
    transport
      .requestBytes(Method.GET, s"/v2.2/faxes/${Transport.encodePathSegment(id)}/document")
