package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.Json
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** SMS/MMS sending and 10DLC brand/campaign registration. */
final class MessagingService(transport: Transport):

  /** Fetch message history. opts.number is required; from/to bound the
    * (inclusive) Unix-timestamp range; `type` is sms (default), mms, or dlr.
    */
  def history(opts: HistoryOptions)(using
      ExecutionContext
  ): Future[MessageHistoryData] =
    val q = Seq(
      Some("number" -> opts.number),
      opts.from.map(v => "from" -> v.toString),
      opts.to.map(v => "to" -> v.toString),
      opts.`type`.map("type" -> _)
    ).flatten
    transport.request(Method.GET, "/v2.2/messages", query = q).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[MessageHistoryData](p))
    }

  /** Send an SMS or MMS. */
  def send(body: MessageSendRequest)(using ExecutionContext): Future[MessageSendData] =
    transport.request(Method.POST, "/v2.2/messages", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[MessageSendData](p))
    }

  /** Register a 10DLC brand with the campaign registry. */
  def createBrand(body: MessagingBrandCreateRequest)(using
      ExecutionContext
  ): Future[MessagingBrandCreateData] =
    transport.request(Method.POST, "/v2.2/messaging/brands", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[MessagingBrandCreateData](p))
    }

  /** Return current 10DLC campaign statuses. ids (required) are the campaign
    * ids to query — max 100, sent comma-separated.
    */
  def campaignStatus(ids: List[String])(using
      ExecutionContext
  ): Future[MessagingCampaignStatusData] =
    transport
      .request(Method.GET, "/v2.2/messaging/campaigns", query = Seq("ids" -> ids.mkString(",")))
      .flatMap { p =>
        Future.fromTry(Transport.decodeRequired[MessagingCampaignStatusData](p))
      }

  /** Register a 10DLC campaign with the carrier. */
  def createCampaign(body: MessagingCampaignCreateRequest)(using
      ExecutionContext
  ): Future[MessagingCampaignCreateData] =
    transport.request(Method.POST, "/v2.2/messaging/campaigns", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[MessagingCampaignCreateData](p))
    }

  /** Messaging state for the given numbers. numbers (required) is a list of
    * 10-digit TNs — max 1000, sent comma-separated. Numbers not on the
    * authenticated account are returned with onAccount=false.
    */
  def numbersState(numbers: List[String])(using
      ExecutionContext
  ): Future[NumbersMessagingListData] =
    val q = Seq("numbers" -> numbers.mkString(","))
    transport.request(Method.GET, "/v2.2/numbers/messaging", query = q).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumbersMessagingListData](p))
    }

  /** Account-wide messaging health overview — the same view as the portal Messaging
    * tab: summary counts (numbers, messaging on, healthy, needing attention, in
    * progress), campaigns with carrier-review progress, numbers needing attention
    * (each with a plain-language title, detail and the actions that fix it), 14 days
    * of delivery counts, and the alert-email setting. Updated hourly.
    *
    * The API spec defines no response schema for this endpoint, so the decoded JSON
    * is returned as a raw circe [[io.circe.Json]] rather than a typed model.
    */
  def health()(using ExecutionContext): Future[Json] =
    transport.request(Method.GET, "/v2.2/messaging/health").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[Json](p))
    }

  /** The alert-email setting: whether new messaging problems are emailed to the
    * account. The spec defines no response schema, so the raw JSON is returned.
    */
  def healthAlerts()(using ExecutionContext): Future[Json] =
    transport.request(Method.GET, "/v2.2/messaging/health/alerts").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[Json](p))
    }

  /** Turn alert emails on or off. When enabled, the account is emailed as new
    * messaging problems are found (at most one digest about every 20 hours). The spec
    * defines no response schema, so the raw JSON is returned.
    */
  def setHealthAlerts(enabled: Boolean)(using ExecutionContext): Future[Json] =
    transport
      .request(
        Method.PUT,
        "/v2.2/messaging/health/alerts",
        body = Some(Json.obj("enabled" -> enabled.asJson))
      )
      .flatMap { p => Future.fromTry(Transport.decodeRequired[Json](p)) }

  /** Live health check of one number: in service, messaging enabled, campaign
    * approved, assigned to the campaign by the messaging provider, delivery failures
    * in the last 24 hours, and enablement in progress. Limited to 30 checks per hour
    * per account. The spec defines no response schema, so the raw JSON is returned.
    */
  def healthCheck(number: String)(using ExecutionContext): Future[Json] =
    transport
      .request(
        Method.POST,
        "/v2.2/messaging/health/check",
        body = Some(Json.obj("number" -> number.asJson))
      )
      .flatMap { p => Future.fromTry(Transport.decodeRequired[Json](p)) }

  /** Up to 3 recent outbound messages from the number that carriers blocked as spam
    * (time, masked recipient, text, reason), to help review content. The spec defines
    * no response schema, so the raw JSON is returned.
    */
  def healthSamples(number: String)(using ExecutionContext): Future[Json] =
    transport
      .request(Method.GET, "/v2.2/messaging/health/samples", query = Seq("number" -> number))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[Json](p)) }
