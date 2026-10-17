package com.voicetel.sdk

import com.voicetel.sdk.Models.*
import io.circe.*
import io.circe.parser.parse
import io.circe.syntax.*

/** Confirms every public DTO round-trips through Circe.
  *
  * Round-tripping touches both the encoder and decoder, plus equality, hash,
  * and toString for the case classes — which boosts coverage materially.
  */
class CodecRoundTripSuite extends munit.FunSuite:

  private def roundTrip[A: Encoder: Decoder](a: A, label: String): Unit =
    val json    = a.asJson
    val s       = json.noSpaces
    val parsed  = parse(s).fold(e => fail(s"$label: parse: $e"), identity)
    val decoded = parsed.as[A].fold(e => fail(s"$label: decode: $e"), identity)
    assertEquals(decoded, a, label)

  test("Account models round-trip") {
    roundTrip(AccountRates(cnam = Some(0.01), sms = Some(0.005)), "AccountRates")
    roundTrip(AccountServices(e911 = Some(true), mms = Some(false)), "AccountServices")
    roundTrip(
      AccountData(
        username = Some("u"), name = Some("n"), email = Some("e@x.com"),
        enabled = Some(true), created = Some("d"), cash = Some(1.0),
        callerId = Some("c"), timezone = Some("UTC"), authType = Some(1),
        ccs = Some(2), notifyEnabled = Some(true), notifyThreshold = Some(3),
        rates = Some(AccountRates(sms = Some(0.005))),
        services = Some(AccountServices(sms = Some(true)))
      ),
      "AccountData"
    )
    roundTrip(CreditEntry("d", true, 1.0), "CreditEntry")
    roundTrip(PaymentEntry(Some("t"), "d", Some("e@x.com"), "Completed", 1.0), "PaymentEntry")
    roundTrip(CdrEntryValue(Some("1"), Some("2"), Some("3"), Some("4"), Some("5"), Some("1.2.3.4"), Some("6")), "CdrEntryValue")
    roundTrip(CdrEntry("id", List("k1", "k2"), CdrEntryValue()), "CdrEntry")
    roundTrip(AccountCdrData(Nil, Some(1), Some(2)), "AccountCdrData")
    roundTrip(AccountCreditsData(List(CreditEntry("d", true, 1.0))), "AccountCreditsData")
    roundTrip(AccountPaymentsData(Nil), "AccountPaymentsData")
    roundTrip(MrcCharge(Some(1.0), Some("d")), "MrcCharge")
    roundTrip(AccountMrcData(List(MrcCharge(Some(1.0))), Some(1.0)), "AccountMrcData")
    roundTrip(AccountRegistrationData(Some("a"), Some("u")), "AccountRegistrationData")
    roundTrip(AccountAddRequest("1000000001", "n", "e@x.com", Some("1000000001")), "AccountAddRequest")
    roundTrip(AccountAddData(Some("u"), Some("n"), Some("e"), Some("m"), Some("p")), "AccountAddData")
    roundTrip(
      AccountPutRequest(Some(true), Some(5), Some("UTC"), Some("c"), Some(true), Some(true), Some(true), Some(true), Some(1)),
      "AccountPutRequest"
    )
    roundTrip(AccountPutData(List("a", "b")), "AccountPutData")
    roundTrip(AccountSignupRequest("n", "e@x.com", Some("p")), "AccountSignupRequest")
    roundTrip(AccountSignupData(Some("u"), Some("n"), Some("e"), Some("p")), "AccountSignupData")
    roundTrip(AccountRecoverRequest("e@x.com"), "AccountRecoverRequest")
    roundTrip(AccountRecoverData(Some("ok")), "AccountRecoverData")
    roundTrip(AccountApiKeyData(Some("hex")), "AccountApiKeyData")
  }

  test("ACL models round-trip") {
    val cidr = CidrEntry("203.0.113.0/24")
    roundTrip(cidr, "CidrEntry")
    roundTrip(AclModifyRequest(List(cidr)), "AclModifyRequest")
    roundTrip(AclListData(List(cidr)), "AclListData")
    roundTrip(AclAddData(List(cidr)), "AclAddData")
    roundTrip(AclRemoveData(List(cidr)), "AclRemoveData")
    roundTrip(AclFailedEntry("1.2.3.4/8", "Invalid"), "AclFailedEntry")
    roundTrip(AclConflictData(List(cidr), Nil, List(AclFailedEntry("x", "y"))), "AclConflictData")
  }

  test("Authentication models round-trip") {
    roundTrip(AuthPutRequest(Some(1), Some("pw1234")), "AuthPutRequest")
    roundTrip(AuthGetData(Some(2), Some("Digest OR IP"), List(CidrEntry("1.2.3.4/32"))), "AuthGetData")
    roundTrip(AuthUpdatedEntry("authType", Some(2)), "AuthUpdatedEntry")
    roundTrip(AuthPutData(List(AuthUpdatedEntry("password"))), "AuthPutData")
    roundTrip(AuthPutConflictData(List(AuthUpdatedEntry("authType", Some(1)))), "AuthPutConflictData")
    assertEquals(AuthType.Digest, 0)
    assertEquals(AuthType.Lax, 1)
    assertEquals(AuthType.IP, 2)
    assertEquals(AuthType.Strict, 3)
  }

  test("E911 models round-trip") {
    roundTrip(E911AddressRequest("1", Some("2"), "C", "NJ", "07082"), "E911AddressRequest")
    roundTrip(E911CreateRequest("2015550123", "X", "1", Some("2"), "C", "NJ", "07082"), "E911CreateRequest")
    roundTrip(E911ProvisionByIDRequest("X", 1), "E911ProvisionByIDRequest")
    val e = E911Entry("12015550123", "X", "1", Some("2"), "C", "NJ", "07082")
    roundTrip(e, "E911Entry")
    roundTrip(E911ValidatedAddress(1, "1", None, "C", "NJ", "07082"), "E911ValidatedAddress")
    roundTrip(E911AllData(List(e)), "E911AllData")
    roundTrip(E911RecordData(e), "E911RecordData")
    roundTrip(E911ValidateData(E911ValidatedAddress(1, "1", None, "C", "NJ", "07082")), "E911ValidateData")
  }

  test("Gateways models round-trip") {
    roundTrip(GatewayAddRequest("1.2.3.4", Some("9"), Some(20)), "GatewayAddRequest")
    roundTrip(GatewayUpdateRequest(Some("1.2.3.4"), Some("9"), Some(20)), "GatewayUpdateRequest")
    val g = GatewayEntry(Some(1), Some("1.2.3.4"), Some("9"), Some(20), Some(false))
    roundTrip(g, "GatewayEntry")
    roundTrip(GatewayNumberSummary("2015550123", "2015550123", true, Some("2125550123"), true, 17, true, true), "GatewayNumberSummary")
    roundTrip(GatewaysListData(List(g)), "GatewaysListData")
    roundTrip(GatewayNumbersData(Nil), "GatewayNumbersData")
  }

  test("iNumbering models round-trip") {
    val plain = OrderNumber.Value("2015550123")
    val spec  = OrderNumber.Spec(OrderNumberSpec("2015551235", Some(4)))
    assertEquals(plain.asJson.noSpaces, "\"2015550123\"")
    assertEquals(spec.asJson.as[OrderNumber].toOption.get, spec)
    assertEquals(plain.asJson.as[OrderNumber].toOption.get, plain)
    roundTrip(OrderCreateRequest(List(plain, spec)), "OrderCreateRequest")
    roundTrip(PortFeatureLidb("ACME"), "PortFeatureLidb")
    roundTrip(PortFeatureRouting(4), "PortFeatureRouting")
    roundTrip(PortFeatureSms(Some("C")), "PortFeatureSms")
    roundTrip(PortFeature("2015550123", Some(PortFeatureRouting(4)), Some(PortFeatureLidb("ACME")), Some(PortFeatureSms())), "PortFeature")
    val port = PortSubmitRequest(
      did = List("2015550123"), name = "X", nameType = "business",
      lcBtn = "1", lcAccountNumber = "1", streetNumber = "1",
      street = "M", streetType = "ST", city = "C", state = "IL",
      zip = "60601", country = "US", authPerson = "J",
      streetPrefix = Some("N"), streetSuffix = Some("S"),
      floor = Some("2"), room = Some("210"), building = Some("B"),
      unitValue = Some("STE 200"), desiredDueDate = Some("2026-01-01"),
      pin = Some("1234"),
      features = List(PortFeature("2015550123"))
    )
    roundTrip(port, "PortSubmitRequest")
    roundTrip(
      InventoryItem(Some("2015550123"), Some("RC"), Some("C"), Some("NJ"), Some("224")),
      "InventoryItem"
    )
    roundTrip(
      InventoryCoverageItem(Some(5), Some("201"), Some("555"), Some("0"), Some("C"), Some("RC"), Some("224"), Some("NJ")),
      "InventoryCoverageItem"
    )
    roundTrip(PortSummary("submitted", Some("1"), Some("P"), Some("F"), Some("c"), Some("m")), "PortSummary")
    roundTrip(PortDetail("submitted", Some("1"), Some("P"), Some("N"), Some("E"), Some("F"), Some("c"), List("2015550123"), Some("m")), "PortDetail")
    roundTrip(InventorySearchData(Nil), "InventorySearchData")
    roundTrip(InventoryCoverageData(Nil), "InventoryCoverageData")
    roundTrip(OrderFailedEntry("n", "r"), "OrderFailedEntry")
    roundTrip(OrderCreateData(Some("O"), Some(1.0), List("n"), List(OrderFailedEntry("n2", "r2"))), "OrderCreateData")
    roundTrip(PortListData(Nil), "PortListData")
    roundTrip(PortDetailData(PortDetail("active")), "PortDetailData")
    roundTrip(PortSubmitData(Some("P"), Some(1), Some("m"), Some("l"), Some("p")), "PortSubmitData")
    roundTrip(
      PortAvailabilityData("2015550123", true, Some("AT&T"), Some("LRN"), Some("tier1"), Some("ok")),
      "PortAvailabilityData"
    )
  }

  test("Lookups models round-trip") {
    roundTrip(CnamData(Some("ACME"), Some("2015550123")), "CnamData")
    roundTrip(LrnData(Some("L"), Some("S"), Some("C"), Some("R"), Some("224"), Some("O"), Some("E"), Some("T"), Some("J"), Some("Y")), "LrnData")
    roundTrip(LrnLookupData(Some("a"), Some("d"), Some(LrnData(Some("L")))), "LrnLookupData")
  }

  test("Messaging models round-trip") {
    roundTrip(MessageSendRequest("2015550100", "2015550123", "hi", Some("subj"), List("u1")), "MessageSendRequest")
    roundTrip(MessagingBrandCreateRequest("BABC", "X", Some("d")), "MessagingBrandCreateRequest")
    roundTrip(MessagingCampaignCreateRequest("B", "C", "d", Some("Cls"), Some("2026-01-01")), "MessagingCampaignCreateRequest")
    roundTrip(MessageRecordValue(Some("s"), Some("d"), Some("in"), Some("0.005"), Some("2015550123"), Some("m")), "MessageRecordValue")
    roundTrip(MessageRecord("id", List(Json.fromString("a"), Json.fromInt(1)), MessageRecordValue()), "MessageRecord")
    roundTrip(MessageHistoryData(Some("2015550123"), Some("sms"), Some(1), Some(2), Nil), "MessageHistoryData")
    roundTrip(MessageSendData(Some("id"), Some("sms"), Some("f"), Some("t"), Some(1), Some("s"), List("u")), "MessageSendData")
    roundTrip(BrandRegistrationResult("200", "Success"), "BrandRegistrationResult")
    roundTrip(MessagingBrandCreateData(BrandRegistrationResult("200", "S")), "MessagingBrandCreateData")
    roundTrip(CampaignRegistrationResult("200", "S"), "CampaignRegistrationResult")
    roundTrip(MessagingCampaignCreateData(CampaignRegistrationResult("200", "S")), "MessagingCampaignCreateData")
    roundTrip(CampaignDcaCarrier("T-Mobile", "ACTIVE", "Campaign approved", "2026-01-02T03:04:05Z"), "CampaignDcaCarrier")
    roundTrip(
      CampaignDca(
        electionComplete = false,
        electedCarriers = Some(List("AT&T", "T-Mobile", "US Cellular")),
        carriers = List(CampaignDcaCarrier("T-Mobile", "ACTIVE", "Campaign approved", "2026-01-02T03:04:05Z"))
      ),
      "CampaignDca"
    )
    roundTrip(CampaignDca(electionComplete = true, electedCarriers = None, carriers = Nil), "CampaignDca (empty)")
    roundTrip(
      CampaignStatusItem(Some("CAMP002"), Some("CAMPAIGN_PENDING_DCA"), List("2015550123"), Some(CampaignDca(electionComplete = true))),
      "CampaignStatusItem"
    )
    roundTrip(CampaignStatusItem(Some("BADID"), Some("CAMPAIGN_INVALID_ID"), Nil, None), "CampaignStatusItem (no dca)")
    roundTrip(MessagingCampaignStatusData(Nil), "MessagingCampaignStatusData")
  }

  test("Numbers models round-trip") {
    roundTrip(NumberAddRequest("2015550123", Some(4)), "NumberAddRequest")
    roundTrip(NumberRouteRequest(4), "NumberRouteRequest")
    roundTrip(NumberCnamRequest(true), "NumberCnamRequest")
    roundTrip(NumberLidbRequest("ACME", Some("ref")), "NumberLidbRequest")
    roundTrip(NumberFaxRequest("f@x.com"), "NumberFaxRequest")
    roundTrip(NumberForwardRequest("2125550123"), "NumberForwardRequest")
    roundTrip(NumberTranslationRequest("2015551235"), "NumberTranslationRequest")
    roundTrip(NumberSmsRequest("email", "f@x.com"), "NumberSmsRequest")
    roundTrip(NumberMessagingPatchRequest(Some(1), Some(2)), "NumberMessagingPatchRequest")
    roundTrip(NumberCampaignAssignRequest("C1"), "NumberCampaignAssignRequest")
    roundTrip(NumberMoveRequest(1, 4), "NumberMoveRequest")
    roundTrip(PortOutPinUpdateRequest("1234"), "PortOutPinUpdateRequest")
    val d = NumberDetail(
      Some("2015550123"), Some("2015550123"), Some(4), Some("gw"), Some(true),
      Some(true), Some("2125550123"), Some(17), Some(true), Some(true)
    )
    roundTrip(d, "NumberDetail")
    roundTrip(CampaignBinding(Some("C"), Some("A"), Some("ACTIVE"), Some("X")), "CampaignBinding")
    val job = MessagingEnablementJob(
      Some("CAMP002"), Some("assigning"), Some("placing network order"),
      Some("2026-01-01T00:00:00Z"), Some("2026-01-01T00:01:00Z")
    )
    roundTrip(job, "MessagingEnablementJob")
    roundTrip(
      NumberMessagingState(
        Some("2015550123"), Some(true), Some(true), Some(1), Some(1), Some("r"),
        Some("C"), Some(CampaignBinding(Some("C"), Some("B"), Some("ACTIVE"), Some("X"))), Some(job)
      ),
      "NumberMessagingState"
    )
    roundTrip(
      NumberMessagingState(Some("2015550123"), None, Some(true), Some(0), Some(0), Some("")),
      "NumberMessagingState (no network)"
    )
    roundTrip(NumberAddData(Some("2015550123"), Some(4)), "NumberAddData")
    roundTrip(NumberCnamData(Some("2015550123"), Some(true)), "NumberCnamData")
    roundTrip(NumberFaxData(Some("2015550123"), Some("f@x.com")), "NumberFaxData")
    roundTrip(NumberForwardData(Some("2015550123"), Some("2125550123")), "NumberForwardData")
    roundTrip(NumberLidbData(Some("2015550123"), Some("ACME"), Some("ref"), Some("Success")), "NumberLidbData")
    roundTrip(NumberMessagingPatchData(Some("2015550123"), List("routeIn")), "NumberMessagingPatchData")
    roundTrip(NumberMoveData(Some("2015550123"), Some(1), Some(4)), "NumberMoveData")
    roundTrip(NumberRouteData(Some("2015550123"), Some(4)), "NumberRouteData")
    roundTrip(NumberSmsData(Some("2015550123"), Some("email"), Some("f@x.com")), "NumberSmsData")
    roundTrip(NumberTranslationData(Some("2015550123"), Some("2015551235")), "NumberTranslationData")
    roundTrip(
      NumberMessagingCampaignAssignData(
        Some("2015550123"), Some("C1"), Some("enabled"), Some(19), Some("B"), Some("CAMP003"), Some("A"),
        Some(true), false, Some(Json.fromInt(4321)), None, None, None
      ),
      "NumberMessagingCampaignAssignData (enabled)"
    )
    roundTrip(
      NumberMessagingCampaignAssignData(
        Some("2015550123"), Some("C1"), Some("pending"), Some(1), Some("C"), Some("BANDW"), None,
        Some(false), false, None, Some("stale registration on the other messaging path"),
        Some("provisioning started"), Some(job)
      ),
      "NumberMessagingCampaignAssignData (pending)"
    )
    roundTrip(
      NumberMessagingCampaignAssignData(
        Some("2015550123"), Some("C1"), Some("already"), Some(19), Some("B"), Some("CAMP003"), None,
        Some(false), true
      ),
      "NumberMessagingCampaignAssignData (already)"
    )
    roundTrip(
      NumberMessagingCampaignUnassignData(Some("2015550123"), Some("C1"), Some("A"), Some("X"), Some(true)),
      "NumberMessagingCampaignUnassignData"
    )
    roundTrip(CampaignUnassignFailure("n", "r"), "CampaignUnassignFailure")
    roundTrip(
      NumbersMessagingCampaignUnassignData(Some("C1"), Some("A"), Some("X"), List("n"), List(CampaignUnassignFailure("x", "y"))),
      "NumbersMessagingCampaignUnassignData"
    )
    roundTrip(NumbersListData(List(d)), "NumbersListData")
    roundTrip(NumbersMessagingListData(Nil), "NumbersMessagingListData")
    roundTrip(PortOutPinUpdateData(Some("2015550123"), Some("1234"), Some(true)), "PortOutPinUpdateData (set)")
    roundTrip(PortOutPinUpdateData(Some("2015550123"), Some("9999"), Some(false)), "PortOutPinUpdateData (locked)")
  }

  test("Support models round-trip") {
    roundTrip(TicketCreateRequest("s", "m", Some("e@x.com")), "TicketCreateRequest")
    roundTrip(TicketUpdateRequest("closed"), "TicketUpdateRequest")
    roundTrip(TicketReplyRequest("ok"), "TicketReplyRequest")
    roundTrip(TicketSource(Some("v"), Some("t")), "TicketSource")
    roundTrip(TicketAction(Some("t"), Some("ty")), "TicketAction")
    roundTrip(TicketActor(Some(1), Some("user"), Some("e@x.com"), Some("F"), Some("L"), Some("u")), "TicketActor")
    roundTrip(CustomFieldValue(Some(1), Some("v"), Some("t")), "CustomFieldValue")
    roundTrip(CustomerContactEntry(Some(1), Some("v"), Some("t")), "CustomerContactEntry")
    roundTrip(CustomerWebsiteEntry(Some(1), Some("v")), "CustomerWebsiteEntry")
    roundTrip(CustomerAddress(Some("s"), Some("c"), Some("NJ"), Some("US"), Some("z")), "CustomerAddress")
    roundTrip(
      CustomerEmbedded(
        Some(CustomerAddress(Some("s"), Some("c"), Some("NJ"), Some("US"), Some("z"))),
        List(CustomerContactEntry(Some(1), Some("e@x.com"), Some("home"))),
        List(CustomerContactEntry()), List(CustomerContactEntry()),
        List(CustomerWebsiteEntry(Some(1), Some("u")))
      ),
      "CustomerEmbedded"
    )
    roundTrip(SupportAttachment(Some(1), Some("text/plain"), Some("f"), Some("u"), Some(100)), "SupportAttachment")
    roundTrip(ThreadEmbedded(List(SupportAttachment())), "ThreadEmbedded")
    val cust = SupportCustomer(
      Some(1), Some("F"), Some("L"), Some("e@x.com"),
      Some("Co"), Some("J"), Some("type"), Some("u"),
      Some("notes"), Some("customer"), Some("c"), Some("u"),
      Some(CustomerEmbedded())
    )
    roundTrip(cust, "SupportCustomer")
    val thread = SupportThread(
      Some(1), "active", Some("st"), Some("message"), Some("body"),
      Some(5), Some("ok"), Some("o"), Some("c"),
      Some(TicketSource(Some("v"))), Some(TicketAction(Some("t"))),
      Some(TicketActor(Some(1))), Some(TicketActor(Some(2))),
      Some(cust), List("a@x.com"), List("c@x.com"), List("b@x.com"),
      Some(ThreadEmbedded())
    )
    roundTrip(thread, "SupportThread")
    roundTrip(ConversationEmbedded(List(thread)), "ConversationEmbedded")
    val conv = SupportConversation(
      id = Some(1), ticketNumber = Some(1015), status = "active",
      state = Some("st"), subject = Some("sub"), preview = Some("p"),
      `type` = Some("t"), mailboxId = Some(2), folderId = Some(3),
      threadsCount = Some(4), closedBy = Some(5), closedAt = Some("c"),
      createdAt = Some("c"), updatedAt = Some("u"), userUpdatedAt = Some("u"),
      customerWaitingSince = Some(Json.obj("time" -> Json.fromInt(100))),
      source = Some(TicketSource(Some("v"))),
      createdBy = Some(TicketActor(Some(1))),
      assignee = Some(TicketActor(Some(2))),
      closedByUser = Some(TicketActor(Some(3))),
      customer = Some(cust),
      cc = List("c@x.com"), bcc = List("b@x.com"),
      customFields = List(CustomFieldValue(Some(1), Some("v"))),
      embedded = Some(ConversationEmbedded())
    )
    roundTrip(conv, "SupportConversation")
    roundTrip(TicketData(conv), "TicketData")
    roundTrip(TicketsListData(List(conv)), "TicketsListData")
    roundTrip(TicketThreadsData(List(thread)), "TicketThreadsData")
    roundTrip(TicketReplyData(Some("Reply added")), "TicketReplyData")
    roundTrip(TicketUpdateData(Some(1), "success"), "TicketUpdateData")
  }

  test("VoiceTelClientConfig defaults") {
    val cfg = VoiceTelClientConfig()
    assertEquals(cfg.baseURL, Version.DefaultBaseURL)
    assertEquals(cfg.apiKey, "")
    assertEquals(cfg.userAgent, Version.DefaultUserAgent)
    assertEquals(cfg.maxRetries, 2)
    assertEquals(cfg.backend, None)
  }

  test("VoiceTelClient.baseURL and close") {
    val c = VoiceTelClient()
    assertEquals(c.baseURL, Version.DefaultBaseURL)
    assertEquals(c.apiKey, "")
    c.close()
    // Idempotent
    c.close()
  }

  test("Version constants present") {
    assertEquals(Version.APIVersion, "v2.2.17")
    assertEquals(Version.SDKVersion, "2.2.17")
    assert(Version.DefaultBaseURL.startsWith("https://"))
    assert(Version.DefaultUserAgent.contains("voicetel-scala"))
  }

  test("Transport.unwrapEnvelope strips status/data") {
    val with2 = Json.obj("status" -> Json.fromString("success"), "data" -> Json.obj("x" -> Json.fromInt(1)))
    assertEquals(Transport.unwrapEnvelope(with2), Json.obj("x" -> Json.fromInt(1)))
    val without = Json.obj("x" -> Json.fromInt(1))
    assertEquals(Transport.unwrapEnvelope(without), without)
    assertEquals(Transport.unwrapEnvelope(Json.fromString("plain")), Json.fromString("plain"))
  }
end CodecRoundTripSuite
