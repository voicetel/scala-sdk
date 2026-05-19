package com.voicetel.sdk

import com.voicetel.sdk.TestSupport.*
import io.circe.Json
import sttp.client3.Response
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, StatusCode}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class TransportSuite extends munit.FunSuite:
  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  test("login installs bearer and returns api key") {
    val backend = stubOk(envelope(Json.obj("apikey" -> Json.fromString("32hex"))))
    val client  = unauthClient(backend)
    val key     = Await.result(client.login(1000000001, "hunter2"), 5.seconds)
    assertEquals(key, "32hex")
    assertEquals(client.apiKey, "32hex")
  }

  test("login rejects empty apikey response") {
    val backend = stubOk(envelope(Json.obj("apikey" -> Json.fromString(""))))
    val client  = unauthClient(backend)
    val t       = intercept[ApiError](Await.result(client.login(1, "p"), 5.seconds))
    assertEquals(t.kind, ErrorKind.Authentication)
  }

  test("missing api key on auth-required request -> Authentication error") {
    val backend = stubOk("{}")
    val client  = unauthClient(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Authentication)
  }

  test("HTTP 400 maps to ErrorKind.BadRequest") {
    val backend = stubStatus(400, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.BadRequest)
    assertEquals(t.statusCode, 400)
    assertEquals(t.code, Some("X"))
  }

  test("HTTP 401 maps to ErrorKind.Authentication") {
    val backend = stubStatus(401, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Authentication)
    assert(ApiError.isAuthentication(t))
  }

  test("HTTP 403 maps to ErrorKind.PermissionDenied") {
    val backend = stubStatus(403, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.PermissionDenied)
  }

  test("HTTP 404 maps to ErrorKind.NotFound") {
    val backend = stubStatus(404, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.NotFound)
    assert(ApiError.isNotFound(t))
  }

  test("HTTP 409 maps to ErrorKind.Conflict") {
    val backend = stubStatus(409, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.acl.add(Models.AclModifyRequest(Nil)), 5.seconds))
    assertEquals(t.kind, ErrorKind.Conflict)
    assert(ApiError.isConflict(t))
  }

  test("HTTP 429 (no retries) maps to ErrorKind.RateLimit") {
    val backend = stubStatus(429, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.RateLimit)
    assert(ApiError.isRateLimit(t))
  }

  test("HTTP 500 maps to ErrorKind.Server") {
    val backend = stubStatus(500, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Server)
  }

  test("retries on 429 then succeeds") {
    val attempts = new AtomicInteger(0)
    val okBody   = envelope(Json.obj("acl" -> Json.arr()))
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespondF { _ =>
        val n = attempts.incrementAndGet()
        Future.successful(
          if n <= 1 then
            Response("rate", StatusCode(429), "", List(Header("Retry-After", "0")))
          else Response(okBody, StatusCode(200))
        )
      }
    val client = clientFor(backend, retries = 2)
    val r      = Await.result(client.acl.list(), 5.seconds)
    assertEquals(r.acl.size, 0)
    assert(attempts.get() >= 2)
  }

  test("unknown / non-numeric status (e.g. 418) returns Unknown") {
    val backend = stubStatus(418, errorBody)
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Unknown)
  }

  test("envelope is stripped before decoding") {
    val backend = stubOk(envelope(Json.obj("acl" -> Json.arr(Json.obj("cidr" -> Json.fromString("1.2.3.4/32"))))))
    val client  = clientFor(backend)
    val r       = Await.result(client.acl.list(), 5.seconds)
    assertEquals(r.acl.head.cidr, "1.2.3.4/32")
  }

  test("empty 204 body is fine for DELETEs") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(204)))
    val client = clientFor(backend)
    Await.result(client.numbers.remove("2015551234"), 5.seconds)
  }

  test("error body that isn't JSON still produces ApiError") {
    val backend = stubStatus(500, "<html>boom</html>")
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Server)
    assert(t.body.exists(_.contains("boom")))
  }

  test("ApiError.toString includes status") {
    val e = ApiError(ErrorKind.NotFound, 404, "missing")
    assert(e.toString.contains("404"))
  }

  test("ErrorKind.fromStatus mapping") {
    assertEquals(ErrorKind.fromStatus(400), ErrorKind.BadRequest)
    assertEquals(ErrorKind.fromStatus(401), ErrorKind.Authentication)
    assertEquals(ErrorKind.fromStatus(403), ErrorKind.PermissionDenied)
    assertEquals(ErrorKind.fromStatus(404), ErrorKind.NotFound)
    assertEquals(ErrorKind.fromStatus(409), ErrorKind.Conflict)
    assertEquals(ErrorKind.fromStatus(429), ErrorKind.RateLimit)
    assertEquals(ErrorKind.fromStatus(500), ErrorKind.Server)
    assertEquals(ErrorKind.fromStatus(502), ErrorKind.Server)
    assertEquals(ErrorKind.fromStatus(599), ErrorKind.Server)
    assertEquals(ErrorKind.fromStatus(418), ErrorKind.Unknown)
  }

  test("ApiError helpers return Unknown for non-ApiError throwables") {
    val t = new RuntimeException("nope")
    assert(!ApiError.isRateLimit(t))
    assert(!ApiError.isNotFound(t))
    assert(!ApiError.isAuthentication(t))
    assert(!ApiError.isConflict(t))
  }

  test("decode failure (malformed body) is mapped to ApiError") {
    val backend = stubOk(envelope(Json.fromString("not-an-object")))
    val client  = clientFor(backend)
    val t       = intercept[ApiError](Await.result(client.account.get(), 5.seconds))
    assertEquals(t.kind, ErrorKind.Unknown)
  }

  test("empty success body for endpoint expecting JSON raises ApiError") {
    val backend = SttpBackendStub
      .asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response("", StatusCode(200)))
    val client = clientFor(backend)
    intercept[ApiError](Await.result(client.account.get(), 5.seconds))
  }
end TransportSuite
