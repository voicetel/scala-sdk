package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** e911 records and address validation.
  *
  * Note the asymmetric `dn` formats: requests take a 10-digit TN; responses
  * return the 11-digit E.164 US form (country code 1 prepended).
  */
final class E911Service(transport: Transport):

  /** Return every e911 record on the account. */
  def list()(using ExecutionContext): Future[E911AllData] =
    transport.request(Method.GET, "/v2.2/e911").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[E911AllData](p))
    }

  /** Validate + provision an e911 record in one call. */
  def create(body: E911CreateRequest)(using ExecutionContext): Future[E911RecordData] =
    transport.request(Method.POST, "/v2.2/e911", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[E911RecordData](p))
    }

  /** Validate an address, returning an addressId for use with provision. */
  def validate(body: E911AddressRequest)(using ExecutionContext): Future[E911ValidateData] =
    transport.request(Method.POST, "/v2.2/e911/validations", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[E911ValidateData](p))
    }

  /** Fetch the e911 record for `dn`. */
  def get(dn: String)(using ExecutionContext): Future[E911RecordData] =
    transport.request(Method.GET, s"/v2.2/e911/$dn").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[E911RecordData](p))
    }

  /** Use a previously-validated addressId to provision e911 for `dn`. */
  def provision(dn: String, body: E911ProvisionByIDRequest)(using
      ExecutionContext
  ): Future[E911RecordData] =
    transport.request(Method.PUT, s"/v2.2/e911/$dn", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[E911RecordData](p))
    }

  /** Delete the e911 record for `dn`. Returns Unit on 204 No Content. */
  def remove(dn: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/e911/$dn").map(_ => ())
