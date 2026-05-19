# 📞 VoiceTel Scala SDK

The official Scala client for the [VoiceTel REST API](https://voicetel.com/docs/api/v2.2/) — provision numbers, place orders, validate e911, send messages, and manage your account, all with strongly-typed, idiomatic Scala 3.

![Version](https://img.shields.io/badge/version-2.2.10-blue)
![Scala](https://img.shields.io/badge/scala-3.3.x-DC322F)
![License](https://img.shields.io/badge/license-MIT-green)
![Coverage](https://img.shields.io/badge/coverage-90%25-brightgreen)

## 📚 Table of Contents

- [Features](#-features)
- [Installation](#-installation)
- [Quickstart](#-quickstart)
- [Authentication](#-authentication)
- [Resource Reference](#-resource-reference)
- [Error Handling](#-error-handling)
- [Rate Limits](#-rate-limits)
- [Development](#-development)
- [API Documentation](#-api-documentation)
- [Contributors](#-contributors)
- [Sponsors](#-sponsors)
- [License](#-license)

## ✨ Features

### 🛡️ Strongly Typed End-to-End
- **Native Scala 3 case classes** for every one of the 73 API operations, JSON-encoded via Circe.
- **`Option[T]` for nullable fields** — distinguish "not set" from default values cleanly.
- **Sealed `enum` types** for error kinds and order-number variants — exhaustive pattern matching at compile time.
- **`Future[T]` everywhere.** No effect-system lock-in; bring your own `ExecutionContext`.

### 🔁 Production-Grade Transport
- Built on [sttp client3](https://sttp.softwaremill.com/) with the default `HttpClientFutureBackend` (JDK 11+ HttpClient under the hood).
- **Automatic retry** with exponential backoff on 429 / 5xx, capped at 8 s. `Retry-After` honored when present.
- **Configurable retries and base URL** via `VoiceTelClientConfig`.
- **Bearer auth** managed for you; the password→key exchange is one method call (`client.login`).
- **Structured `ApiError`** with a typed `ErrorKind` — switch on `e.kind` without parsing HTTP status codes.

### 📞 Complete API Coverage
- **Numbers** — list, get, add, remove, route, translate, CNAM, LIDB, fax, forward, SMS, messaging campaigns, port-out PIN, account moves.
- **Account** — profile, sub-accounts, CDRs, credits, payments, MRC, registration, password recovery.
- **e911** — record provisioning, address validation, lookup, removal.
- **Gateways** — list, create, update, delete, view bound numbers.
- **Messaging** — SMS & MMS sending, message history, 10DLC brand and campaign registration, per-number messaging state.
- **Lookups** — CNAM and LRN dips.
- **iNumbering** — inventory search, coverage queries, number orders, port-in submissions, port-out availability.
- **Support** — ticket create / read / update / delete, threaded messages, replies.
- **ACL** — IP allowlist management with structured 409 conflict bodies.
- **Authentication** — switch between Digest, IP-only, or hybrid modes; rotate passwords.

### 🧪 Battle-Tested
- **90% statement coverage** with `sbt coverage test coverageReport` (sbt-scoverage).
- **MUnit unit tests** that exercise every method (happy path) and every error path.
- **`SttpBackendStub`-backed tests** — no live network needed.

### 📦 Clean Distribution
- Zero codegen footprint — every byte hand-written.
- Single artifact (`com.voicetel:voicetel-sdk_3:2.2.10`); pull with sbt or Maven.
- Dependencies kept minimal: sttp client3 (core + circe), circe.

## 🚀 Installation

### sbt

```scala
libraryDependencies += "com.voicetel" %% "voicetel-sdk" % "2.2.10"
```

### Maven

```xml
<dependency>
  <groupId>com.voicetel</groupId>
  <artifactId>voicetel-sdk_3</artifactId>
  <version>2.2.10</version>
</dependency>
```

Requires Scala 3.3.x (LTS) and JDK 11+.

## 🏁 Quickstart

```scala
import com.voicetel.sdk.*
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

@main def example(): Unit =
  given ExecutionContext = ExecutionContext.global

  val client = VoiceTelClient()

  try
    // Exchange username + password for an API key (one-time per session)
    Await.result(client.login(1000000001, "hunter2"), 30.seconds)

    val me = Await.result(client.account.get(), 30.seconds)
    println(s"Balance: $$${me.cash.getOrElse(0.0)}%.2f  |  Caller ID: ${me.callerId.getOrElse("")}")

    val numbers = Await.result(client.numbers.list(), 30.seconds)
    numbers.numbers.foreach { n =>
      println(s"${n.number}  route=${n.route}  cnam=${n.cnam}  sms=${n.smsEnabled}")
    }
  finally client.close()
```

Or, if you already have an API key:

```scala
import com.voicetel.sdk.*
import com.voicetel.sdk.Models.CoverageQuery
import scala.concurrent.ExecutionContext
given ExecutionContext = ExecutionContext.global

val client = VoiceTelClient(VoiceTelClientConfig(apiKey = "32hex..."))
val coverage = client.iNumbering.coverage(CoverageQuery(state = Some("NJ")))
```

## 🔑 Authentication

Every endpoint requires `Authorization: Bearer <apikey>` **except** `POST /v2.2/account/api-key`, which exchanges username + password for a fresh key. `VoiceTelClient.login()` handles the exchange and installs the returned key on the transport.

Re-fetch the API key after any password change — the old one is invalidated.

> Don't have credentials yet? Get them at **[voicetel.com/docs/api/v2.2/credentials/](https://voicetel.com/docs/api/v2.2/credentials/)**.

```scala
val client = VoiceTelClient()
val key    = Await.result(client.login(1000000001, "hunter2"), 30.seconds)
// `key` is the new 32-hex bearer; the client already has it installed.
```

## 🗺️ Resource Reference

| Resource | Field on Client | Example |
|---|---|---|
| Account | `client.account` | `client.account.cdr(Some(t1), Some(t2))` |
| ACL | `client.acl` | `client.acl.add(AclModifyRequest(...))` |
| Authentication | `client.authentication` | `client.authentication.update(AuthPutRequest(authType = Some(1)))` |
| e911 | `client.e911` | `client.e911.validate(E911AddressRequest(...))` |
| Gateways | `client.gateways` | `client.gateways.list()` |
| iNumbering | `client.iNumbering` | `client.iNumbering.searchInventory(InventoryQuery(npa = Some(201)))` |
| Lookups | `client.lookups` | `client.lookups.lrn("2015551234", "2012548000")` |
| Messaging | `client.messaging` | `client.messaging.send(MessageSendRequest(...))` |
| Numbers | `client.numbers` | `client.numbers.assignCampaign("2015551234", ...)` |
| Support | `client.support` | `client.support.create(TicketCreateRequest(...))` |

Optional request fields are typed `Option[T]` with `None` defaults:

```scala
client.account.update(AccountPutRequest(
  timezone        = Some("America/Chicago"),
  notifyThreshold = Some(5),
  notifyEnabled   = Some(true)
))
```

## 🚨 Error Handling

All HTTP errors fail the returned `Future` with an `ApiError`. Inspect `kind` or use the predicate helpers:

| Kind | HTTP status |
|---|---|
| `ErrorKind.BadRequest` | 400 |
| `ErrorKind.Authentication` | 401 |
| `ErrorKind.PermissionDenied` | 403 |
| `ErrorKind.NotFound` | 404 |
| `ErrorKind.Conflict` | 409 |
| `ErrorKind.RateLimit` | 429 |
| `ErrorKind.Server` | 5xx |
| `ErrorKind.Unknown` | other / transport |

```scala
client.numbers.get("9999999999").recover {
  case e: ApiError if ApiError.isNotFound(e)   => println("That number isn't on your account.")
  case e: ApiError if ApiError.isRateLimit(e) => println("Slow down — backoff and retry.")
  case e: ApiError                              => throw e
}
```

Or pattern-match on `kind`:

```scala
import com.voicetel.sdk.ErrorKind.*
client.numbers.get("9999999999").recover {
  case e: ApiError => e.kind match
    case NotFound          => println("not on account")
    case RateLimit         => println("backoff")
    case Conflict          => println(s"conflict body: ${e.body.getOrElse("")}")
    case BadRequest        => println(s"bad request: ${e.code.getOrElse("")}")
    case _                 => throw e
}
```

## ⏱️ Rate Limits

These endpoints are limited to **6 requests per hour per IP**:

- `account/info`
- `account/cdr`
- `account/recurring-charges`
- `account/payments`
- `account/registration`
- `account/api-key` (`client.login`)

The SDK automatically retries 429 responses with `Retry-After` honored, up to `maxRetries` (default 2). To bump it:

```scala
val client = VoiceTelClient(VoiceTelClientConfig(
  apiKey     = key,
  maxRetries = 4
))
```

## 🛠️ Development

```bash
git clone https://github.com/voicetel/scala-sdk
cd scala-sdk

# Run unit tests
sbt test

# With coverage
sbt clean coverage test coverageReport

# Open HTML report
xdg-open target/scala-3.3.4/scoverage-report/index.html
```

Integration tests are gated behind env vars. Export `VOICETEL_USERNAME` (numeric account id) and `VOICETEL_PASSWORD` to run them:

```bash
export VOICETEL_USERNAME=1000000001
export VOICETEL_PASSWORD=hunter2
sbt "testOnly com.voicetel.sdk.IntegrationSuite"
```

## 📖 API Documentation

- **Reference docs:** [voicetel.com/docs/api/v2.2/](https://voicetel.com/docs/api/v2.2/)
- **Interactive playground:** [voicetel.com/docs/api/v2.2/playground/](https://voicetel.com/docs/api/v2.2/playground/) — try the API in your browser without writing any code
- **API credentials:** [voicetel.com/docs/api/v2.2/credentials/](https://voicetel.com/docs/api/v2.2/credentials/)

## 🙌 Contributors

- [Michael Mavroudis](https://github.com/mavroudis) — Lead Developer

Contributions welcome. Open an issue describing the change, or send a pull request against `main`.

## 💖 Sponsors

| Sponsor | Contribution |
|---------|--------------|
| [VoiceTel Communications](https://voicetel.com) | Primary development and production hosting |

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
