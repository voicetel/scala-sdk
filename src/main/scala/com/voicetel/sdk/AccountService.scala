package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.syntax.*
import sttp.model.Method

import scala.concurrent.{ExecutionContext, Future}

/** Operations under the Account tag.
  *
  * Note: cdr, recurringCharges, payments, registration, and Client.login share
  * a 6 req/hour/IP rate limit. Bursting will trigger 429s.
  */
final class AccountService(transport: Transport):

  /** Return the authenticated account's profile. */
  def get()(using ExecutionContext): Future[AccountData] =
    transport.request(Method.GET, "/v2.2/account").flatMap { payload =>
      Future.fromTry(Transport.decodeRequired[AccountData](payload))
    }

  /** Partial-update account settings. Only set fields are sent. */
  def update(body: AccountPutRequest)(using ExecutionContext): Future[AccountPutData] =
    transport.request(Method.PUT, "/v2.2/account", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountPutData](p))
    }

  /** Create a sub-account. Admin only. */
  def add(body: AccountAddRequest)(using ExecutionContext): Future[AccountAddData] =
    transport.request(Method.POST, "/v2.2/account", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountAddData](p))
    }

  /** Public sign-up flow. */
  def signup(body: AccountSignupRequest)(using ExecutionContext): Future[AccountSignupData] =
    transport.request(Method.POST, "/v2.2/accounts", body = Some(body.asJson)).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountSignupData](p))
    }

  /** Fetch call detail records. Rate-limited (6/hr/IP). */
  def cdr(start: Option[Int] = None, end: Option[Int] = None)(using
      ExecutionContext
  ): Future[AccountCdrData] =
    val q = Seq(
      start.map(s => "start" -> s.toString),
      end.map(e => "end" -> e.toString)
    ).flatten
    transport.request(Method.GET, "/v2.2/account/cdr", query = q).flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountCdrData](p))
    }

  /** Return credit history, newest first. */
  def credits()(using ExecutionContext): Future[AccountCreditsData] =
    transport.request(Method.GET, "/v2.2/account/credits").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountCreditsData](p))
    }

  /** Return active monthly-recurring charges. Rate-limited. */
  def recurringCharges()(using ExecutionContext): Future[AccountMrcData] =
    transport.request(Method.GET, "/v2.2/account/recurring-charges").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountMrcData](p))
    }

  /** Return payment history, newest first. Rate-limited. */
  def payments()(using ExecutionContext): Future[AccountPaymentsData] =
    transport.request(Method.GET, "/v2.2/account/payments").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountPaymentsData](p))
    }

  /** Current SIP registration. Rate-limited. */
  def registration()(using ExecutionContext): Future[AccountRegistrationData] =
    transport.request(Method.GET, "/v2.2/account/registration").flatMap { p =>
      Future.fromTry(Transport.decodeRequired[AccountRegistrationData](p))
    }

  /** Start the password recovery flow. No auth required. */
  def recover(body: AccountRecoverRequest)(using ExecutionContext): Future[AccountRecoverData] =
    transport
      .request(Method.POST, "/v2.2/account/recovery", body = Some(body.asJson), requireAuth = false)
      .flatMap { p => Future.fromTry(Transport.decodeRequired[AccountRecoverData](p)) }
