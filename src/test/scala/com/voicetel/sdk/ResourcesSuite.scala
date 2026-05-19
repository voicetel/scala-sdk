package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import com.voicetel.sdk.TestSupport.*
import io.circe.Json
import sttp.client3.Response
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class ResourcesSuite extends munit.FunSuite:
  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private def await[A](f: Future[A]): A = Await.result(f, 5.seconds)

  // ---------------------------------------------------------------- Account ---

  test("Account.get decodes profile") {
    val data = AccountData(username = Some("1000000001"), name = Some("Test"))
    val c    = clientFor(stubOk(envelopeOf(data)))
    val r    = await(c.account.get())
    assertEquals(r.username, Some("1000000001"))
  }

  test("Account.update sends notify and decodes Updated list") {
    val resp = AccountPutData(updated = List("timezone"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r = await(c.account.update(AccountPutRequest(timezone = Some("UTC"), notifyEnabled = Some(true))))
    assertEquals(r.updated, List("timezone"))
  }

  test("Account.add returns AccountAddData") {
    val resp = AccountAddData(username = Some("1000000002"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.account.add(AccountAddRequest(2, "n", "e@x.com")))
    assertEquals(r.username, Some("1000000002"))
  }

  test("Account.signup returns AccountSignupData") {
    val resp = AccountSignupData(username = Some("X"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.account.signup(AccountSignupRequest("n", "e@x.com")))
    assertEquals(r.username, Some("X"))
  }

  test("Account.cdr accepts start/end and decodes") {
    val resp = AccountCdrData(cdr = Nil, start = 1, end = 2)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.account.cdr(Some(1), Some(2)))
    assertEquals(r.start, 1)
  }

  test("Account.credits decodes") {
    val resp = AccountCreditsData(credits = List(CreditEntry("2026-01-01", true, 12.34)))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.account.credits())
    assertEquals(r.credits.head.amount, 12.34)
  }

  test("Account.recurringCharges decodes") {
    val resp = AccountMrcData(charges = List(MrcCharge(1.0)), total = 1.0)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.account.recurringCharges()).total, 1.0)
  }

  test("Account.payments decodes") {
    val resp = AccountPaymentsData(payments = List(PaymentEntry(date = "x", status = "Completed", amount = 9.99)))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.account.payments()).payments.head.status, "Completed")
  }

  test("Account.registration decodes") {
    val resp = AccountRegistrationData(agent = Some("ua"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.account.registration()).agent, Some("ua"))
  }

  test("Account.recover decodes") {
    val resp = AccountRecoverData(message = Some("ok"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.account.recover(AccountRecoverRequest("e@x.com")))
    assertEquals(r.message, Some("ok"))
  }

  // ---------------------------------------------------------------- ACL ---

  test("ACL.list decodes") {
    val resp = AclListData(acl = List(CidrEntry("203.0.113.0/24")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.acl.list()).acl.head.cidr, "203.0.113.0/24")
  }

  test("ACL.add decodes") {
    val resp = AclAddData(added = List(CidrEntry("203.0.113.0/24")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.acl.add(AclModifyRequest(List(CidrEntry("203.0.113.0/24")))))
    assertEquals(r.added.size, 1)
  }

  test("ACL.remove decodes (200 with body)") {
    val resp = AclRemoveData(removed = List(CidrEntry("203.0.113.0/24")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.acl.remove(AclModifyRequest(List(CidrEntry("203.0.113.0/24")))))
    assertEquals(r.removed.size, 1)
  }

  // -------------------------------------------------------- Authentication ---

  test("Authentication.get decodes") {
    val resp = AuthGetData(authType = 1, authTypeDescription = "IP Auth", acl = Nil)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.authentication.get()).authType, 1)
  }

  test("Authentication.update decodes") {
    val resp = AuthPutData(updated = List(AuthUpdatedEntry("authType", Some(1))))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.authentication.update(AuthPutRequest(authType = Some(1))))
    assertEquals(r.updated.head.field, "authType")
  }

  // ---------------------------------------------------------------- e911 ---

  test("E911.list decodes") {
    val resp = E911AllData(records = Nil)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.e911.list()).records, Nil)
  }

  test("E911.create decodes") {
    val resp = E911RecordData(E911Entry("12015551234", "ACME", "1 Main", None, "C", "NJ", "07601"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.create(E911CreateRequest("2015551234", "ACME", "1 Main", None, "C", "NJ", "07601")))
    assertEquals(r.record.dn, "12015551234")
  }

  test("E911.validate decodes") {
    val resp = E911ValidateData(E911ValidatedAddress(1, "1 Main", None, "C", "NJ", "07601"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.validate(E911AddressRequest("1 Main", None, "C", "NJ", "07601")))
    assertEquals(r.address.addressid, 1)
  }

  test("E911.get decodes") {
    val resp = E911RecordData(E911Entry("12015551234", "ACME", "1 Main", None, "C", "NJ", "07601"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.get("2015551234"))
    assertEquals(r.record.callername, "ACME")
  }

  test("E911.provision decodes") {
    val resp = E911RecordData(E911Entry("12015551234", "ACME", "1 Main", None, "C", "NJ", "07601"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.provision("2015551234", E911ProvisionByIDRequest("ACME", 1)))
    assertEquals(r.record.dn, "12015551234")
  }

  test("E911.remove returns Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.e911.remove("2015551234"))
  }

  // ---------------------------------------------------------------- gateways ---

  test("Gateways.list decodes") {
    val resp = GatewaysListData(gateways = List(GatewayEntry(id = Some(1), gateway = Some("1.2.3.4"))))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.gateways.list()).gateways.head.id, Some(1))
  }

  test("Gateways.add decodes") {
    val resp = GatewayEntry(id = Some(2), gateway = Some("1.2.3.4"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.gateways.add(GatewayAddRequest("1.2.3.4")))
    assertEquals(r.id, Some(2))
  }

  test("Gateways.get decodes") {
    val resp = GatewayEntry(id = Some(2), gateway = Some("1.2.3.4"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.gateways.get(2)).id, Some(2))
  }

  test("Gateways.update decodes") {
    val resp = GatewayEntry(id = Some(2), prefix = Some("9"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.gateways.update(2, GatewayUpdateRequest(prefix = Some("9"))))
    assertEquals(r.prefix, Some("9"))
  }

  test("Gateways.remove returns Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.gateways.remove(2))
  }

  test("Gateways.numbers decodes") {
    val resp = GatewayNumbersData(numbers = List(
      GatewayNumberSummary("2015551234", "2015551234", false, None, false, 0, false, false)
    ))
    val c = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.gateways.numbers(2)).numbers.size, 1)
  }

  // ---------------------------------------------------------------- iNumbering ---

  test("INumbering.searchInventory decodes and serializes query") {
    val resp = InventorySearchData(numbers = List(InventoryItem("2015551234", "RC", "C", "NJ", "224")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r = await(c.iNumbering.searchInventory(InventoryQuery(npa = Some(201), state = Some("NJ"))))
    assertEquals(r.numbers.head.number, "2015551234")
  }

  test("INumbering.coverage decodes (default query)") {
    val resp = InventoryCoverageData(coverage = List(InventoryCoverageItem(count = 5, npa = Some("201"))))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.iNumbering.coverage())
    assertEquals(r.coverage.head.count, 5)
  }

  test("INumbering.order decodes with mixed Value/Spec entries") {
    val resp = OrderCreateData(orderId = "O1", amountCharged = 1.0, numbersOrdered = List("2015551234"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r = await(
      c.iNumbering.order(
        OrderCreateRequest(
          List(
            OrderNumber.Value("2015551234"),
            OrderNumber.Spec(OrderNumberSpec("2015551235", Some(4)))
          )
        )
      )
    )
    assertEquals(r.orderId, "O1")
  }

  test("INumbering.ports decodes") {
    val resp = PortListData(ports = List(PortSummary("submitted", id = Some("1"))))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.iNumbering.ports()).ports.head.status, "submitted")
  }

  test("INumbering.port decodes") {
    val resp = PortDetailData(PortDetail("submitted", id = Some("42")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.iNumbering.port(42)).port.id, Some("42"))
  }

  test("INumbering.submitPort decodes") {
    val resp = PortSubmitData(pid = "ABCDE", ticket = 1, message = "ok", loaUrl = "u", portUrl = "u")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r = await(
      c.iNumbering.submitPort(
        PortSubmitRequest(
          did = List("2015551234"), name = "X", nameType = "business",
          lcBtn = "1", lcAccountNumber = "1", streetNumber = "1",
          street = "M", streetType = "ST", city = "C", state = "IL",
          zip = "60601", country = "US", authPerson = "J"
        )
      )
    )
    assertEquals(r.pid, "ABCDE")
  }

  test("INumbering.portAvailability decodes v2.2.10 fields") {
    val resp = PortAvailabilityData(
      number = "2017301000",
      portable = true,
      localRoutingNumber = Some("2012548000"),
      rateCenterTier = Some("tier1")
    )
    val c = clientFor(stubOk(envelopeOf(resp)))
    val r = await(c.iNumbering.portAvailability("2017301000"))
    assertEquals(r.localRoutingNumber, Some("2012548000"))
    assertEquals(r.rateCenterTier, Some("tier1"))
  }

  // ---------------------------------------------------------------- lookups ---

  test("Lookups.cnam decodes") {
    val resp = CnamData(cnam = Some("ACME"), number = "2015551234")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.lookups.cnam("2015551234")).cnam, Some("ACME"))
  }

  test("Lookups.lrn decodes") {
    val resp = LrnLookupData(ani = "2015551234", destination = "2012548000", lrn = LrnData(lrn = Some("2012548000")))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.lookups.lrn("2012548000", "2015551234")).lrn.lrn, Some("2012548000"))
  }

  // ---------------------------------------------------------------- messaging ---

  test("Messaging.history decodes") {
    val resp = MessageHistoryData("2015551234", "sms", 1, 2, Nil)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.messaging.history(HistoryOptions(number = Some("2015551234"), `type` = Some("sms"))))
    assertEquals(r.`type`, "sms")
  }

  test("Messaging.send uses wire fields fromNumber/toNumber") {
    val resp = MessageSendData("msgid", "sms", "2012548000", "2015551234", 1)
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches { req =>
        val body = req.body match
          case sttp.client3.StringBody(s, _, _) => s
          case _                                 => ""
        body.contains("\"fromNumber\":\"2012548000\"") &&
        body.contains("\"toNumber\":\"2015551234\"")
      }
      .thenRespond(envelopeOf(resp))
    val c = clientFor(backend)
    val r = await(c.messaging.send(MessageSendRequest("2012548000", "2015551234", "hi")))
    assertEquals(r.fromNumber, "2012548000")
  }

  test("Messaging.createBrand decodes") {
    val resp = MessagingBrandCreateData(BrandRegistrationResult("200", "Success"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.messaging.createBrand(MessagingBrandCreateRequest("BABC", "ACME")))
    assertEquals(r.result.status, "Success")
  }

  test("Messaging.campaignStatus decodes") {
    val resp = MessagingCampaignStatusData(campaigns = Nil)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.messaging.campaignStatus()).campaigns, Nil)
  }

  test("Messaging.createCampaign decodes") {
    val resp = MessagingCampaignCreateData(CampaignRegistrationResult("200", "Success"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.messaging.createCampaign(MessagingCampaignCreateRequest("B", "C", "desc")))
    assertEquals(r.result.statusCode, "200")
  }

  test("Messaging.numbersState decodes") {
    val resp = NumbersMessagingListData(numbers = Nil)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.messaging.numbersState()).numbers, Nil)
    assertEquals(await(c.messaging.numbersState(List("2015551234"))).numbers, Nil)
  }

  // ---------------------------------------------------------------- numbers ---

  private val numDetail = NumberDetail(
    number = "2015551234", translated = "2015551234", route = 4,
    gateway = None, cnam = false, forward = false, forwardTo = None,
    carrier = 0, smsEnabled = false, faxEnabled = false
  )

  test("Numbers.list decodes") {
    val resp = NumbersListData(numbers = List(numDetail))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.numbers.list()).numbers.head.number, "2015551234")
  }

  test("Numbers.add decodes") {
    val resp = NumberAddData("2015551234", 4)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.numbers.add(NumberAddRequest("2015551234"))).route, 4)
  }

  test("Numbers.get decodes") {
    val c = clientFor(stubOk(envelopeOf(numDetail)))
    assertEquals(await(c.numbers.get("2015551234")).number, "2015551234")
  }

  test("Numbers.remove and .release return Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.numbers.remove("2015551234"))
    await(c.numbers.release("2015551234"))
  }

  test("Numbers.move decodes") {
    val resp = NumberMoveData("2015551234", 1, 4)
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.numbers.move("2015551234", NumberMoveRequest(1, 4))).accountId, 1)
  }

  test("Numbers route/translation/cnam/lidb setters") {
    val rRoute = NumberRouteData("2015551234", 7)
    val rTr    = NumberTranslationData("2015551234", "2015551235")
    val rCnam  = NumberCnamData("2015551234", true)
    val rLidb  = NumberLidbData("2015551234", "ACME", "ref", "Success")
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches(_.uri.path.lastOption.contains("route")).thenRespond(envelopeOf(rRoute))
      .whenRequestMatches(_.uri.path.lastOption.contains("translation")).thenRespond(envelopeOf(rTr))
      .whenRequestMatches(_.uri.path.lastOption.contains("cnam")).thenRespond(envelopeOf(rCnam))
      .whenRequestMatches(_.uri.path.lastOption.contains("lidb")).thenRespond(envelopeOf(rLidb))
    val c = clientFor(backend)
    assertEquals(await(c.numbers.setRoute("2015551234", NumberRouteRequest(7))).route, 7)
    assertEquals(await(c.numbers.setTranslation("2015551234", NumberTranslationRequest("2015551235"))).translation, "2015551235")
    assertEquals(await(c.numbers.setCnam("2015551234", NumberCnamRequest(true))).cnam, true)
    assertEquals(await(c.numbers.setLidb("2015551234", NumberLidbRequest("ACME"))).cnam, "ACME")
  }

  test("Numbers fax getter and setter") {
    val resp    = NumberFaxData("2015551234", "f@x.com")
    val backend = stubOk(envelopeOf(resp))
    val c       = clientFor(backend)
    assertEquals(await(c.numbers.getFax("2015551234")).email, "f@x.com")
    assertEquals(await(c.numbers.setFax("2015551234", NumberFaxRequest("f@x.com"))).email, "f@x.com")
  }

  test("Numbers fax/forward/sms DELETE return Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.numbers.removeFax("2015551234"))
    await(c.numbers.removeForward("2015551234"))
    await(c.numbers.removeSms("2015551234"))
  }

  test("Numbers.setForward decodes") {
    val resp = NumberForwardData("2015551234", Some("2125551234"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.numbers.setForward("2015551234", NumberForwardRequest("2125551234")))
    assertEquals(r.forwardTo, Some("2125551234"))
  }

  test("Numbers SMS get/set") {
    val resp = NumberSmsData("2015551234", "email", "f@x.com")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.numbers.getSms("2015551234")).resource, "f@x.com")
    assertEquals(await(c.numbers.setSms("2015551234", NumberSmsRequest("email", "f@x.com"))).resource, "f@x.com")
  }

  test("Numbers.getMessaging and patchMessaging") {
    val state = NumberMessagingState("2015551234", None, true, 17, 1, "r", None, None)
    val patch = NumberMessagingPatchData("2015551234", List("routeIn"))
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches(_.method == sttp.model.Method.GET).thenRespond(envelopeOf(state))
      .whenRequestMatches(_.method == sttp.model.Method.PATCH).thenRespond(envelopeOf(patch))
    val c = clientFor(backend)
    assertEquals(await(c.numbers.getMessaging("2015551234")).carrier, 17)
    assertEquals(await(c.numbers.patchMessaging("2015551234", NumberMessagingPatchRequest(routeIn = Some(1)))).updated, List("routeIn"))
  }

  test("Numbers campaign assign") {
    val assign = NumberMessagingCampaignAssignData("2015551234", "C1", 17, Some("A"), Some("X"), None, false)
    val c      = clientFor(stubOk(envelopeOf(assign)))
    assertEquals(
      await(c.numbers.assignCampaign("2015551234", NumberCampaignAssignRequest("C1"))).campaignId,
      "C1"
    )
  }

  test("Numbers campaign unassign (single)") {
    val unassign = NumberMessagingCampaignUnassignData("2015551234", "C1", Some("A"), Some("X"), true)
    val c        = clientFor(stubOk(envelopeOf(unassign)))
    assertEquals(await(c.numbers.unassignCampaign("2015551234")).unassigned, true)
  }

  test("Numbers campaign bulk unassign") {
    val bulk = NumbersMessagingCampaignUnassignData("C1", Some("A"), Some("X"), List("2015551234"))
    val c    = clientFor(stubOk(envelopeOf(bulk)))
    assertEquals(
      await(c.numbers.bulkUnassignCampaign(List("2015551234"))).unassignedNumbers,
      List("2015551234")
    )
  }

  test("Numbers.setPortOutPin decodes") {
    val resp = PortOutPinUpdateData("2015551234", "1234")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.numbers.setPortOutPin("2015551234", PortOutPinUpdateRequest("1234"))).portOutPin, "1234")
  }

  // ---------------------------------------------------------------- support ---

  private val ticket = SupportConversation(
    id = Some(1), ticketNumber = Some(1015), status = "active", subject = Some("hi")
  )

  test("Support.list decodes ticketNumber from wire 'number' field") {
    val raw = envelope(Json.obj("tickets" -> Json.arr(
      Json.obj(
        "id"     -> Json.fromInt(1),
        "number" -> Json.fromInt(1015),
        "status" -> Json.fromString("active")
      )
    )))
    val c = clientFor(stubOk(raw))
    val r = await(c.support.list())
    assertEquals(r.tickets.head.ticketNumber, Some(1015))
  }

  test("Support.create decodes") {
    val c = clientFor(stubOk(envelopeOf(TicketData(ticket))))
    val r = await(c.support.create(TicketCreateRequest("s", "m")))
    assertEquals(r.ticket.ticketNumber, Some(1015))
  }

  test("Support.get decodes") {
    val c = clientFor(stubOk(envelopeOf(TicketData(ticket))))
    assertEquals(await(c.support.get(1)).ticket.id, Some(1))
  }

  test("Support.update decodes") {
    val resp = TicketUpdateData(id = Some(1), status = "success")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.support.update(1, TicketUpdateRequest("closed"))).status, "success")
  }

  test("Support.delete returns Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest.thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.support.delete(1))
  }

  test("Support.messages decodes") {
    val resp = TicketThreadsData(messages = List(SupportThread(status = "active", body = Some("hello"))))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.support.messages(1)).messages.head.body, Some("hello"))
  }

  test("Support.reply decodes") {
    val resp = TicketReplyData("Reply added")
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.support.reply(1, TicketReplyRequest("ok"))).message, "Reply added")
  }

  // ---------------------------------------------------------------- error path ---

  test("every method propagates server errors") {
    val backend = stubStatus(500, errorBody)
    val c       = clientFor(backend)
    val acl     = AclModifyRequest(List(CidrEntry("203.0.113.0/24")))
    val numReq  = NumberAddRequest("2015551234")
    def shouldFail(f: Future[?], name: String): Unit =
      val t = intercept[ApiError](await(f))
      assertEquals(t.kind, ErrorKind.Server, name)
    // account
    shouldFail(c.account.get(), "account.get")
    shouldFail(c.account.update(AccountPutRequest(timezone = Some("UTC"))), "account.update")
    shouldFail(c.account.add(AccountAddRequest(1, "n", "e@x.com")), "account.add")
    shouldFail(c.account.signup(AccountSignupRequest("n", "e@x.com")), "account.signup")
    shouldFail(c.account.cdr(Some(1), Some(2)), "account.cdr")
    shouldFail(c.account.credits(), "account.credits")
    shouldFail(c.account.recurringCharges(), "account.recurringCharges")
    shouldFail(c.account.payments(), "account.payments")
    shouldFail(c.account.registration(), "account.registration")
    shouldFail(c.account.recover(AccountRecoverRequest("e@x.com")), "account.recover")
    // acl
    shouldFail(c.acl.list(), "acl.list")
    shouldFail(c.acl.add(acl), "acl.add")
    shouldFail(c.acl.remove(acl), "acl.remove")
    // auth
    shouldFail(c.authentication.get(), "auth.get")
    shouldFail(c.authentication.update(AuthPutRequest(authType = Some(1))), "auth.update")
    // e911
    shouldFail(c.e911.list(), "e911.list")
    shouldFail(c.e911.create(E911CreateRequest("2015551234", "x", "1", None, "C", "NJ", "07601")), "e911.create")
    shouldFail(c.e911.validate(E911AddressRequest("1", None, "C", "NJ", "07601")), "e911.validate")
    shouldFail(c.e911.get("2015551234"), "e911.get")
    shouldFail(c.e911.provision("2015551234", E911ProvisionByIDRequest("x", 1)), "e911.provision")
    shouldFail(c.e911.remove("2015551234"), "e911.remove")
    // gateways
    shouldFail(c.gateways.list(), "gateways.list")
    shouldFail(c.gateways.add(GatewayAddRequest("1.2.3.4")), "gateways.add")
    shouldFail(c.gateways.get(1), "gateways.get")
    shouldFail(c.gateways.update(1, GatewayUpdateRequest(prefix = Some("9"))), "gateways.update")
    shouldFail(c.gateways.remove(1), "gateways.remove")
    shouldFail(c.gateways.numbers(1), "gateways.numbers")
    // lookups
    shouldFail(c.lookups.cnam("2015551234"), "lookups.cnam")
    shouldFail(c.lookups.lrn("2012548000", "2015551234"), "lookups.lrn")
    // messaging
    shouldFail(c.messaging.history(HistoryOptions()), "messaging.history")
    shouldFail(c.messaging.send(MessageSendRequest("2012548000", "2015551234", "hi")), "messaging.send")
    shouldFail(c.messaging.createBrand(MessagingBrandCreateRequest("BABC", "X")), "messaging.createBrand")
    shouldFail(c.messaging.campaignStatus(), "messaging.campaignStatus")
    shouldFail(c.messaging.createCampaign(MessagingCampaignCreateRequest("B", "C", "d")), "messaging.createCampaign")
    shouldFail(c.messaging.numbersState(), "messaging.numbersState")
    // numbers
    shouldFail(c.numbers.list(), "numbers.list")
    shouldFail(c.numbers.add(numReq), "numbers.add")
    shouldFail(c.numbers.get("2015551234"), "numbers.get")
    shouldFail(c.numbers.remove("2015551234"), "numbers.remove")
    shouldFail(c.numbers.move("2015551234", NumberMoveRequest(1, 4)), "numbers.move")
    shouldFail(c.numbers.release("2015551234"), "numbers.release")
    shouldFail(c.numbers.setRoute("2015551234", NumberRouteRequest(7)), "numbers.setRoute")
    shouldFail(c.numbers.setTranslation("2015551234", NumberTranslationRequest("2015551235")), "numbers.setTranslation")
    shouldFail(c.numbers.setCnam("2015551234", NumberCnamRequest(true)), "numbers.setCnam")
    shouldFail(c.numbers.setLidb("2015551234", NumberLidbRequest("ACME")), "numbers.setLidb")
    shouldFail(c.numbers.getFax("2015551234"), "numbers.getFax")
    shouldFail(c.numbers.setFax("2015551234", NumberFaxRequest("f@x.com")), "numbers.setFax")
    shouldFail(c.numbers.removeFax("2015551234"), "numbers.removeFax")
    shouldFail(c.numbers.setForward("2015551234", NumberForwardRequest("2125551234")), "numbers.setForward")
    shouldFail(c.numbers.removeForward("2015551234"), "numbers.removeForward")
    shouldFail(c.numbers.getSms("2015551234"), "numbers.getSms")
    shouldFail(c.numbers.setSms("2015551234", NumberSmsRequest("email", "f@x.com")), "numbers.setSms")
    shouldFail(c.numbers.removeSms("2015551234"), "numbers.removeSms")
    shouldFail(c.numbers.getMessaging("2015551234"), "numbers.getMessaging")
    shouldFail(c.numbers.patchMessaging("2015551234", NumberMessagingPatchRequest(routeIn = Some(1))), "numbers.patchMessaging")
    shouldFail(c.numbers.assignCampaign("2015551234", NumberCampaignAssignRequest("C1")), "numbers.assignCampaign")
    shouldFail(c.numbers.unassignCampaign("2015551234"), "numbers.unassignCampaign")
    shouldFail(c.numbers.bulkUnassignCampaign(List("2015551234")), "numbers.bulkUnassignCampaign")
    shouldFail(c.numbers.setPortOutPin("2015551234", PortOutPinUpdateRequest("1234")), "numbers.setPortOutPin")
    // iNumbering
    shouldFail(c.iNumbering.searchInventory(InventoryQuery(state = Some("NJ"))), "iNumbering.searchInventory")
    shouldFail(c.iNumbering.coverage(), "iNumbering.coverage")
    shouldFail(c.iNumbering.order(OrderCreateRequest(List(OrderNumber.Value("2015551234")))), "iNumbering.order")
    shouldFail(c.iNumbering.ports(), "iNumbering.ports")
    shouldFail(c.iNumbering.port(1), "iNumbering.port")
    shouldFail(
      c.iNumbering.submitPort(
        PortSubmitRequest(
          did = List("2015551234"), name = "X", nameType = "business",
          lcBtn = "1", lcAccountNumber = "1", streetNumber = "1",
          street = "M", streetType = "ST", city = "C", state = "IL",
          zip = "60601", country = "US", authPerson = "J"
        )
      ), "iNumbering.submitPort"
    )
    shouldFail(c.iNumbering.portAvailability("2015551234"), "iNumbering.portAvailability")
    // support
    shouldFail(c.support.list(), "support.list")
    shouldFail(c.support.create(TicketCreateRequest("s", "m")), "support.create")
    shouldFail(c.support.get(1), "support.get")
    shouldFail(c.support.update(1, TicketUpdateRequest("closed")), "support.update")
    shouldFail(c.support.delete(1), "support.delete")
    shouldFail(c.support.messages(1), "support.messages")
    shouldFail(c.support.reply(1, TicketReplyRequest("ok")), "support.reply")
  }
end ResourcesSuite
