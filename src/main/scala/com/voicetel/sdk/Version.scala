package com.voicetel.sdk

/** SDK and API version constants. */
object Version:
  /** This client library's semantic version. */
  val SDKVersion: String = "2.2.10"

  /** The VoiceTel REST API version this SDK targets. */
  val APIVersion: String = "v2.2.10"

  /** Production VoiceTel API endpoint. */
  val DefaultBaseURL: String = "https://api.voicetel.com"

  /** Default User-Agent sent on every request. */
  val DefaultUserAgent: String =
    s"voicetel-scala/$SDKVersion (+https://github.com/voicetel/scala-sdk)"
