package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Operations on telephone numbers owned by the account. */
final class NumbersService(transport: Transport):

  def list()(using ExecutionContext): Future[NumbersListData] =
    transport.request(Method.GET, "/v2.2/numbers").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumbersListData](p))
    }

  def add(body: NumberAddRequest)(using ExecutionContext): Future[NumberAddData] =
    transport.request(Method.POST, "/v2.2/numbers", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberAddData](p))
    }

  def get(number: String)(using ExecutionContext): Future[NumberDetail] =
    transport.request(Method.GET, s"/v2.2/numbers/$number").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberDetail](p))
    }

  /** Detach a TN. Returns Unit on 204 No Content. */
  def remove(number: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/numbers/$number").map(_ => ())

  /** Transfer a TN to another account on the same authenticated org. */
  def move(number: String, body: NumberMoveRequest)(using
      ExecutionContext
  ): Future[NumberMoveData] =
    transport.request(Method.PATCH, s"/v2.2/numbers/$number", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberMoveData](p))
    }

  /** Return a TN to the network. Returns Unit on 204 No Content. */
  def release(number: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.POST, s"/v2.2/numbers/$number/release").map(_ => ())

  def setRoute(number: String, body: NumberRouteRequest)(using
      ExecutionContext
  ): Future[NumberRouteData] =
    transport.request(Method.PUT, s"/v2.2/numbers/$number/route", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberRouteData](p))
    }

  def setTranslation(number: String, body: NumberTranslationRequest)(using
      ExecutionContext
  ): Future[NumberTranslationData] =
    transport
      .request(Method.PUT, s"/v2.2/numbers/$number/translation", body = Some(body.asJson))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[NumberTranslationData](p)) }

  def setCnam(number: String, body: NumberCnamRequest)(using
      ExecutionContext
  ): Future[NumberCnamData] =
    transport.request(Method.PUT, s"/v2.2/numbers/$number/cnam", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberCnamData](p))
    }

  def setLidb(number: String, body: NumberLidbRequest)(using
      ExecutionContext
  ): Future[NumberLidbData] =
    transport.request(Method.PUT, s"/v2.2/numbers/$number/lidb", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberLidbData](p))
    }

  def getFax(number: String)(using ExecutionContext): Future[NumberFaxData] =
    transport.request(Method.GET, s"/v2.2/numbers/$number/fax").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberFaxData](p))
    }

  def setFax(number: String, body: NumberFaxRequest)(using
      ExecutionContext
  ): Future[NumberFaxData] =
    transport.request(Method.PUT, s"/v2.2/numbers/$number/fax", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberFaxData](p))
    }

  /** Disable fax-to-email. Returns Unit on 204 No Content. */
  def removeFax(number: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/numbers/$number/fax").map(_ => ())

  def setForward(number: String, body: NumberForwardRequest)(using
      ExecutionContext
  ): Future[NumberForwardData] =
    transport
      .request(Method.PUT, s"/v2.2/numbers/$number/forward", body = Some(body.asJson))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[NumberForwardData](p)) }

  /** Disable call forwarding. Returns Unit on 204 No Content. */
  def removeForward(number: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/numbers/$number/forward").map(_ => ())

  def getSms(number: String)(using ExecutionContext): Future[NumberSmsData] =
    transport.request(Method.GET, s"/v2.2/numbers/$number/sms").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberSmsData](p))
    }

  def setSms(number: String, body: NumberSmsRequest)(using
      ExecutionContext
  ): Future[NumberSmsData] =
    transport.request(Method.PUT, s"/v2.2/numbers/$number/sms", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumberSmsData](p))
    }

  /** Clear SMS routing. Returns Unit on 204 No Content. */
  def removeSms(number: String)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/numbers/$number/sms").map(_ => ())

  def getMessaging(number: String)(using ExecutionContext): Future[NumberMessagingState] =
    transport.request(Method.GET, s"/v2.2/numbers/$number/messaging").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberMessagingState](p))
    }

  def patchMessaging(number: String, body: NumberMessagingPatchRequest)(using
      ExecutionContext
  ): Future[NumberMessagingPatchData] =
    transport
      .request(Method.PATCH, s"/v2.2/numbers/$number/messaging", body = Some(body.asJson))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[NumberMessagingPatchData](p)) }

  def assignCampaign(number: String, body: NumberCampaignAssignRequest)(using
      ExecutionContext
  ): Future[NumberMessagingCampaignAssignData] =
    transport
      .request(Method.PUT, s"/v2.2/numbers/$number/messaging-campaign", body = Some(body.asJson))
      .flatMap { p =>
        Future.fromTry(Transport.decodeRequired[NumberMessagingCampaignAssignData](p))
      }

  /** Remove the campaign binding from a TN. Returns 200 with a body. */
  def unassignCampaign(number: String)(using
      ExecutionContext
  ): Future[NumberMessagingCampaignUnassignData] =
    transport.request(Method.DELETE, s"/v2.2/numbers/$number/messaging-campaign").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumberMessagingCampaignUnassignData](p))
    }

  /** Bulk remove the campaign binding from many TNs at once. Returns 200 with a body. */
  def bulkUnassignCampaign(numbers: List[String])(using
      ExecutionContext
  ): Future[NumbersMessagingCampaignUnassignData] =
    val body = BulkUnassignRequest(numbers).asJson
    transport.request(Method.DELETE, "/v2.2/numbers/messaging-campaign", body = Some(body)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[NumbersMessagingCampaignUnassignData](p))
    }

  def setPortOutPin(number: String, body: PortOutPinUpdateRequest)(using
      ExecutionContext
  ): Future[PortOutPinUpdateData] =
    transport
      .request(Method.PATCH, s"/v2.2/numbers/$number/port-out-pin", body = Some(body.asJson))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[PortOutPinUpdateData](p)) }
