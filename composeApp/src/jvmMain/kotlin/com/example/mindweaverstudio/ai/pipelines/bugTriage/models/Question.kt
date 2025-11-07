package com.example.mindweaverstudio.ai.pipelines.bugTriage.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val question: String,
    val answer: String,
)

@Serializable
@SerialName("QuestionValidation")
@LLMDescription("Validation result for a user's answer to a question")
data class QuestionValidationResult(
    @property:LLMDescription("True if the answer is sufficient, false if it lacks detail or relevance")
    val isValid: Boolean,

    @property:LLMDescription("Short explanation describing why the answer is or isn’t sufficient")
    val reason: String
)