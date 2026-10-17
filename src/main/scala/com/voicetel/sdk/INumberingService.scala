package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Inventory searches, orders, and port-ins. */
final class INumberingService(transport: Transport):

  /** Search available TNs by tnMask prefix/wildcard plus optional filters. */
  def searchInventory(q: InventoryQuery)(using
      ExecutionContext
  ): Future[InventorySearchData] =
    val params = Seq(
      Some("tnMask" -> q.tnMask),
      q.quantity.map(v => "quantity" -> v.toString),
      q.province.map(v => "province" -> v),
      q.rateCenter.map(v => "rateCenter" -> v),
      q.lata.map(v => "lata" -> v.toString)
    ).flatten
    transport.request(Method.GET, "/v2.2/inventory", query = params).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[InventorySearchData](p))
    }

  /** Aggregated availability buckets, grouped by q.countBy
    * (state | city | rateCenter | npanxx | block).
    */
  def coverage(q: CoverageQuery)(using
      ExecutionContext
  ): Future[InventoryCoverageData] =
    val params = Seq(
      Some("countBy" -> q.countBy),
      q.locState.map(v => "locState" -> v),
      q.city.map(v => "city" -> v),
      q.rcAbbre.map(v => "rcAbbre" -> v),
      q.npa.map(v => "npa" -> v),
      q.lata.map(v => "lata" -> v.toString)
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

  /** Fetch detail for one port-in by its 5-character alphanumeric pid. */
  def port(id: String)(using ExecutionContext): Future[PortDetailData] =
    transport.request(Method.GET, s"/v2.2/ports/${Transport.encodePathSegment(id)}").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortDetailData](p))
    }

  /** Submit a port-in order. */
  def submitPort(body: PortSubmitRequest)(using ExecutionContext): Future[PortSubmitData] =
    transport.request(Method.POST, "/v2.2/ports", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[PortSubmitData](p))
    }

  /** Check whether a given TN can be ported in. */
  def portAvailability(number: String)(using ExecutionContext): Future[PortAvailabilityData] =
    transport
      .request(Method.GET, s"/v2.2/ports/availability/${Transport.encodePathSegment(number)}")
      .flatMap { p =>
        Future.fromTry(Transport.decodeRequired[PortAvailabilityData](p))
      }
