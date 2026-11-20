package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** CNAM, carrier & line type, and LRN dips. */
final class LookupsService(transport: Transport):

  /** Perform a CNAM dip on `number` (10-digit TN). */
  def cnam(number: String)(using ExecutionContext): Future[CnamData] =
    transport.request(Method.GET, s"/v2.2/cnam/${Transport.encodePathSegment(number)}").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[CnamData](p))
    }

  /** Look up the carrier and line type of `number` (10-digit TN) — voip,
    * landline, mobile, ... Billed per lookup at the account's CNAM rate, only
    * when a result is returned.
    */
  def carrier(number: String)(using ExecutionContext): Future[CarrierData] =
    transport.request(Method.GET, s"/v2.2/carrier/${Transport.encodePathSegment(number)}").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[CarrierData](p))
    }

  /** Perform an LRN dip. `ani` is the presented ANI used for billing/auth. */
  def lrn(number: String, ani: String)(using ExecutionContext): Future[LrnLookupData] =
    transport.request(Method.GET, s"/v2.2/lrn/${Transport.encodePathSegment(number)}/${Transport.encodePathSegment(ani)}").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[LrnLookupData](p))
    }
