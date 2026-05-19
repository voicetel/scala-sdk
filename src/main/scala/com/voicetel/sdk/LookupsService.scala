package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** CNAM and LRN dips. */
final class LookupsService(transport: Transport):

  /** Perform a CNAM dip on `number` (10-digit TN). */
  def cnam(number: String)(using ExecutionContext): Future[CnamData] =
    transport.request(Method.GET, s"/v2.2/cnam/$number").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[CnamData](p))
    }

  /** Perform an LRN dip. `ani` is the presented ANI used for billing/auth. */
  def lrn(number: String, ani: String)(using ExecutionContext): Future[LrnLookupData] =
    transport.request(Method.GET, s"/v2.2/lrn/$number/$ani").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[LrnLookupData](p))
    }
