package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** SIP/HTTP authentication settings (mode + password). */
final class AuthenticationService(transport: Transport):

  /** Get the current auth mode + allowlist. */
  def get()(using ExecutionContext): Future[AuthGetData] =
    transport.request(Method.GET, "/v2.2/auth").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AuthGetData](p))
    }

  /** Set the auth mode and/or password. */
  def update(body: AuthPutRequest)(using ExecutionContext): Future[AuthPutData] =
    transport.request(Method.PUT, "/v2.2/auth", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AuthPutData](p))
    }
