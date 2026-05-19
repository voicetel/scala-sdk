package com.voicetel.sdk

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

/** Shared types and codecs used across the SDK. */
object Models:

  // ---------------------------------------------------------------- shared ---

  /** A single row in the IP allowlist.
    *
    * Mask must be /8, /16, /24, or /32 and must describe a routable public address.
    */
  final case class CidrEntry(cidr: String)
  object CidrEntry:
    given Codec[CidrEntry] = deriveCodec

  // ---------------------------------------------------------------- Account ---

  /** Per-service rates exposed on an account. Read-only for non-administrators. */
  final case class AccountRates(
      cnam: Option[Double] = None,
      intlMax: Option[Double] = None,
      nibble: Option[Double] = None,
      lrn: Option[Double] = None,
      fax: Option[Double] = None,
      tfAdj: Option[Double] = None,
      did: Option[Double] = None,
      mms: Option[Double] = None,
      sms: Option[Double] = None
  )
  object AccountRates:
    given Codec[AccountRates] = deriveCodec

  /** Per-service feature flags. */
  final case class AccountServices(
      e911: Option[Boolean] = None,
      cnam: Option[Boolean] = None,
      bypassMedia: Option[Boolean] = None,
      intl: Option[Boolean] = None,
      rcid: Option[Boolean] = None,
      mms: Option[Boolean] = None,
      dialer: Option[Boolean] = None,
      sms: Option[Boolean] = None
  )
  object AccountServices:
    given Codec[AccountServices] = deriveCodec

  /** The profile returned by GET /v2.2/account.
    *
    * Note: the JSON wire field `notify` clashes with `java.lang.Object.notify()`
    * so the case-class field is named `notifyEnabled`; the codec maps both ways.
    */
  final case class AccountData(
      username: Option[String] = None,
      name: Option[String] = None,
      email: Option[String] = None,
      enabled: Option[Boolean] = None,
      created: Option[String] = None,
      cash: Option[Double] = None,
      callerId: Option[String] = None,
      timezone: Option[String] = None,
      authType: Option[Int] = None,
      ccs: Option[Int] = None,
      notifyEnabled: Option[Boolean] = None,
      notifyThreshold: Option[Int] = None,
      rates: Option[AccountRates] = None,
      services: Option[AccountServices] = None
  )
  object AccountData:
    given Encoder[AccountData] = Encoder.instance { a =>
      Json
        .obj(
          "username"        -> a.username.asJson,
          "name"            -> a.name.asJson,
          "email"           -> a.email.asJson,
          "enabled"         -> a.enabled.asJson,
          "created"         -> a.created.asJson,
          "cash"            -> a.cash.asJson,
          "callerId"        -> a.callerId.asJson,
          "timezone"        -> a.timezone.asJson,
          "authType"        -> a.authType.asJson,
          "ccs"             -> a.ccs.asJson,
          "notify"          -> a.notifyEnabled.asJson,
          "notifyThreshold" -> a.notifyThreshold.asJson,
          "rates"           -> a.rates.asJson,
          "services"        -> a.services.asJson
        )
        .dropNullValues
    }
    given Decoder[AccountData] = Decoder.instance { c =>
      for
        username        <- c.get[Option[String]]("username")
        name            <- c.get[Option[String]]("name")
        email           <- c.get[Option[String]]("email")
        enabled         <- c.get[Option[Boolean]]("enabled")
        created         <- c.get[Option[String]]("created")
        cash            <- c.get[Option[Double]]("cash")
        callerId        <- c.get[Option[String]]("callerId")
        timezone        <- c.get[Option[String]]("timezone")
        authType        <- c.get[Option[Int]]("authType")
        ccs             <- c.get[Option[Int]]("ccs")
        notify          <- c.get[Option[Boolean]]("notify")
        notifyThreshold <- c.get[Option[Int]]("notifyThreshold")
        rates           <- c.get[Option[AccountRates]]("rates")
        services        <- c.get[Option[AccountServices]]("services")
      yield AccountData(
        username, name, email, enabled, created, cash, callerId, timezone,
        authType, ccs, notify, notifyThreshold, rates, services
      )
    }

  /** A single credit row. */
  final case class CreditEntry(date: String, paid: Boolean, amount: Double)
  object CreditEntry:
    given Codec[CreditEntry] = deriveCodec

  /** A single payment row. */
  final case class PaymentEntry(
      transactionId: Option[String] = None,
      date: String,
      payerEmail: Option[String] = None,
      status: String,
      amount: Double
  )
  object PaymentEntry:
    given Codec[PaymentEntry] = deriveCodec

  /** Per-call billing summary inside a CDR row. */
  final case class CdrEntryValue(
      dur: Option[String] = None,
      dst: Option[String] = None,
      ba: Option[String] = None,
      nr: Option[String] = None,
      cn: Option[String] = None,
      ip: Option[String] = None,
      cid: Option[String] = None
  )
  object CdrEntryValue:
    given Codec[CdrEntryValue] = deriveCodec

  /** One row in AccountCdrData.cdr. */
  final case class CdrEntry(id: String, key: List[String], value: CdrEntryValue)
  object CdrEntry:
    given Codec[CdrEntry] = deriveCodec

  final case class AccountCdrData(cdr: List[CdrEntry], start: Int, end: Int)
  object AccountCdrData:
    given Codec[AccountCdrData] = deriveCodec

  final case class AccountCreditsData(credits: List[CreditEntry])
  object AccountCreditsData:
    given Codec[AccountCreditsData] = deriveCodec

  final case class AccountPaymentsData(payments: List[PaymentEntry])
  object AccountPaymentsData:
    given Codec[AccountPaymentsData] = deriveCodec

  final case class MrcCharge(amount: Double, description: Option[String] = None)
  object MrcCharge:
    given Codec[MrcCharge] = deriveCodec

  final case class AccountMrcData(charges: List[MrcCharge], total: Double)
  object AccountMrcData:
    given Codec[AccountMrcData] = deriveCodec

  final case class AccountRegistrationData(
      agent: Option[String] = None,
      uri: Option[String] = None,
      expires: Option[Int] = None
  )
  object AccountRegistrationData:
    given Codec[AccountRegistrationData] = deriveCodec

  /** Body for POST /v2.2/account (admin-only sub-account creation). */
  final case class AccountAddRequest(
      username: Int,
      name: String,
      email: String,
      masterAccount: Option[Int] = None
  )
  object AccountAddRequest:
    given Codec[AccountAddRequest] = deriveCodec

  final case class AccountAddData(
      username: Option[String] = None,
      name: Option[String] = None,
      email: Option[String] = None,
      masterAccount: Option[String] = None,
      password: Option[String] = None
  )
  object AccountAddData:
    given Codec[AccountAddData] = deriveCodec

  /** Body for PUT /v2.2/account.
    *
    * Note: case-class field `notifyEnabled` is encoded as JSON `notify`.
    */
  final case class AccountPutRequest(
      notifyEnabled: Option[Boolean] = None,
      notifyThreshold: Option[Int] = None,
      timezone: Option[String] = None,
      callerId: Option[String] = None,
      e911: Option[Boolean] = None,
      intl: Option[Boolean] = None,
      sms: Option[Boolean] = None,
      mms: Option[Boolean] = None,
      ccs: Option[Int] = None
  )
  object AccountPutRequest:
    given Encoder[AccountPutRequest] = Encoder.instance { a =>
      Json
        .obj(
          "notify"          -> a.notifyEnabled.asJson,
          "notifyThreshold" -> a.notifyThreshold.asJson,
          "timezone"        -> a.timezone.asJson,
          "callerId"        -> a.callerId.asJson,
          "e911"            -> a.e911.asJson,
          "intl"            -> a.intl.asJson,
          "sms"             -> a.sms.asJson,
          "mms"             -> a.mms.asJson,
          "ccs"             -> a.ccs.asJson
        )
        .dropNullValues
    }
    given Decoder[AccountPutRequest] = Decoder.instance { c =>
      for
        notify          <- c.get[Option[Boolean]]("notify")
        notifyThreshold <- c.get[Option[Int]]("notifyThreshold")
        timezone        <- c.get[Option[String]]("timezone")
        callerId        <- c.get[Option[String]]("callerId")
        e911            <- c.get[Option[Boolean]]("e911")
        intl            <- c.get[Option[Boolean]]("intl")
        sms             <- c.get[Option[Boolean]]("sms")
        mms             <- c.get[Option[Boolean]]("mms")
        ccs             <- c.get[Option[Int]]("ccs")
      yield AccountPutRequest(notify, notifyThreshold, timezone, callerId, e911, intl, sms, mms, ccs)
    }

  final case class AccountPutData(updated: List[String])
  object AccountPutData:
    given Codec[AccountPutData] = deriveCodec

  final case class AccountSignupRequest(name: String, email: String, promo: Option[String] = None)
  object AccountSignupRequest:
    given Codec[AccountSignupRequest] = deriveCodec

  final case class AccountSignupData(
      username: Option[String] = None,
      name: Option[String] = None,
      email: Option[String] = None,
      password: Option[String] = None
  )
  object AccountSignupData:
    given Codec[AccountSignupData] = deriveCodec

  final case class AccountRecoverRequest(email: String)
  object AccountRecoverRequest:
    given Codec[AccountRecoverRequest] = deriveCodec

  final case class AccountRecoverData(message: Option[String] = None)
  object AccountRecoverData:
    given Codec[AccountRecoverData] = deriveCodec

  final case class AccountApiKeyData(apikey: String)
  object AccountApiKeyData:
    given Codec[AccountApiKeyData] = deriveCodec

  // ---------------------------------------------------------------- ACL ---

  final case class AclModifyRequest(acl: List[CidrEntry])
  object AclModifyRequest:
    given Codec[AclModifyRequest] = deriveCodec

  final case class AclListData(acl: List[CidrEntry])
  object AclListData:
    given Codec[AclListData] = deriveCodec

  final case class AclAddData(added: List[CidrEntry])
  object AclAddData:
    given Codec[AclAddData] = deriveCodec

  final case class AclRemoveData(removed: List[CidrEntry])
  object AclRemoveData:
    given Codec[AclRemoveData] = deriveCodec

  /** A CIDR that was rejected by add/remove, with a reason string.
    *
    * Reason is one of "DB Insert failed", "DB delete failed",
    * "Invalid mask: must be /8, /16, /24, or /32", "CIDR range must be routable".
    */
  final case class AclFailedEntry(cidr: String, reason: String)
  object AclFailedEntry:
    given Codec[AclFailedEntry] = deriveCodec

  final case class AclConflictData(
      added: List[CidrEntry] = Nil,
      removed: List[CidrEntry] = Nil,
      failed: List[AclFailedEntry] = Nil
  )
  object AclConflictData:
    given Codec[AclConflictData] = deriveCodec

  // -------------------------------------------------------- Authentication ---

  /** Auth-mode constants for AuthPutRequest.authType and AuthGetData.authType.
    *
    *  0 = Digest, 1 = IP Auth, 2 = Digest OR IP, 3 = Digest AND IP.
    */
  object AuthType:
    val Digest: Int      = 0
    val IPAuth: Int      = 1
    val DigestOrIP: Int  = 2
    val DigestAndIP: Int = 3

  final case class AuthPutRequest(
      authType: Option[Int] = None,
      password: Option[String] = None
  )
  object AuthPutRequest:
    given Codec[AuthPutRequest] = deriveCodec

  final case class AuthGetData(
      authType: Int,
      authTypeDescription: String,
      acl: List[CidrEntry]
  )
  object AuthGetData:
    given Codec[AuthGetData] = deriveCodec

  final case class AuthUpdatedEntry(field: String, value: Option[Int] = None)
  object AuthUpdatedEntry:
    given Codec[AuthUpdatedEntry] = deriveCodec

  final case class AuthPutData(updated: List[AuthUpdatedEntry])
  object AuthPutData:
    given Codec[AuthPutData] = deriveCodec

  final case class AuthPutConflictData(updated: List[AuthUpdatedEntry] = Nil)
  object AuthPutConflictData:
    given Codec[AuthPutConflictData] = deriveCodec

  // ---------------------------------------------------------------- e911 ---

  final case class E911AddressRequest(
      address1: String,
      address2: Option[String] = None,
      city: String,
      state: String,
      zip: String
  )
  object E911AddressRequest:
    given Codec[E911AddressRequest] = deriveCodec

  final case class E911CreateRequest(
      dn: String,
      callername: String,
      address1: String,
      address2: Option[String] = None,
      city: String,
      state: String,
      zip: String
  )
  object E911CreateRequest:
    given Codec[E911CreateRequest] = deriveCodec

  final case class E911ProvisionByIDRequest(callername: String, addressid: Int)
  object E911ProvisionByIDRequest:
    given Codec[E911ProvisionByIDRequest] = deriveCodec

  final case class E911Entry(
      dn: String,
      callername: String,
      address1: String,
      address2: Option[String] = None,
      city: String,
      state: String,
      zip: String
  )
  object E911Entry:
    given Codec[E911Entry] = deriveCodec

  final case class E911ValidatedAddress(
      addressid: Int,
      address1: String,
      address2: Option[String] = None,
      city: String,
      state: String,
      zip: String
  )
  object E911ValidatedAddress:
    given Codec[E911ValidatedAddress] = deriveCodec

  final case class E911AllData(records: List[E911Entry])
  object E911AllData:
    given Codec[E911AllData] = deriveCodec

  final case class E911RecordData(record: E911Entry)
  object E911RecordData:
    given Codec[E911RecordData] = deriveCodec

  final case class E911ValidateData(address: E911ValidatedAddress)
  object E911ValidateData:
    given Codec[E911ValidateData] = deriveCodec

  // ---------------------------------------------------------------- gateways ---

  final case class GatewayAddRequest(
      gateway: String,
      prefix: Option[String] = None,
      limit: Option[Int] = None
  )
  object GatewayAddRequest:
    given Codec[GatewayAddRequest] = deriveCodec

  final case class GatewayUpdateRequest(
      gateway: Option[String] = None,
      prefix: Option[String] = None,
      limit: Option[Int] = None
  )
  object GatewayUpdateRequest:
    given Codec[GatewayUpdateRequest] = deriveCodec

  final case class GatewayEntry(
      id: Option[Int] = None,
      gateway: Option[String] = None,
      prefix: Option[String] = None,
      limit: Option[Int] = None,
      system: Option[Boolean] = None
  )
  object GatewayEntry:
    given Codec[GatewayEntry] = deriveCodec

  final case class GatewayNumberSummary(
      number: String,
      translated: String,
      forward: Boolean,
      forwardTo: Option[String] = None,
      cnam: Boolean,
      carrier: Int,
      smsEnabled: Boolean,
      faxEnabled: Boolean
  )
  object GatewayNumberSummary:
    given Codec[GatewayNumberSummary] = deriveCodec

  final case class GatewaysListData(gateways: List[GatewayEntry])
  object GatewaysListData:
    given Codec[GatewaysListData] = deriveCodec

  final case class GatewayNumbersData(numbers: List[GatewayNumberSummary])
  object GatewayNumbersData:
    given Codec[GatewayNumbersData] = deriveCodec

  // ---------------------------------------------------------------- iNumbering ---

  final case class OrderNumberSpec(number: String, route: Option[Int] = None)
  object OrderNumberSpec:
    given Codec[OrderNumberSpec] = deriveCodec

  /** A single entry in OrderCreateRequest.numbers. Use OrderNumber.Value for a
    * plain TN string, or OrderNumber.Spec for a {number, route} object.
    */
  enum OrderNumber:
    case Value(number: String)
    case Spec(spec: OrderNumberSpec)
  object OrderNumber:
    given Encoder[OrderNumber] = Encoder.instance {
      case Value(n) => Json.fromString(n)
      case Spec(s)  => s.asJson
    }
    given Decoder[OrderNumber] = Decoder.instance { c =>
      c.as[String].map(OrderNumber.Value(_)).left.flatMap { _ =>
        c.as[OrderNumberSpec].map(OrderNumber.Spec(_))
      }
    }

  final case class OrderCreateRequest(numbers: List[OrderNumber])
  object OrderCreateRequest:
    given Codec[OrderCreateRequest] = deriveCodec

  final case class PortFeatureLidb(name: String)
  object PortFeatureLidb:
    given Codec[PortFeatureLidb] = deriveCodec

  final case class PortFeatureRouting(gatewayId: Int)
  object PortFeatureRouting:
    given Codec[PortFeatureRouting] = deriveCodec

  final case class PortFeatureSms(campaignId: Option[String] = None)
  object PortFeatureSms:
    given Codec[PortFeatureSms] = deriveCodec

  final case class PortFeature(
      number: String,
      routing: Option[PortFeatureRouting] = None,
      lidb: Option[PortFeatureLidb] = None,
      sms: Option[PortFeatureSms] = None
  )
  object PortFeature:
    given Codec[PortFeature] = deriveCodec

  final case class PortSubmitRequest(
      did: List[String],
      name: String,
      nameType: String,
      lcBtn: String,
      lcAccountNumber: String,
      streetNumber: String,
      street: String,
      streetType: String,
      city: String,
      state: String,
      zip: String,
      country: String,
      authPerson: String,
      streetPrefix: Option[String] = None,
      streetSuffix: Option[String] = None,
      floor: Option[String] = None,
      room: Option[String] = None,
      building: Option[String] = None,
      unitValue: Option[String] = None,
      desiredDueDate: Option[String] = None,
      pin: Option[String] = None,
      features: List[PortFeature] = Nil
  )
  object PortSubmitRequest:
    given Codec[PortSubmitRequest] = deriveCodec

  final case class InventoryItem(
      number: String,
      rateCenter: String,
      city: String,
      province: String,
      lata: String
  )
  object InventoryItem:
    given Codec[InventoryItem] = deriveCodec

  final case class InventoryCoverageItem(
      count: Int,
      npa: Option[String] = None,
      nxx: Option[String] = None,
      block: Option[String] = None,
      city: Option[String] = None,
      rcAbbre: Option[String] = None,
      lata: Option[String] = None,
      locState: Option[String] = None
  )
  object InventoryCoverageItem:
    given Codec[InventoryCoverageItem] = deriveCodec

  final case class PortSummary(
      status: String,
      id: Option[String] = None,
      pid: Option[String] = None,
      foc: Option[String] = None,
      createdAt: Option[String] = None,
      message: Option[String] = None,
      supportUrl: Option[String] = None
  )
  object PortSummary:
    given Codec[PortSummary] = deriveCodec

  final case class PortDetail(
      status: String,
      id: Option[String] = None,
      pid: Option[String] = None,
      name: Option[String] = None,
      email: Option[String] = None,
      foc: Option[String] = None,
      createdAt: Option[String] = None,
      numbers: List[String] = Nil,
      message: Option[String] = None
  )
  object PortDetail:
    given Codec[PortDetail] = deriveCodec

  final case class InventorySearchData(numbers: List[InventoryItem])
  object InventorySearchData:
    given Codec[InventorySearchData] = deriveCodec

  final case class InventoryCoverageData(coverage: List[InventoryCoverageItem])
  object InventoryCoverageData:
    given Codec[InventoryCoverageData] = deriveCodec

  final case class OrderFailedEntry(number: String, reason: String)
  object OrderFailedEntry:
    given Codec[OrderFailedEntry] = deriveCodec

  final case class OrderCreateData(
      orderId: String,
      amountCharged: Double,
      numbersOrdered: List[String],
      failed: List[OrderFailedEntry] = Nil
  )
  object OrderCreateData:
    given Codec[OrderCreateData] = deriveCodec

  final case class PortListData(ports: List[PortSummary])
  object PortListData:
    given Codec[PortListData] = deriveCodec

  final case class PortDetailData(port: PortDetail)
  object PortDetailData:
    given Codec[PortDetailData] = deriveCodec

  final case class PortSubmitData(
      pid: String,
      ticket: Int,
      message: String,
      loaUrl: String,
      portUrl: String
  )
  object PortSubmitData:
    given Codec[PortSubmitData] = deriveCodec

  /** Response for GET /v2.2/ports/availability/{number}.
    *
    * v2.2.10 adds localRoutingNumber and rateCenterTier.
    */
  final case class PortAvailabilityData(
      number: String,
      portable: Boolean,
      losingCarrier: Option[String] = None,
      localRoutingNumber: Option[String] = None,
      rateCenterTier: Option[String] = None,
      reason: Option[String] = None
  )
  object PortAvailabilityData:
    given Codec[PortAvailabilityData] = deriveCodec

  /** Query filters for INumberingService.searchInventory. */
  final case class InventoryQuery(
      npa: Option[Int] = None,
      nxx: Option[Int] = None,
      state: Option[String] = None,
      rateCenter: Option[String] = None,
      contains: Option[String] = None,
      endsWith: Option[String] = None,
      limit: Option[Int] = None
  )

  /** Query filters for INumberingService.coverage. */
  final case class CoverageQuery(
      state: Option[String] = None,
      rateCenter: Option[String] = None
  )

  // ---------------------------------------------------------------- lookups ---

  final case class CnamData(cnam: Option[String] = None, number: String)
  object CnamData:
    given Codec[CnamData] = deriveCodec

  final case class LrnData(
      lrn: Option[String] = None,
      state: Option[String] = None,
      city: Option[String] = None,
      rc: Option[String] = None,
      lata: Option[String] = None,
      ocn: Option[String] = None,
      lec: Option[String] = None,
      lecType: Option[String] = None,
      jurisdiction: Option[String] = None,
      local: Option[String] = None
  )
  object LrnData:
    given Codec[LrnData] = deriveCodec

  final case class LrnLookupData(ani: String, destination: String, lrn: LrnData)
  object LrnLookupData:
    given Codec[LrnLookupData] = deriveCodec

  // ---------------------------------------------------------------- messaging ---

  /** Body for POST /v2.2/messages. Wire fields are `fromNumber` and `toNumber`. */
  final case class MessageSendRequest(
      fromNumber: String,
      toNumber: String,
      text: String,
      subject: Option[String] = None,
      mediaUrls: List[String] = Nil
  )
  object MessageSendRequest:
    given Codec[MessageSendRequest] = deriveCodec

  final case class MessagingBrandCreateRequest(
      messagingBrandId: String,
      messagingBrandName: String,
      messagingBrandDescription: Option[String] = None
  )
  object MessagingBrandCreateRequest:
    given Codec[MessagingBrandCreateRequest] = deriveCodec

  final case class MessagingCampaignCreateRequest(
      messagingBrandId: String,
      externalCampaignId: String,
      campaignDescription: String,
      campaignClassName: Option[String] = None,
      campaignStartDate: Option[String] = None
  )
  object MessagingCampaignCreateRequest:
    given Codec[MessagingCampaignCreateRequest] = deriveCodec

  final case class MessageRecordValue(
      sourceNumber: Option[String] = None,
      destinationNumber: Option[String] = None,
      direction: Option[String] = None,
      rate: Option[String] = None,
      number: Option[Long] = None,
      message: Option[String] = None
  )
  object MessageRecordValue:
    given Codec[MessageRecordValue] = deriveCodec

  final case class MessageRecord(id: String, key: List[Json], value: MessageRecordValue)
  object MessageRecord:
    given Codec[MessageRecord] = deriveCodec

  final case class MessageHistoryData(
      number: String,
      `type`: String,
      fromTs: Int,
      toTs: Int,
      messages: List[MessageRecord]
  )
  object MessageHistoryData:
    given Codec[MessageHistoryData] = deriveCodec

  final case class MessageSendData(
      id: String,
      `type`: String,
      fromNumber: String,
      toNumber: String,
      parts: Int,
      subject: Option[String] = None,
      mediaUrls: List[String] = Nil
  )
  object MessageSendData:
    given Codec[MessageSendData] = deriveCodec

  final case class BrandRegistrationResult(statusCode: String, status: String)
  object BrandRegistrationResult:
    given Codec[BrandRegistrationResult] = deriveCodec

  final case class MessagingBrandCreateData(result: BrandRegistrationResult)
  object MessagingBrandCreateData:
    given Codec[MessagingBrandCreateData] = deriveCodec

  final case class CampaignRegistrationResult(statusCode: String, status: String)
  object CampaignRegistrationResult:
    given Codec[CampaignRegistrationResult] = deriveCodec

  final case class MessagingCampaignCreateData(result: CampaignRegistrationResult)
  object MessagingCampaignCreateData:
    given Codec[MessagingCampaignCreateData] = deriveCodec

  final case class CampaignStatusItem(id: String, status: String, numbers: List[String])
  object CampaignStatusItem:
    given Codec[CampaignStatusItem] = deriveCodec

  final case class MessagingCampaignStatusData(campaigns: List[CampaignStatusItem])
  object MessagingCampaignStatusData:
    given Codec[MessagingCampaignStatusData] = deriveCodec

  /** Optional query filters for MessagingService.history. */
  final case class HistoryOptions(
      number: Option[String] = None,
      start: Option[Int] = None,
      end: Option[Int] = None,
      `type`: Option[String] = None
  )

  // ---------------------------------------------------------------- numbers ---

  final case class NumberAddRequest(number: String, route: Option[Int] = None)
  object NumberAddRequest:
    given Codec[NumberAddRequest] = deriveCodec

  final case class NumberRouteRequest(route: Int)
  object NumberRouteRequest:
    given Codec[NumberRouteRequest] = deriveCodec

  final case class NumberCnamRequest(enabled: Boolean)
  object NumberCnamRequest:
    given Codec[NumberCnamRequest] = deriveCodec

  final case class NumberLidbRequest(cnam: String, customerOrderReference: Option[String] = None)
  object NumberLidbRequest:
    given Codec[NumberLidbRequest] = deriveCodec

  final case class NumberFaxRequest(email: String)
  object NumberFaxRequest:
    given Codec[NumberFaxRequest] = deriveCodec

  final case class NumberForwardRequest(destination: String)
  object NumberForwardRequest:
    given Codec[NumberForwardRequest] = deriveCodec

  final case class NumberTranslationRequest(translation: String)
  object NumberTranslationRequest:
    given Codec[NumberTranslationRequest] = deriveCodec

  final case class NumberSmsRequest(`type`: String, resource: String)
  object NumberSmsRequest:
    given Codec[NumberSmsRequest] = deriveCodec

  final case class NumberMessagingPatchRequest(
      routeIn: Option[Int] = None,
      routeOut: Option[Int] = None
  )
  object NumberMessagingPatchRequest:
    given Codec[NumberMessagingPatchRequest] = deriveCodec

  final case class NumberCampaignAssignRequest(campaignId: String)
  object NumberCampaignAssignRequest:
    given Codec[NumberCampaignAssignRequest] = deriveCodec

  final case class NumberMoveRequest(accountId: Int, route: Int)
  object NumberMoveRequest:
    given Codec[NumberMoveRequest] = deriveCodec

  final case class PortOutPinUpdateRequest(pin: String)
  object PortOutPinUpdateRequest:
    given Codec[PortOutPinUpdateRequest] = deriveCodec

  final case class BulkUnassignRequest(numbers: List[String])
  object BulkUnassignRequest:
    given Codec[BulkUnassignRequest] = deriveCodec

  final case class NumberDetail(
      number: String,
      translated: String,
      route: Int,
      gateway: Option[String] = None,
      cnam: Boolean,
      forward: Boolean,
      forwardTo: Option[String] = None,
      carrier: Int,
      smsEnabled: Boolean,
      faxEnabled: Boolean
  )
  object NumberDetail:
    given Codec[NumberDetail] = deriveCodec

  final case class CampaignBinding(
      id: String,
      network: String,
      status: String,
      upstreamCnpId: String
  )
  object CampaignBinding:
    given Codec[CampaignBinding] = deriveCodec

  final case class NumberMessagingState(
      number: String,
      onAccount: Option[Boolean] = None,
      enabled: Boolean,
      carrier: Int,
      routeIn: Int,
      resource: String,
      network: Option[String] = None,
      campaign: Option[CampaignBinding] = None
  )
  object NumberMessagingState:
    given Codec[NumberMessagingState] = deriveCodec

  final case class NumberAddData(number: String, route: Int)
  object NumberAddData:
    given Codec[NumberAddData] = deriveCodec

  final case class NumberCnamData(number: String, cnam: Boolean)
  object NumberCnamData:
    given Codec[NumberCnamData] = deriveCodec

  final case class NumberFaxData(number: String, email: String)
  object NumberFaxData:
    given Codec[NumberFaxData] = deriveCodec

  final case class NumberForwardData(number: String, forwardTo: Option[String] = None)
  object NumberForwardData:
    given Codec[NumberForwardData] = deriveCodec

  final case class NumberLidbData(
      number: String,
      cnam: String,
      customerOrderReference: String,
      carrierStatus: String
  )
  object NumberLidbData:
    given Codec[NumberLidbData] = deriveCodec

  final case class NumberMessagingPatchData(number: String, updated: List[String])
  object NumberMessagingPatchData:
    given Codec[NumberMessagingPatchData] = deriveCodec

  final case class NumberMoveData(number: String, accountId: Int, route: Int)
  object NumberMoveData:
    given Codec[NumberMoveData] = deriveCodec

  final case class NumberRouteData(number: String, route: Int)
  object NumberRouteData:
    given Codec[NumberRouteData] = deriveCodec

  final case class NumberSmsData(number: String, `type`: String, resource: String)
  object NumberSmsData:
    given Codec[NumberSmsData] = deriveCodec

  final case class NumberTranslationData(number: String, translation: String)
  object NumberTranslationData:
    given Codec[NumberTranslationData] = deriveCodec

  final case class NumberMessagingCampaignAssignData(
      number: String,
      campaignId: String,
      carrier: Int,
      network: Option[String] = None,
      upstreamCnpId: Option[String] = None,
      previousNetwork: Option[String] = None,
      previousNetworkCleared: Boolean
  )
  object NumberMessagingCampaignAssignData:
    given Codec[NumberMessagingCampaignAssignData] = deriveCodec

  final case class NumberMessagingCampaignUnassignData(
      number: String,
      campaignId: String,
      network: Option[String] = None,
      upstreamCnpId: Option[String] = None,
      unassigned: Boolean
  )
  object NumberMessagingCampaignUnassignData:
    given Codec[NumberMessagingCampaignUnassignData] = deriveCodec

  final case class CampaignUnassignFailure(number: String, reason: String)
  object CampaignUnassignFailure:
    given Codec[CampaignUnassignFailure] = deriveCodec

  final case class NumbersMessagingCampaignUnassignData(
      campaignId: String,
      network: Option[String] = None,
      upstreamCnpId: Option[String] = None,
      unassignedNumbers: List[String],
      failed: List[CampaignUnassignFailure] = Nil
  )
  object NumbersMessagingCampaignUnassignData:
    given Codec[NumbersMessagingCampaignUnassignData] = deriveCodec

  final case class NumbersListData(numbers: List[NumberDetail])
  object NumbersListData:
    given Codec[NumbersListData] = deriveCodec

  final case class NumbersMessagingListData(numbers: List[NumberMessagingState])
  object NumbersMessagingListData:
    given Codec[NumbersMessagingListData] = deriveCodec

  final case class PortOutPinUpdateData(number: String, portOutPin: String)
  object PortOutPinUpdateData:
    given Codec[PortOutPinUpdateData] = deriveCodec

  // ---------------------------------------------------------------- support ---

  final case class TicketCreateRequest(
      subject: String,
      message: String,
      email: Option[String] = None
  )
  object TicketCreateRequest:
    given Codec[TicketCreateRequest] = deriveCodec

  final case class TicketUpdateRequest(status: String)
  object TicketUpdateRequest:
    given Codec[TicketUpdateRequest] = deriveCodec

  final case class TicketReplyRequest(message: String)
  object TicketReplyRequest:
    given Codec[TicketReplyRequest] = deriveCodec

  final case class TicketSource(via: Option[String] = None, `type`: Option[String] = None)
  object TicketSource:
    given Codec[TicketSource] = deriveCodec

  final case class TicketAction(text: Option[String] = None, `type`: Option[String] = None)
  object TicketAction:
    given Codec[TicketAction] = deriveCodec

  final case class TicketActor(
      id: Option[Int] = None,
      `type`: Option[String] = None,
      email: Option[String] = None,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      photoUrl: Option[String] = None
  )
  object TicketActor:
    given Codec[TicketActor] = deriveCodec

  final case class CustomFieldValue(
      id: Option[Int] = None,
      value: Option[String] = None,
      text: Option[String] = None
  )
  object CustomFieldValue:
    given Codec[CustomFieldValue] = deriveCodec

  final case class CustomerContactEntry(
      id: Option[Int] = None,
      value: Option[String] = None,
      `type`: Option[String] = None
  )
  object CustomerContactEntry:
    given Codec[CustomerContactEntry] = deriveCodec

  final case class CustomerWebsiteEntry(id: Option[Int] = None, value: Option[String] = None)
  object CustomerWebsiteEntry:
    given Codec[CustomerWebsiteEntry] = deriveCodec

  final case class CustomerAddress(
      street: Option[String] = None,
      city: Option[String] = None,
      state: Option[String] = None,
      country: Option[String] = None,
      zip: Option[String] = None
  )
  object CustomerAddress:
    given Codec[CustomerAddress] = deriveCodec

  final case class CustomerEmbedded(
      address: Option[CustomerAddress] = None,
      emails: List[CustomerContactEntry] = Nil,
      phones: List[CustomerContactEntry] = Nil,
      socialProfiles: List[CustomerContactEntry] = Nil,
      websites: List[CustomerWebsiteEntry] = Nil
  )
  object CustomerEmbedded:
    given Codec[CustomerEmbedded] = deriveCodec

  final case class SupportAttachment(
      id: Option[Int] = None,
      mimeType: Option[String] = None,
      fileName: Option[String] = None,
      fileUrl: Option[String] = None,
      size: Option[Int] = None
  )
  object SupportAttachment:
    given Codec[SupportAttachment] = deriveCodec

  final case class ThreadEmbedded(attachments: List[SupportAttachment] = Nil)
  object ThreadEmbedded:
    given Codec[ThreadEmbedded] = deriveCodec

  final case class SupportCustomer(
      id: Option[Int] = None,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      email: Option[String] = None,
      company: Option[String] = None,
      jobTitle: Option[String] = None,
      photoType: Option[String] = None,
      photoUrl: Option[String] = None,
      notes: Option[String] = None,
      `type`: Option[String] = None,
      createdAt: Option[String] = None,
      updatedAt: Option[String] = None,
      embedded: Option[CustomerEmbedded] = None
  )
  object SupportCustomer:
    given Codec[SupportCustomer] = deriveCodec

  final case class SupportThread(
      id: Option[Int] = None,
      status: String,
      state: Option[String] = None,
      `type`: Option[String] = None,
      body: Option[String] = None,
      rating: Option[Int] = None,
      ratingComment: Option[String] = None,
      openedAt: Option[String] = None,
      createdAt: Option[String] = None,
      source: Option[TicketSource] = None,
      action: Option[TicketAction] = None,
      createdBy: Option[TicketActor] = None,
      assignedTo: Option[TicketActor] = None,
      customer: Option[SupportCustomer] = None,
      to: List[String] = Nil,
      cc: List[String] = Nil,
      bcc: List[String] = Nil,
      embedded: Option[ThreadEmbedded] = None
  )
  object SupportThread:
    given Codec[SupportThread] = deriveCodec

  final case class ConversationEmbedded(threads: List[SupportThread] = Nil)
  object ConversationEmbedded:
    given Codec[ConversationEmbedded] = deriveCodec

  /** A support ticket.
    *
    * Note: the wire field "number" is a ticket sequence number (1015, 2114, ...),
    * NOT a phone number. It is surfaced as `ticketNumber` to avoid confusion
    * with 10-digit TNs everywhere else in this API.
    */
  final case class SupportConversation(
      id: Option[Int] = None,
      ticketNumber: Option[Int] = None,
      status: String,
      state: Option[String] = None,
      subject: Option[String] = None,
      preview: Option[String] = None,
      `type`: Option[String] = None,
      mailboxId: Option[Int] = None,
      folderId: Option[Int] = None,
      threadsCount: Option[Int] = None,
      closedBy: Option[Int] = None,
      closedAt: Option[String] = None,
      createdAt: Option[String] = None,
      updatedAt: Option[String] = None,
      userUpdatedAt: Option[String] = None,
      customerWaitingSince: Option[Json] = None,
      source: Option[TicketSource] = None,
      createdBy: Option[TicketActor] = None,
      assignee: Option[TicketActor] = None,
      closedByUser: Option[TicketActor] = None,
      customer: Option[SupportCustomer] = None,
      cc: List[String] = Nil,
      bcc: List[String] = Nil,
      customFields: List[CustomFieldValue] = Nil,
      embedded: Option[ConversationEmbedded] = None
  )
  object SupportConversation:
    given Encoder[SupportConversation] = Encoder.instance { sc =>
      Json.obj(
        "id"                   -> sc.id.asJson,
        "number"               -> sc.ticketNumber.asJson,
        "status"               -> sc.status.asJson,
        "state"                -> sc.state.asJson,
        "subject"              -> sc.subject.asJson,
        "preview"              -> sc.preview.asJson,
        "type"                 -> sc.`type`.asJson,
        "mailboxId"            -> sc.mailboxId.asJson,
        "folderId"             -> sc.folderId.asJson,
        "threadsCount"         -> sc.threadsCount.asJson,
        "closedBy"             -> sc.closedBy.asJson,
        "closedAt"             -> sc.closedAt.asJson,
        "createdAt"            -> sc.createdAt.asJson,
        "updatedAt"            -> sc.updatedAt.asJson,
        "userUpdatedAt"        -> sc.userUpdatedAt.asJson,
        "customerWaitingSince" -> sc.customerWaitingSince.asJson,
        "source"               -> sc.source.asJson,
        "createdBy"            -> sc.createdBy.asJson,
        "assignee"             -> sc.assignee.asJson,
        "closedByUser"         -> sc.closedByUser.asJson,
        "customer"             -> sc.customer.asJson,
        "cc"                   -> (if sc.cc.isEmpty then Json.Null else sc.cc.asJson),
        "bcc"                  -> (if sc.bcc.isEmpty then Json.Null else sc.bcc.asJson),
        "customFields"         -> (if sc.customFields.isEmpty then Json.Null else sc.customFields.asJson),
        "embedded"             -> sc.embedded.asJson
      ).dropNullValues
    }
    given Decoder[SupportConversation] = Decoder.instance { c =>
      for
        id                   <- c.get[Option[Int]]("id")
        ticketNumber         <- c.get[Option[Int]]("number")
        status               <- c.get[String]("status")
        state                <- c.get[Option[String]]("state")
        subject              <- c.get[Option[String]]("subject")
        preview              <- c.get[Option[String]]("preview")
        tpe                  <- c.get[Option[String]]("type")
        mailboxId            <- c.get[Option[Int]]("mailboxId")
        folderId             <- c.get[Option[Int]]("folderId")
        threadsCount         <- c.get[Option[Int]]("threadsCount")
        closedBy             <- c.get[Option[Int]]("closedBy")
        closedAt             <- c.get[Option[String]]("closedAt")
        createdAt            <- c.get[Option[String]]("createdAt")
        updatedAt            <- c.get[Option[String]]("updatedAt")
        userUpdatedAt        <- c.get[Option[String]]("userUpdatedAt")
        customerWaitingSince <- c.get[Option[Json]]("customerWaitingSince")
        source               <- c.get[Option[TicketSource]]("source")
        createdBy            <- c.get[Option[TicketActor]]("createdBy")
        assignee             <- c.get[Option[TicketActor]]("assignee")
        closedByUser         <- c.get[Option[TicketActor]]("closedByUser")
        customer             <- c.get[Option[SupportCustomer]]("customer")
        cc                   <- c.getOrElse[List[String]]("cc")(Nil)
        bcc                  <- c.getOrElse[List[String]]("bcc")(Nil)
        customFields         <- c.getOrElse[List[CustomFieldValue]]("customFields")(Nil)
        embedded             <- c.get[Option[ConversationEmbedded]]("embedded")
      yield SupportConversation(
        id, ticketNumber, status, state, subject, preview, tpe,
        mailboxId, folderId, threadsCount, closedBy, closedAt,
        createdAt, updatedAt, userUpdatedAt, customerWaitingSince,
        source, createdBy, assignee, closedByUser, customer,
        cc, bcc, customFields, embedded
      )
    }

  final case class TicketData(ticket: SupportConversation)
  object TicketData:
    given Codec[TicketData] = deriveCodec

  final case class TicketsListData(tickets: List[SupportConversation])
  object TicketsListData:
    given Codec[TicketsListData] = deriveCodec

  final case class TicketThreadsData(messages: List[SupportThread])
  object TicketThreadsData:
    given Codec[TicketThreadsData] = deriveCodec

  final case class TicketReplyData(message: String)
  object TicketReplyData:
    given Codec[TicketReplyData] = deriveCodec

  final case class TicketUpdateData(id: Option[Int] = None, status: String)
  object TicketUpdateData:
    given Codec[TicketUpdateData] = deriveCodec
