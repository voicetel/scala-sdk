package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Support tickets (create, read, update, delete, reply). */
final class SupportService(transport: Transport):

  def list()(using ExecutionContext): Future[TicketsListData] =
    transport.request(Method.GET, "/v2.2/support/tickets").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[TicketsListData](p))
    }

  def create(body: TicketCreateRequest)(using ExecutionContext): Future[TicketData] =
    transport.request(Method.POST, "/v2.2/support/tickets", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[TicketData](p))
    }

  def get(id: Int)(using ExecutionContext): Future[TicketData] =
    transport.request(Method.GET, s"/v2.2/support/tickets/$id").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[TicketData](p))
    }

  def update(id: Int, body: TicketUpdateRequest)(using
      ExecutionContext
  ): Future[TicketUpdateData] =
    transport.request(Method.PUT, s"/v2.2/support/tickets/$id", body = Some(body.asJson)).flatMap {
      p => Future.fromTry(Transport.decodeRequired[TicketUpdateData](p))
    }

  /** Delete a ticket. Admin only. Returns Unit on 204 No Content. */
  def delete(id: Int)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/support/tickets/$id").map(_ => ())

  def messages(id: Int)(using ExecutionContext): Future[TicketThreadsData] =
    transport.request(Method.GET, s"/v2.2/support/tickets/$id/messages").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[TicketThreadsData](p))
    }

  def reply(id: Int, body: TicketReplyRequest)(using
      ExecutionContext
  ): Future[TicketReplyData] =
    transport
      .request(Method.POST, s"/v2.2/support/tickets/$id/replies", body = Some(body.asJson))
      .flatMap { p => Future.fromTry(Transport.decodeRequired[TicketReplyData](p)) }
