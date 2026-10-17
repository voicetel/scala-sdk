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
    val raw =
      """{"status":"success","data":{"username":"1000000001","name":"Test","email":"e@x.com",""" +
        """"enabled":true,"created":"2024-01-01T00:00:00Z","cash":12.5,"callerId":"2015550123",""" +
        """"timezone":"UTC","authType":0,"ccs":1,"notify":true,"notifyThreshold":5,""" +
        """"rates":{"sms":0.005},"services":{"sms":true}}}"""
    val c = clientFor(stubOk(raw))
    val r = await(c.account.get())
    assertEquals(r.username, Some("1000000001"))
    assertEquals(r.notifyEnabled, Some(true))
    assertEquals(r.rates.flatMap(_.sms), Some(0.005))
  }

  test("Account.update sends notify and decodes Updated list") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val c    = clientFor(recordingStub(reqs, """{"status":"success","data":{"updated":["timezone"]}}"""))
    val r = await(c.account.update(AccountPutRequest(timezone = Some("UTC"), notifyEnabled = Some(true))))
    assertEquals(r.updated, List("timezone"))
    val sent = reqs.head.body match
      case sttp.client3.StringBody(s, _, _) => s
      case _                                => ""
    assert(sent.contains("\"notify\":true"), sent)
  }

  test("Account.add sends username as a string and decodes AccountAddData") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"username":"1000000002","name":"n","email":"e@x.com",""" +
        """"password":"pw","masterAccount":"1000000001"}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.account.add(AccountAddRequest("1000000002", "n", "e@x.com", Some("1000000001"))))
    assertEquals(r.username, Some("1000000002"))
    val sent = reqs.head.body match
      case sttp.client3.StringBody(s, _, _) => s
      case _                                => ""
    assert(sent.contains("\"username\":\"1000000002\""), sent)
    assert(sent.contains("\"masterAccount\":\"1000000001\""), sent)
  }

  test("Account.signup is public (no Authorization header) and decodes") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"username":"1000000003","password":"pw","name":"n","email":"e@x.com"}}"""
    val c = unauthClient(recordingStub(reqs, raw))
    val r = await(c.account.signup(AccountSignupRequest("n", "e@x.com")))
    assertEquals(r.username, Some("1000000003"))
    assertEquals(reqs.head.header("Authorization"), None)
  }

  test("Account.cdr accepts start/end and decodes") {
    val raw =
      """{"status":"success","data":{"cdr":[{"id":"x","key":["1000000001","1747345200"],""" +
        """"value":{"dur":"40","dst":"2025550106","ba":"0.017500"}}],"start":1,"end":2}}"""
    val c = clientFor(stubOk(raw))
    val r = await(c.account.cdr(Some(1), Some(2)))
    assertEquals(r.start, Some(1))
    assertEquals(r.cdr.head.value.dur, Some("40"))
  }

  test("Account.credits decodes and sends the optional status filter") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"credits":[{"paid":true,"date":"2026-01-01","amount":12.34}]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.account.credits(Some("unpaid")))
    assertEquals(r.credits.head.amount, 12.34)
    assertEquals(reqs.head.uri.paramsMap.get("status"), Some("unpaid"))
  }

  test("Account.recurringCharges decodes") {
    val raw =
      """{"status":"success","data":{"charges":[{"amount":1.0,"description":"d"}],"total":1.0}}"""
    val c = clientFor(stubOk(raw))
    assertEquals(await(c.account.recurringCharges()).total, Some(1.0))
  }

  test("Account.payments decodes") {
    val raw =
      """{"status":"success","data":{"payments":[{"amount":9.99,"date":"2024-09-30T12:52:59+00:00",""" +
        """"status":"Completed","transactionId":"2N045992R140725E3","payerEmail":"e@x.com"}]}}"""
    val c = clientFor(stubOk(raw))
    assertEquals(await(c.account.payments()).payments.head.status, "Completed")
  }

  test("Account.registration decodes") {
    val raw =
      """{"status":"success","data":{"agent":"Zoiper rv2.8.97/","uri":"sip:4592086960@192.0.2.1:5060"}}"""
    val c = clientFor(stubOk(raw))
    assertEquals(await(c.account.registration()).agent, Some("Zoiper rv2.8.97/"))
  }

  test("Account.recover decodes") {
    val c = clientFor(stubOk("""{"status":"success","data":{"message":"ok"}}"""))
    val r = await(c.account.recover(AccountRecoverRequest("e@x.com")))
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

  test("ACL.remove sends cidrs query param, no body (200 with body)") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw  = """{"status":"success","data":{"removed":[{"cidr":"203.0.113.0/24"}]}}"""
    val c    = clientFor(recordingStub(reqs, raw))
    val r    = await(c.acl.remove(List("203.0.113.0/24", "203.0.113.1/32")))
    assertEquals(r.removed.size, 1)
    assertEquals(reqs.head.method, sttp.model.Method.DELETE)
    assertEquals(reqs.head.uri.paramsMap.get("cidrs"), Some("203.0.113.0/24,203.0.113.1/32"))
    assertEquals(reqs.head.body, sttp.client3.NoBody)
  }

  // -------------------------------------------------------- Authentication ---

  test("Authentication.get decodes") {
    val raw =
      """{"status":"success","data":{"authType":1,"authTypeDescription":"IP Auth","acl":[{"cidr":"203.0.113.0/24"}]}}"""
    val c = clientFor(stubOk(raw))
    assertEquals(await(c.authentication.get()).authType, Some(1))
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
    val resp = E911RecordData(E911Entry("12015550123", "ACME", "1 Main", None, "C", "NJ", "07082"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.create(E911CreateRequest("2015550123", "ACME", "1 Main", None, "C", "NJ", "07082")))
    assertEquals(r.record.dn, "12015550123")
  }

  test("E911.validate decodes") {
    val resp = E911ValidateData(E911ValidatedAddress(1, "1 Main", None, "C", "NJ", "07082"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.validate(E911AddressRequest("1 Main", None, "C", "NJ", "07082")))
    assertEquals(r.address.addressid, 1)
  }

  test("E911.get decodes") {
    val resp = E911RecordData(E911Entry("12015550123", "ACME", "1 Main", None, "C", "NJ", "07082"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.get("2015550123"))
    assertEquals(r.record.callername, "ACME")
  }

  test("E911.provision decodes") {
    val resp = E911RecordData(E911Entry("12015550123", "ACME", "1 Main", None, "C", "NJ", "07082"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    val r    = await(c.e911.provision("2015550123", E911ProvisionByIDRequest("ACME", 1)))
    assertEquals(r.record.dn, "12015550123")
  }

  test("E911.remove returns Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.e911.remove("2015550123"))
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
      GatewayNumberSummary("2015550123", "2015550123", false, None, false, 0, false, false)
    ))
    val c = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.gateways.numbers(2)).numbers.size, 1)
  }

  // ---------------------------------------------------------------- iNumbering ---

  test("INumbering.searchInventory sends spec query params and decodes") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"numbers":[{"number":"2015550103","rateCenter":"CLOSTER",""" +
        """"city":"Closter","province":"NJ","lata":"224"}]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(
      c.iNumbering.searchInventory(
        InventoryQuery(
          tnMask = "201*",
          quantity = Some(10),
          province = Some("NJ"),
          rateCenter = Some("CLOSTER"),
          lata = Some("224")
        )
      )
    )
    assertEquals(r.numbers.head.number, Some("2015550103"))
    assertEquals(r.numbers.head.rateCenter, Some("CLOSTER"))
    val params = reqs.head.uri.paramsMap
    assertEquals(params.get("tnMask"), Some("201*"))
    assertEquals(params.get("quantity"), Some("10"))
    assertEquals(params.get("province"), Some("NJ"))
    assertEquals(params.get("rateCenter"), Some("CLOSTER"))
    assertEquals(params.get("lata"), Some("224"))
    assert(!params.contains("npa"), params.toString)
    assert(!params.contains("state"), params.toString)
  }

  test("INumbering.coverage sends countBy and decodes") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"coverage":[{"count":270026,"npa":"201","nxx":"908","locState":"NJ"}]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(
      c.iNumbering.coverage(CoverageQuery(countBy = "npanxx", locState = Some("NJ"), lata = Some("224")))
    )
    assertEquals(r.coverage.head.count, Some(270026))
    val params = reqs.head.uri.paramsMap
    assertEquals(params.get("countBy"), Some("npanxx"))
    assertEquals(params.get("locState"), Some("NJ"))
    assertEquals(params.get("lata"), Some("224"))
  }

  test("INumbering.order decodes with mixed Value/Spec entries") {
    val raw =
      """{"status":"success","data":{"orderId":"O1","amountCharged":1.0,""" +
        """"numbersOrdered":["2015550123"],"failed":[]}}"""
    val c = clientFor(stubOk(raw))
    val r = await(
      c.iNumbering.order(
        OrderCreateRequest(
          List(
            OrderNumber.Value("2015550123"),
            OrderNumber.Spec(OrderNumberSpec("2015551235", Some(4)))
          )
        )
      )
    )
    assertEquals(r.orderId, Some("O1"))
  }

  test("INumbering.ports decodes") {
    val raw =
      """{"status":"success","data":{"ports":[{"status":"submitted",""" +
        """"id":"fedcba9876543210fedcba9876543210fedcba98","pid":"w1476"}]}}"""
    val c = clientFor(stubOk(raw))
    assertEquals(await(c.iNumbering.ports()).ports.head.pid, Some("w1476"))
  }

  test("INumbering.port decodes by 5-char pid") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"port":{"status":"Complete",""" +
        """"id":"fedcba9876543210fedcba9876543210fedcba98","pid":"w1476",""" +
        """"numbers":["2015550123"]}}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.iNumbering.port("w1476"))
    assertEquals(r.port.pid, Some("w1476"))
    assert(reqs.head.uri.toString.contains("/v2.2/ports/w1476"))
  }

  test("INumbering.submitPort decodes") {
    val raw =
      """{"status":"success","data":{"pid":"a3a2a","ticket":2114,""" +
        """"message":"Port request a3a2a submitted. Please complete the LOA to proceed.",""" +
        """"loaUrl":"https://api.voicetel.com/a/loa/a3a2a/",""" +
        """"portUrl":"https://api.voicetel.com/a/port/a3a2a/"}}"""
    val c = clientFor(stubOk(raw))
    val r = await(
      c.iNumbering.submitPort(
        PortSubmitRequest(
          did = List("2015550123"), name = "X", nameType = "business",
          lcBtn = "1", lcAccountNumber = "1", streetNumber = "1",
          street = "M", streetType = "ST", city = "C", state = "IL",
          zip = "60601", country = "US", authPerson = "J"
        )
      )
    )
    assertEquals(r.pid, Some("a3a2a"))
    assertEquals(r.ticket, Some(2114))
  }

  test("INumbering.portAvailability decodes v2.2.10 fields") {
    val raw =
      """{"status":"success","data":{"number":"2015550102","portable":true,""" +
        """"localRoutingNumber":"2015550100","rateCenterTier":"tier1"}}"""
    val c = clientFor(stubOk(raw))
    val r = await(c.iNumbering.portAvailability("2015550102"))
    assertEquals(r.localRoutingNumber, Some("2015550100"))
    assertEquals(r.rateCenterTier, Some("tier1"))
  }

  // ---------------------------------------------------------------- lookups ---

  test("Lookups.cnam decodes") {
    val resp = CnamData(cnam = Some("ACME"), number = Some("2015550123"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.lookups.cnam("2015550123")).cnam, Some("ACME"))
  }

  test("Lookups.lrn decodes") {
    val resp = LrnLookupData(
      ani = Some("2015550123"),
      destination = Some("2015550100"),
      lrn = Some(LrnData(lrn = Some("2015550100")))
    )
    val c = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.lookups.lrn("2015550100", "2015550123")).lrn.flatMap(_.lrn), Some("2015550100"))
  }

  // ---------------------------------------------------------------- messaging ---

  test("Messaging.history sends spec query params and decodes (value.number is a string)") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"number":"2015550123","type":"sms","fromTs":1747345200,""" +
        """"toTs":1747258800,"messages":[{"id":"0123456789abcdef0123456789abcdef01234567",""" +
        """"key":["2015550123","1747345200"],"value":{"direction":"in","rate":"0.0150",""" +
        """"message":"hi","number":"2015550123"}}]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(
      c.messaging.history(
        HistoryOptions(
          number = "2015550123",
          from = Some(1747345200),
          to = Some(1747258800),
          `type` = Some("sms")
        )
      )
    )
    assertEquals(r.`type`, Some("sms"))
    assertEquals(r.messages.head.value.number, Some("2015550123"))
    val params = reqs.head.uri.paramsMap
    assertEquals(params.get("number"), Some("2015550123"))
    assertEquals(params.get("from"), Some("1747345200"))
    assertEquals(params.get("to"), Some("1747258800"))
    assertEquals(params.get("type"), Some("sms"))
    assert(!params.contains("start"), params.toString)
    assert(!params.contains("end"), params.toString)
  }

  test("Messaging.send uses wire fields fromNumber/toNumber") {
    val raw =
      """{"status":"success","data":{"id":"msgid","type":"sms","fromNumber":"2015550100",""" +
        """"toNumber":"2015550123","parts":1,"subject":"example","mediaUrls":["example"]}}"""
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches { req =>
        val body = req.body match
          case sttp.client3.StringBody(s, _, _) => s
          case _                                => ""
        body.contains("\"fromNumber\":\"2015550100\"") &&
        body.contains("\"toNumber\":\"2015550123\"")
      }
      .thenRespond(raw)
    val c = clientFor(backend)
    val r = await(c.messaging.send(MessageSendRequest("2015550100", "2015550123", "hi")))
    assertEquals(r.fromNumber, Some("2015550100"))
    assertEquals(r.mediaUrls, List("example"))
  }

  test("Messaging.createBrand decodes") {
    val raw = """{"status":"success","data":{"result":{"statusCode":"200","status":"Success"}}}"""
    val c   = clientFor(stubOk(raw))
    val r   = await(c.messaging.createBrand(MessagingBrandCreateRequest("BABC", "ACME")))
    assertEquals(r.result.status, "Success")
  }

  test("Messaging.campaignStatus sends required ids query param and decodes") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw  = """{"status":"success","data":{"campaigns":[]}}"""
    val c    = clientFor(recordingStub(reqs, raw))
    val r    = await(c.messaging.campaignStatus(List("CAMP002", "NOPEID1")))
    assertEquals(r.campaigns, Nil)
    assertEquals(reqs.head.uri.paramsMap.get("ids"), Some("CAMP002,NOPEID1"))
  }

  test("Messaging.campaignStatus decodes carrier (DCA) review progress") {
    val raw = envelope(Json.obj("campaigns" -> Json.arr(
      Json.obj(
        "id"      -> Json.fromString("CAMP002"),
        "status"  -> Json.fromString("CAMPAIGN_PENDING_DCA"),
        "numbers" -> Json.arr(Json.fromString("2015550123")),
        "dca" -> Json.obj(
          "electionComplete" -> Json.False,
          "electedCarriers"  -> Json.arr(
            Json.fromString("AT&T"), Json.fromString("T-Mobile"),
            Json.fromString("US Cellular"), Json.fromString("Verizon Wireless"),
            Json.fromString("ClearSky"), Json.fromString("Interop")
          ),
          "carriers" -> Json.arr(
            Json.obj(
              "carrier"   -> Json.fromString("T-Mobile"),
              "status"    -> Json.fromString("ACTIVE"),
              "detail"    -> Json.fromString("Campaign approved"),
              "updatedAt" -> Json.fromString("2026-01-02T03:04:05Z")
            ),
            Json.obj(
              "carrier"   -> Json.fromString("Verizon Wireless"),
              "status"    -> Json.fromString("REJECTED"),
              "detail"    -> Json.fromString("Missing sample opt-in"),
              "updatedAt" -> Json.fromString("2026-01-03T03:04:05Z")
            )
          )
        )
      ),
      Json.obj(
        "id"      -> Json.fromString("NOPEID1"),
        "status"  -> Json.fromString("CAMPAIGN_INVALID_ID"),
        "numbers" -> Json.arr(),
        "dca"     -> Json.Null
      )
    )))
    val c     = clientFor(stubOk(raw))
    val camps = await(c.messaging.campaignStatus(List("CAMP002", "NOPEID1"))).campaigns
    assertEquals(camps.head.id, Some("CAMP002"))
    assertEquals(camps.head.dca.map(_.electionComplete), Some(false))
    assertEquals(camps.head.dca.flatMap(_.electedCarriers).map(_.size), Some(6))
    assertEquals(camps.head.dca.map(_.carriers.map(_.status)), Some(List("ACTIVE", "REJECTED")))
    assertEquals(camps.head.dca.map(_.carriers.head.updatedAt), Some("2026-01-02T03:04:05Z"))
    assertEquals(camps(1).dca, None)
  }

  test("Messaging.createCampaign decodes") {
    val raw = """{"status":"success","data":{"result":{"statusCode":"200","status":"Success"}}}"""
    val c   = clientFor(stubOk(raw))
    val r   = await(c.messaging.createCampaign(MessagingCampaignCreateRequest("B", "C", "desc")))
    assertEquals(r.result.statusCode, "200")
  }

  test("Messaging.numbersState sends required numbers query param and decodes") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"numbers":[{"number":"2015550123","onAccount":true,""" +
        """"enabled":true,"carrier":17,"routeIn":1,"resource":"https://example.com/sms",""" +
        """"network":"B","campaign":{"id":"CAMP002","status":"ACTIVE","network":"B",""" +
        """"upstreamCnpId":"CAMP003"}}]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.messaging.numbersState(List("2015550123", "2015550100")))
    assertEquals(r.numbers.head.carrier, Some(17))
    assertEquals(r.numbers.head.campaign.flatMap(_.id), Some("CAMP002"))
    assertEquals(reqs.head.uri.paramsMap.get("numbers"), Some("2015550123,2015550100"))
  }

  // v2.2.15: messaging-health endpoints have no response schema in the spec, so the
  // SDK returns the decoded JSON as a raw circe Json; bodies/query are still shaped.
  test("Messaging.health returns raw JSON") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val data = Json.obj("summary" -> Json.obj("numbers" -> Json.fromInt(3), "healthy" -> Json.fromInt(2)))
    val c    = clientFor(recordingStub(reqs, envelope(data)))
    val r    = await(c.messaging.health())
    assertEquals(r.hcursor.downField("summary").get[Int]("numbers"), Right(3))
    assertEquals(reqs.head.method, sttp.model.Method.GET)
    assert(reqs.head.uri.toString.contains("/v2.2/messaging/health"))
  }

  test("Messaging.healthAlerts returns raw JSON") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val data = Json.obj("enabled" -> Json.True)
    val c    = clientFor(recordingStub(reqs, envelope(data)))
    val r    = await(c.messaging.healthAlerts())
    assertEquals(r.hcursor.get[Boolean]("enabled"), Right(true))
    assert(reqs.head.uri.toString.contains("/v2.2/messaging/health/alerts"))
  }

  test("Messaging.setHealthAlerts sends enabled and returns raw JSON") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val data = Json.obj("enabled" -> Json.False)
    val c    = clientFor(recordingStub(reqs, envelope(data)))
    val r    = await(c.messaging.setHealthAlerts(false))
    assertEquals(r.hcursor.get[Boolean]("enabled"), Right(false))
    assertEquals(reqs.head.method, sttp.model.Method.PUT)
    val sent = reqs.head.body match
      case sttp.client3.StringBody(s, _, _) => s
      case _                                => ""
    assert(sent.contains("\"enabled\":false"))
  }

  test("Messaging.healthCheck sends number and returns raw JSON") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val data = Json.obj("number" -> Json.fromString("2125550123"), "inService" -> Json.True)
    val c    = clientFor(recordingStub(reqs, envelope(data)))
    val r    = await(c.messaging.healthCheck("2125550123"))
    assertEquals(r.hcursor.get[Boolean]("inService"), Right(true))
    assertEquals(reqs.head.method, sttp.model.Method.POST)
    val sent = reqs.head.body match
      case sttp.client3.StringBody(s, _, _) => s
      case _                                => ""
    assert(sent.contains("\"number\":\"2125550123\""))
  }

  test("Messaging.healthSamples sends number query and returns raw JSON") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val data = Json.obj("samples" -> Json.arr(Json.obj("reason" -> Json.fromString("spam"))))
    val c    = clientFor(recordingStub(reqs, envelope(data)))
    val r    = await(c.messaging.healthSamples("2125550123"))
    assertEquals(r.hcursor.downField("samples").downArray.get[String]("reason"), Right("spam"))
    assert(reqs.head.uri.toString.contains("number=2125550123"))
  }

  // ---------------------------------------------------------------- numbers ---

  private val numDetailJson =
    """{"number":"2015550123","translated":"2015550123","route":4,"gateway":null,""" +
      """"cnam":false,"forward":false,"forwardTo":null,"carrier":0,"smsEnabled":false,""" +
      """"faxEnabled":false}"""

  test("Numbers.list decodes") {
    val raw = s"""{"status":"success","data":{"numbers":[$numDetailJson]}}"""
    val c   = clientFor(stubOk(raw))
    val r   = await(c.numbers.list())
    assertEquals(r.numbers.head.number, Some("2015550123"))
    assertEquals(r.numbers.head.route, Some(4))
  }

  test("Numbers.add decodes") {
    val c = clientFor(stubOk("""{"status":"success","data":{"number":"2015550123","route":4}}"""))
    assertEquals(await(c.numbers.add(NumberAddRequest("2015550123"))).route, Some(4))
  }

  test("Numbers.get decodes") {
    val c = clientFor(stubOk(s"""{"status":"success","data":$numDetailJson}"""))
    assertEquals(await(c.numbers.get("2015550123")).number, Some("2015550123"))
  }

  test("Numbers.remove and .release return Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.numbers.remove("2015550123"))
    await(c.numbers.release("2015550123"))
  }

  test("Numbers.move decodes") {
    val raw = """{"status":"success","data":{"number":"2015550123","accountId":1,"route":4}}"""
    val c   = clientFor(stubOk(raw))
    assertEquals(await(c.numbers.move("2015550123", NumberMoveRequest(1, 4))).accountId, Some(1))
  }

  test("Numbers route/translation/cnam/lidb setters") {
    val rRoute = """{"status":"success","data":{"number":"2015550123","route":7}}"""
    val rTr    = """{"status":"success","data":{"number":"2015550123","translation":"2015551235"}}"""
    val rCnam  = """{"status":"success","data":{"number":"2015550123","cnam":true}}"""
    val rLidb  =
      """{"status":"success","data":{"number":"2015550123","cnam":"ACME",""" +
        """"customerOrderReference":"ref","carrierStatus":"Success"}}"""
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches(_.uri.path.lastOption.contains("route")).thenRespond(rRoute)
      .whenRequestMatches(_.uri.path.lastOption.contains("translation")).thenRespond(rTr)
      .whenRequestMatches(_.uri.path.lastOption.contains("cnam")).thenRespond(rCnam)
      .whenRequestMatches(_.uri.path.lastOption.contains("lidb")).thenRespond(rLidb)
    val c = clientFor(backend)
    assertEquals(await(c.numbers.setRoute("2015550123", NumberRouteRequest(7))).route, Some(7))
    assertEquals(
      await(c.numbers.setTranslation("2015550123", NumberTranslationRequest("2015551235"))).translation,
      Some("2015551235")
    )
    assertEquals(await(c.numbers.setCnam("2015550123", NumberCnamRequest(true))).cnam, Some(true))
    assertEquals(await(c.numbers.setLidb("2015550123", NumberLidbRequest("ACME"))).cnam, Some("ACME"))
  }

  test("Numbers fax getter and setter") {
    val raw     = """{"status":"success","data":{"number":"2015550123","email":"f@x.com"}}"""
    val backend = stubOk(raw)
    val c       = clientFor(backend)
    assertEquals(await(c.numbers.getFax("2015550123")).email, Some("f@x.com"))
    assertEquals(await(c.numbers.setFax("2015550123", NumberFaxRequest("f@x.com"))).email, Some("f@x.com"))
  }

  test("Numbers fax/forward/sms DELETE return Unit on 204") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val c = clientFor(backend)
    await(c.numbers.removeFax("2015550123"))
    await(c.numbers.removeForward("2015550123"))
    await(c.numbers.removeSms("2015550123"))
  }

  test("Numbers.setForward decodes") {
    val raw = """{"status":"success","data":{"number":"2015550123","forwardTo":"2125550123"}}"""
    val c   = clientFor(stubOk(raw))
    val r   = await(c.numbers.setForward("2015550123", NumberForwardRequest("2125550123")))
    assertEquals(r.forwardTo, Some("2125550123"))
  }

  test("Numbers SMS get/set") {
    val raw = """{"status":"success","data":{"number":"2015550123","type":"email","resource":"f@x.com"}}"""
    val c   = clientFor(stubOk(raw))
    assertEquals(await(c.numbers.getSms("2015550123")).resource, Some("f@x.com"))
    assertEquals(
      await(c.numbers.setSms("2015550123", NumberSmsRequest("email", "f@x.com"))).resource,
      Some("f@x.com")
    )
  }

  test("Numbers.getMessaging and patchMessaging") {
    val state =
      """{"status":"success","data":{"number":"2015550123","enabled":true,"carrier":17,""" +
        """"routeIn":1,"resource":"r","network":"A"}}"""
    val patch = """{"status":"success","data":{"number":"2015550123","updated":["routeIn"]}}"""
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenRequestMatches(_.method == sttp.model.Method.GET).thenRespond(state)
      .whenRequestMatches(_.method == sttp.model.Method.PATCH).thenRespond(patch)
    val c = clientFor(backend)
    assertEquals(await(c.numbers.getMessaging("2015550123")).carrier, Some(17))
    assertEquals(
      await(c.numbers.patchMessaging("2015550123", NumberMessagingPatchRequest(routeIn = Some(1)))).updated,
      List("routeIn")
    )
  }

  test("Numbers.getMessaging decodes pending path-C job") {
    val raw = envelope(Json.obj(
      "number"   -> Json.fromString("2015550123"),
      "enabled"  -> Json.False,
      "carrier"  -> Json.fromInt(1),
      "routeIn"  -> Json.fromInt(0),
      "resource" -> Json.fromString(""),
      "network"  -> Json.fromString("C"),
      "pending" -> Json.obj(
        "campaignId" -> Json.fromString("CAMP002"),
        "state"      -> Json.fromString("assigning"),
        "message"    -> Json.fromString("placing network order"),
        "createdAt"  -> Json.fromString("2026-01-01T00:00:00Z"),
        "updatedAt"  -> Json.fromString("2026-01-01T00:01:00Z")
      )
    ))
    val c = clientFor(stubOk(raw))
    val r = await(c.numbers.getMessaging("2015550123"))
    assertEquals(r.network, Some("C"))
    assertEquals(r.pending.flatMap(_.campaignId), Some("CAMP002"))
    assertEquals(r.pending.flatMap(_.state), Some("assigning"))
  }

  test("Numbers campaign assign") {
    val raw =
      """{"status":"success","data":{"number":"6185550104","campaignId":"CAMP002",""" +
        """"status":"pending","network":"C","upstreamCnpId":"BANDW","carrier":1,""" +
        """"alreadyAssigned":false,"previousNetwork":null,"previousNetworkCleared":false,""" +
        """"orderId":null,"warning":null,""" +
        """"message":"Enablement accepted; the number is being provisioned with the messaging provider (typically minutes, sometimes longer).",""" +
        """"job":{"campaignId":"CAMP002","state":"queued","message":"Queued",""" +
        """"createdAt":"2026-10-13T09:30:00+00:00","updatedAt":"2026-10-13T09:30:00+00:00"}}}"""
    val c = clientFor(stubOk(raw))
    val r = await(c.numbers.assignCampaign("6185550104", NumberCampaignAssignRequest("CAMP002")))
    assertEquals(r.campaignId, Some("CAMP002"))
    assertEquals(r.status, Some("pending"))
    assertEquals(r.alreadyAssigned, false)
    assertEquals(r.job.flatMap(_.state), Some("queued"))
  }

  test("Numbers campaign unassign (single) sends campaignId query param, no body") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"number":"2015550123","campaignId":"CAMP002","network":"A",""" +
        """"upstreamCnpId":"CAMP004","unassigned":true}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.numbers.unassignCampaign("2015550123", "CAMP002"))
    assertEquals(r.unassigned, Some(true))
    assertEquals(reqs.head.method, sttp.model.Method.DELETE)
    assertEquals(reqs.head.uri.paramsMap.get("campaignId"), Some("CAMP002"))
    assertEquals(reqs.head.body, sttp.client3.NoBody)
  }

  test("Numbers campaign bulk unassign sends numbers+campaignId query params, no body") {
    val reqs = scala.collection.mutable.ListBuffer[sttp.client3.Request[?, ?]]()
    val raw =
      """{"status":"success","data":{"campaignId":"CAMP002","network":"A","upstreamCnpId":"CAMP004",""" +
        """"unassignedNumbers":["2015550123"],"failed":[]}}"""
    val c = clientFor(recordingStub(reqs, raw))
    val r = await(c.numbers.bulkUnassignCampaign(List("2015550123", "2015550100"), "CAMP002"))
    assertEquals(r.unassignedNumbers, List("2015550123"))
    assertEquals(reqs.head.uri.paramsMap.get("numbers"), Some("2015550123,2015550100"))
    assertEquals(reqs.head.uri.paramsMap.get("campaignId"), Some("CAMP002"))
    assertEquals(reqs.head.body, sttp.client3.NoBody)
  }

  test("Numbers.setPortOutPin decodes") {
    val raw =
      """{"status":"success","data":{"number":"2015550123","portOutPin":"1234","changed":true}}"""
    val c = clientFor(stubOk(raw))
    val r = await(c.numbers.setPortOutPin("2015550123", PortOutPinUpdateRequest("1234")))
    assertEquals(r.portOutPin, Some("1234"))
    assertEquals(r.changed, Some(true))
  }

  // ---------------------------------------------------------------- support ---

  private val ticket = SupportConversation(
    id = Some(1), ticketNumber = Some(1015), status = "active", subject = Some("hi")
  )

  test("Support.list decodes ticketNumber from wire 'number' field, plus totalCount") {
    val raw = envelope(Json.obj(
      "totalCount" -> Json.fromInt(1),
      "tickets" -> Json.arr(
        Json.obj(
          "id"     -> Json.fromInt(1),
          "number" -> Json.fromInt(1015),
          "status" -> Json.fromString("active")
        )
      )
    ))
    val c = clientFor(stubOk(raw))
    val r = await(c.support.list())
    assertEquals(r.tickets.head.ticketNumber, Some(1015))
    assertEquals(r.totalCount, Some(1))
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
    val resp = TicketReplyData(Some("Reply added"))
    val c    = clientFor(stubOk(envelopeOf(resp)))
    assertEquals(await(c.support.reply(1, TicketReplyRequest("ok"))).message, Some("Reply added"))
  }

  // ---------------------------------------------------------------- error path ---

  test("every method propagates server errors") {
    val backend = stubStatus(500, errorBody)
    val c       = clientFor(backend)
    val acl     = AclModifyRequest(List(CidrEntry("203.0.113.0/24")))
    val numReq  = NumberAddRequest("2015550123")
    def shouldFail(f: Future[?], name: String): Unit =
      val t = intercept[ApiError](await(f))
      assertEquals(t.kind, ErrorKind.Server, name)
    // account
    shouldFail(c.account.get(), "account.get")
    shouldFail(c.account.update(AccountPutRequest(timezone = Some("UTC"))), "account.update")
    shouldFail(c.account.add(AccountAddRequest("1000000002", "n", "e@x.com")), "account.add")
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
    shouldFail(c.acl.remove(List("203.0.113.0/24")), "acl.remove")
    // auth
    shouldFail(c.authentication.get(), "auth.get")
    shouldFail(c.authentication.update(AuthPutRequest(authType = Some(1))), "auth.update")
    // e911
    shouldFail(c.e911.list(), "e911.list")
    shouldFail(c.e911.create(E911CreateRequest("2015550123", "x", "1", None, "C", "NJ", "07082")), "e911.create")
    shouldFail(c.e911.validate(E911AddressRequest("1", None, "C", "NJ", "07082")), "e911.validate")
    shouldFail(c.e911.get("2015550123"), "e911.get")
    shouldFail(c.e911.provision("2015550123", E911ProvisionByIDRequest("x", 1)), "e911.provision")
    shouldFail(c.e911.remove("2015550123"), "e911.remove")
    // gateways
    shouldFail(c.gateways.list(), "gateways.list")
    shouldFail(c.gateways.add(GatewayAddRequest("1.2.3.4")), "gateways.add")
    shouldFail(c.gateways.get(1), "gateways.get")
    shouldFail(c.gateways.update(1, GatewayUpdateRequest(prefix = Some("9"))), "gateways.update")
    shouldFail(c.gateways.remove(1), "gateways.remove")
    shouldFail(c.gateways.numbers(1), "gateways.numbers")
    // lookups
    shouldFail(c.lookups.cnam("2015550123"), "lookups.cnam")
    shouldFail(c.lookups.lrn("2015550100", "2015550123"), "lookups.lrn")
    // messaging
    shouldFail(c.messaging.history(HistoryOptions(number = "2015550123")), "messaging.history")
    shouldFail(c.messaging.send(MessageSendRequest("2015550100", "2015550123", "hi")), "messaging.send")
    shouldFail(c.messaging.createBrand(MessagingBrandCreateRequest("BABC", "X")), "messaging.createBrand")
    shouldFail(c.messaging.campaignStatus(List("CAMP002")), "messaging.campaignStatus")
    shouldFail(c.messaging.createCampaign(MessagingCampaignCreateRequest("B", "C", "d")), "messaging.createCampaign")
    shouldFail(c.messaging.numbersState(List("2015550123")), "messaging.numbersState")
    // numbers
    shouldFail(c.numbers.list(), "numbers.list")
    shouldFail(c.numbers.add(numReq), "numbers.add")
    shouldFail(c.numbers.get("2015550123"), "numbers.get")
    shouldFail(c.numbers.remove("2015550123"), "numbers.remove")
    shouldFail(c.numbers.move("2015550123", NumberMoveRequest(1, 4)), "numbers.move")
    shouldFail(c.numbers.release("2015550123"), "numbers.release")
    shouldFail(c.numbers.setRoute("2015550123", NumberRouteRequest(7)), "numbers.setRoute")
    shouldFail(c.numbers.setTranslation("2015550123", NumberTranslationRequest("2015551235")), "numbers.setTranslation")
    shouldFail(c.numbers.setCnam("2015550123", NumberCnamRequest(true)), "numbers.setCnam")
    shouldFail(c.numbers.setLidb("2015550123", NumberLidbRequest("ACME")), "numbers.setLidb")
    shouldFail(c.numbers.getFax("2015550123"), "numbers.getFax")
    shouldFail(c.numbers.setFax("2015550123", NumberFaxRequest("f@x.com")), "numbers.setFax")
    shouldFail(c.numbers.removeFax("2015550123"), "numbers.removeFax")
    shouldFail(c.numbers.setForward("2015550123", NumberForwardRequest("2125550123")), "numbers.setForward")
    shouldFail(c.numbers.removeForward("2015550123"), "numbers.removeForward")
    shouldFail(c.numbers.getSms("2015550123"), "numbers.getSms")
    shouldFail(c.numbers.setSms("2015550123", NumberSmsRequest("email", "f@x.com")), "numbers.setSms")
    shouldFail(c.numbers.removeSms("2015550123"), "numbers.removeSms")
    shouldFail(c.numbers.getMessaging("2015550123"), "numbers.getMessaging")
    shouldFail(c.numbers.patchMessaging("2015550123", NumberMessagingPatchRequest(routeIn = Some(1))), "numbers.patchMessaging")
    shouldFail(c.numbers.assignCampaign("2015550123", NumberCampaignAssignRequest("C1")), "numbers.assignCampaign")
    shouldFail(c.numbers.unassignCampaign("2015550123", "C1"), "numbers.unassignCampaign")
    shouldFail(c.numbers.bulkUnassignCampaign(List("2015550123"), "C1"), "numbers.bulkUnassignCampaign")
    shouldFail(c.numbers.setPortOutPin("2015550123", PortOutPinUpdateRequest("1234")), "numbers.setPortOutPin")
    // iNumbering
    shouldFail(c.iNumbering.searchInventory(InventoryQuery(tnMask = "201*")), "iNumbering.searchInventory")
    shouldFail(c.iNumbering.coverage(CoverageQuery(countBy = "state")), "iNumbering.coverage")
    shouldFail(c.iNumbering.order(OrderCreateRequest(List(OrderNumber.Value("2015550123")))), "iNumbering.order")
    shouldFail(c.iNumbering.ports(), "iNumbering.ports")
    shouldFail(c.iNumbering.port("w1476"), "iNumbering.port")
    shouldFail(
      c.iNumbering.submitPort(
        PortSubmitRequest(
          did = List("2015550123"), name = "X", nameType = "business",
          lcBtn = "1", lcAccountNumber = "1", streetNumber = "1",
          street = "M", streetType = "ST", city = "C", state = "IL",
          zip = "60601", country = "US", authPerson = "J"
        )
      ), "iNumbering.submitPort"
    )
    shouldFail(c.iNumbering.portAvailability("2015550123"), "iNumbering.portAvailability")
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
