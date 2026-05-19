package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** SMS/MMS sending and 10DLC brand/campaign registration. */
final class MessagingService(transport: Transport):

  /** Fetch message history. */
  def history(opts: HistoryOptions = HistoryOptions())(using
      ExecutionContext
  ): Future[MessageHistoryData] =
    val q = Seq(
      opts.number.map("number" -> _),
      opts.start.map(v => "start" -> v.toString),
      opts.end.map(v => "end" -> v.toString),
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

  /** Return current 10DLC campaign statuses. */
  def campaignStatus()(using ExecutionContext): Future[MessagingCampaignStatusData] =
    transport.request(Method.GET, "/v2.2/messaging/campaigns").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[MessagingCampaignStatusData](p))
    }

  /** Register a 10DLC campaign with the carrier. */
  def createCampaign(body: MessagingCampaignCreateRequest)(using
      ExecutionContext
  ): Future[MessagingCampaignCreateData] =
    transport.request(Method.POST, "/v2.2/messaging/campaigns", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[MessagingCampaignCreateData](p))
    }

  /** Messaging state for many numbers at once. Empty list = all numbers. */
  def numbersState(numbers: List[String] = Nil)(using
      ExecutionContext
  ): Future[NumbersMessagingListData] =
    val q = if numbers.isEmpty then Nil else Seq("numbers" -> numbers.mkString(","))
    transport.request(Method.GET, "/v2.2/numbers/messaging", query = q).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[NumbersMessagingListData](p))
    }
