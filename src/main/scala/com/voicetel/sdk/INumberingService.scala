package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Inventory searches, orders, and port-ins. */
final class INumberingService(transport: Transport):

  /** Search available TNs by NPA/NXX/state/rate-center/etc. */
  def searchInventory(q: InventoryQuery)(using
      ExecutionContext
  ): Future[InventorySearchData] =
    val params = Seq(
      q.npa.map(v => "npa" -> v.toString),
      q.nxx.map(v => "nxx" -> v.toString),
      q.state.map(v => "state" -> v),
      q.rateCenter.map(v => "ratecenter" -> v),
      q.contains.map(v => "contains" -> v),
      q.endsWith.map(v => "endswith" -> v),
      q.limit.map(v => "limit" -> v.toString)
    ).flatten
    transport.request(Method.GET, "/v2.2/inventory", query = params).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[InventorySearchData](p))
    }

  /** Aggregated availability buckets. */
  def coverage(q: CoverageQuery = CoverageQuery())(using
      ExecutionContext
  ): Future[InventoryCoverageData] =
    val params = Seq(
      q.state.map(v => "state" -> v),
      q.rateCenter.map(v => "ratecenter" -> v)
    ).flatten
    transport.request(Method.GET, "/v2.2/inventory/coverage", query = params).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[InventoryCoverageData](p))
    }

  /** Purchase new TNs. */
  def order(body: OrderCreateRequest)(using ExecutionContext): Future[OrderCreateData] =
    transport.request(Method.POST, "/v2.2/orders", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[OrderCreateData](p))
    }

  /** List every port-in record on the account. */
  def ports()(using ExecutionContext): Future[PortListData] =
    transport.request(Method.GET, "/v2.2/ports").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortListData](p))
    }

  /** Fetch detail for one port-in by id. */
  def port(id: Int)(using ExecutionContext): Future[PortDetailData] =
    transport.request(Method.GET, s"/v2.2/ports/$id").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortDetailData](p))
    }

  /** Submit a port-in order. */
  def submitPort(body: PortSubmitRequest)(using ExecutionContext): Future[PortSubmitData] =
    transport.request(Method.POST, "/v2.2/ports", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortSubmitData](p))
    }

  /** Check whether a given TN can be ported in. */
  def portAvailability(number: String)(using ExecutionContext): Future[PortAvailabilityData] =
    transport.request(Method.GET, s"/v2.2/ports/availability/$number").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortAvailabilityData](p))
    }
