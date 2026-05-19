package com.voicetel.sdk

import com.voicetel.sdk.Models.CoverageQuery

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

/** Live read-only checks against api.voicetel.com.
  *
  * Gated by the VOICETEL_USERNAME / VOICETEL_PASSWORD env vars; if either is
  * unset, every test in this suite is skipped via munit.assume.
  *
  * Strict rules:
  *   - No state mutations. No POST/PUT/PATCH/DELETE.
  *   - Login + cdr + recurring-charges + payments + registration all share a
  *     6/hr/IP rate limit. Run sparingly.
  */
class IntegrationSuite extends munit.FunSuite:

  given ExecutionContext = ExecutionContext.global

  private def envOk: Boolean =
    sys.env.get("VOICETEL_USERNAME").exists(_.nonEmpty) &&
      sys.env.get("VOICETEL_PASSWORD").exists(_.nonEmpty)

  override def munitTimeout: Duration = 30.seconds

  private def buildClient(): VoiceTelClient =
    val user = sys.env("VOICETEL_USERNAME").toInt
    val pass = sys.env("VOICETEL_PASSWORD")
    val base = sys.env.getOrElse("VOICETEL_BASE_URL", Version.DefaultBaseURL)
    val c    = VoiceTelClient(VoiceTelClientConfig(baseURL = base))
    Await.result(c.login(user, pass), 15.seconds)
    c

  test("Account.get returns a username".tag(IntegrationTag)) {
    assume(envOk, "set VOICETEL_USERNAME and VOICETEL_PASSWORD to run integration tests")
    val c = buildClient()
    try
      val me = Await.result(c.account.get(), 15.seconds)
      assert(me.username.exists(_.nonEmpty))
    finally c.close()
  }

  test("read-only lists do not blow up".tag(IntegrationTag)) {
    assume(envOk, "set VOICETEL_USERNAME and VOICETEL_PASSWORD to run integration tests")
    val c = buildClient()
    try
      val tasks: List[(String, Future[?])] = List(
        "numbers.list"           -> c.numbers.list(),
        "gateways.list"          -> c.gateways.list(),
        "acl.list"               -> c.acl.list(),
        "e911.list"              -> c.e911.list(),
        "support.list"           -> c.support.list(),
        "iNumbering.coverage"    -> c.iNumbering.coverage(CoverageQuery()),
        "iNumbering.ports"       -> c.iNumbering.ports(),
        "authentication.get"     -> c.authentication.get(),
        "messaging.campaignStatus" -> c.messaging.campaignStatus()
      )
      tasks.foreach { case (label, f) =>
        try Await.result(f, 15.seconds)
        catch case t: Throwable => fail(s"$label failed: $t")
      }
    finally c.close()
  }

end IntegrationSuite

/** Marker tag for integration tests. Run only with the env vars set. */
object IntegrationTag extends munit.Tag("integration")
