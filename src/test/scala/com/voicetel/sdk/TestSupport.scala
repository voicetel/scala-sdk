package com.voicetel.sdk

import io.circe.Json
import io.circe.syntax.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Identity, Request, Response}
import sttp.model.{Header, StatusCode}

import scala.concurrent.Future

/** Helpers for unit tests. */
object TestSupport:

  /** Wrap a JSON payload in the standard `{status, data}` envelope. */
  def envelope(data: Json): String =
    Json.obj("status" -> Json.fromString("success"), "data" -> data).noSpaces

  /** Build a sttp backend stub that replies to every request with `body`. */
  def stubOk(body: String): SttpBackendStub[Future, Any] =
    SttpBackendStub.asynchronousFuture
      .whenAnyRequest
      .thenRespond(body)

  /** Build a sttp backend stub that replies to every request with the supplied status + body. */
  def stubStatus(status: Int, body: String, headers: List[Header] = Nil): SttpBackendStub[Future, Any] =
    SttpBackendStub.asynchronousFuture
      .whenAnyRequest
      .thenRespond(Response(body, StatusCode(status), "", headers))

  /** A recording stub: records every request and replies with a single body. */
  def recordingStub(
      bodies: scala.collection.mutable.ListBuffer[Request[?, ?]],
      response: String,
      status: Int = 200
  ): SttpBackendStub[Future, Any] =
    SttpBackendStub.asynchronousFuture
      .whenRequestMatchesPartial { case req =>
        bodies += req
        Response(response, StatusCode(status))
      }

  /** Build a VoiceTelClient over `backend` that uses base http://api.test and key "k". */
  def clientFor(backend: SttpBackendStub[Future, Any], retries: Int = 0): VoiceTelClient =
    VoiceTelClient.withBackend(
      backend,
      baseURL = "http://api.test",
      apiKey = "k",
      maxRetries = retries
    )

  /** Build a VoiceTelClient with no api key — useful for testing the no-auth path. */
  def unauthClient(backend: SttpBackendStub[Future, Any], retries: Int = 0): VoiceTelClient =
    VoiceTelClient.withBackend(
      backend,
      baseURL = "http://api.test",
      apiKey = "",
      maxRetries = retries
    )

  /** Encode a value to a JSON envelope string. */
  def envelopeOf[A: io.circe.Encoder](a: A): String =
    envelope(a.asJson)

  /** Common error body. */
  val errorBody: String = """{"code":"X","message":"server is angry"}"""

  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
