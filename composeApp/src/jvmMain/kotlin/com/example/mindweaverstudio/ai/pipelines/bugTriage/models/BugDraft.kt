package com.example.mindweaverstudio.ai.pipelines.bugTriage.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("BugDraft")
@LLMDescription("Simplified structured bug draft collected by the triage agent.")
data class BugDraft(
  @property:LLMDescription("Unique draft ID (UUID).")
  val draftId: String,

  @property:LLMDescription("Short title suitable for an issue tracker.")
  var title: String? = null,

  @property:LLMDescription("One-line summary describing the essence of the bug.")
  var summary: String? = null,

  @property:LLMDescription("Ordered steps to reproduce the issue.")
  var steps: List<String> = emptyList(),

  @property:LLMDescription("Expected behavior or correct result.")
  var expected: String? = null,

  @property:LLMDescription("Actual behavior observed (error, crash, wrong output, etc.).")
  var actual: String? = null,

  @property:LLMDescription("How often the issue reproduces: 'always', 'sometimes', 'rarely', or 'unknown'.")
  var reproducibility: String? = null,

  @property:LLMDescription("Estimated impact: approximate number or percentage of affected users.")
  var impact: String? = null,

  // Flattened environment fields
  @property:LLMDescription("Platform: 'android', 'ios', 'web', 'backend', etc.")
  var platform: String? = null,

  @property:LLMDescription("Application or service version (e.g., '1.4.2' or commit hash).")
  var appVersion: String? = null,

  @property:LLMDescription("Operating system/version (e.g., 'Android 14', 'iOS 17').")
  var osVersion: String? = null,

  @property:LLMDescription("Device model or environment identifier (e.g., 'Pixel 6', 'iPhone 13').")
  var deviceModel: String? = null
)

fun BugDraft.isComplete(): Boolean =
    !title.isNullOrBlank()
        && !summary.isNullOrBlank()
        && steps.isNotEmpty()
        && !expected.isNullOrBlank()
        && !actual.isNullOrBlank()
        && !reproducibility.isNullOrBlank()
        && !impact.isNullOrBlank()
        && !platform.isNullOrBlank()
        && !appVersion.isNullOrBlank()
        && !osVersion.isNullOrBlank()
        && !deviceModel.isNullOrBlank()
