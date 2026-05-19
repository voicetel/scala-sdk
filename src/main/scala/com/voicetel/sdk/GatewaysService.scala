package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Outbound termination gateways. */
final class GatewaysService(transport: Transport):

  def list()(using ExecutionContext): Future[GatewaysListData] =
    transport.request(Method.GET, "/v2.2/gateways").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[GatewaysListData](p))
    }

  def add(body: GatewayAddRequest)(using ExecutionContext): Future[GatewayEntry] =
    transport.request(Method.POST, "/v2.2/gateways", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[GatewayEntry](p))
    }

  def get(id: Int)(using ExecutionContext): Future[GatewayEntry] =
    transport.request(Method.GET, s"/v2.2/gateways/$id").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[GatewayEntry](p))
    }

  def update(id: Int, body: GatewayUpdateRequest)(using
      ExecutionContext
  ): Future[GatewayEntry] =
    transport.request(Method.PUT, s"/v2.2/gateways/$id", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[GatewayEntry](p))
    }

  /** Delete a gateway. Returns Unit on 204 No Content. */
  def remove(id: Int)(using ExecutionContext): Future[Unit] =
    transport.request(Method.DELETE, s"/v2.2/gateways/$id").map(_ => ())

  def numbers(id: Int)(using ExecutionContext): Future[GatewayNumbersData] =
    transport.request(Method.GET, s"/v2.2/gateways/$id/numbers").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[GatewayNumbersData](p))
    }
