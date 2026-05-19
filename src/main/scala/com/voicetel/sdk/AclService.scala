package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** IP allowlist management. */
final class AclService(transport: Transport):

  /** Return the current allowlist. */
  def list()(using ExecutionContext): Future[AclListData] =
    transport.request(Method.GET, "/v2.2/acl").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AclListData](p))
    }

  /** Append CIDR entries to the allowlist. */
  def add(body: AclModifyRequest)(using ExecutionContext): Future[AclAddData] =
    transport.request(Method.POST, "/v2.2/acl", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AclAddData](p))
    }

  /** Remove CIDR entries from the allowlist (returns 200 with a body). */
  def remove(body: AclModifyRequest)(using ExecutionContext): Future[AclRemoveData] =
    transport.request(Method.DELETE, "/v2.2/acl", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AclRemoveData](p))
    }
