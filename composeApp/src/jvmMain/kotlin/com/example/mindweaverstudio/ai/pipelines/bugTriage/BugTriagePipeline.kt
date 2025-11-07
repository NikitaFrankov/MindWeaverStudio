package com.example.mindweaverstudio.ai.pipelines.bugTriage

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.StructureFixingParser
import com.example.mindweaverstudio.ai.pipelines.bugTriage.models.BugDraft
import com.example.mindweaverstudio.ai.pipelines.bugTriage.models.Question
import com.example.mindweaverstudio.ai.pipelines.bugTriage.models.QuestionValidationResult
import com.example.mindweaverstudio.ai.pipelines.bugTriage.models.isComplete
import com.example.mindweaverstudio.ai.tools.user.UserInteractionTools
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Bug Triage Pipeline - An AI-powered system for collecting complete bug reports from users.
 *
 * This pipeline implements an iterative workflow that:
 * 1. Generates intelligent questions based on the current bug draft
 * 2. Asks users for missing information
 * 3. Validates their answers
 * 4. Merges validated answers into the bug report draft
 * 5. Repeats until the bug report is complete
 * 6. Creates a final formatted bug report
 *
 * The workflow uses a graph-based state machine approach where each node represents
 * a processing step, and edges define the flow between steps based on conditions.
 */
class BugTriagePipeline(
    private val config: ApiConfiguration,
    private val userInteractionTools: UserInteractionTools,
) {
    // Core configuration
    private val model = OpenAIModels.CostOptimized.GPT4oMini // LLM model used for all operations
    private val name = "BUG_TRIAGE_PIPELINE" // Pipeline identifier
    private val json: Json = Json {  } // JSON serializer for data structures

    // Memory keys - These keys are used to store and retrieve data from the agent's memory storage
    // throughout the workflow execution
    private val currentQuestionKey = createStorageKey<Question>("Current question to ask")
    private val bugDraftKey = createStorageKey<BugDraft>("Draft for describe bug")
    private val questionValidationResultKey = createStorageKey<QuestionValidationResult>("Question validation result")

    // Strategy - Defines the workflow graph with nodes and edges
    // Input: Initial user message (String)
    // Output: Final formatted bug report (String)
    @OptIn(ExperimentalUuidApi::class)
    private val bugTriageStrategy = strategy<String, String>(name) {
        /**
         * Node: Prepare Storage
         * Purpose: Initializes the workflow by clearing any previous state and creating a new bug draft
         * Input: Initial user message
         * Output: Same input (pass-through)
         */
        val nodePrepareStorage by node<String, String> { input ->
            // Clear any previous workflow state from storage
            storage.remove(questionValidationResultKey)
            storage.remove(currentQuestionKey)
            storage.remove(bugDraftKey)

            // Initialize a new empty bug draft with a unique ID
            val bugDraft = BugDraft(draftId = Uuid.random().toString())
            storage.set(bugDraftKey, bugDraft)

            return@node input
        }

        /**
         * Node: Prepare Question
         * Purpose: Uses LLM to analyze the current bug draft and generate the next question
         *          to ask the user to fill in missing information
         * Input: Any (not used, can come from different nodes)
         * Output: Generated question text (String)
         */
        val nodePrepareQuestion by node<Any, String> {
            // Get the current state of the bug draft
            val currentDraft = storage.getValue(bugDraftKey)
            val draftString = json.encodeToString(BugDraft.serializer(), currentDraft)

            llm.writeSession {
                updatePrompt {
                    // System prompt instructs LLM on how to generate intelligent questions
                    system(nodePrepareQuestionSystemPrompt)
                    // Provide the current draft as context
                    user(draftString)
                }

                // Request LLM to generate a question based on what's missing in the draft
                val question = Question(
                    question = requestLLMWithoutTools().content,
                    answer = "" // Answer will be filled when user responds
                )
                storage.set(currentQuestionKey, question)

                return@writeSession question.question
            }
        }

        /**
         * Subgraph: Ask User
         * Purpose: Executes a separate agent workflow to interact with the user and get their answer
         * Output: User's answer (String)
         * Note: This subgraph has access to userInteractionTools to send messages and receive responses
         */
        val subgraphAskUser by subgraphWithTask<Any, String>(
            tools = userInteractionTools.asTools(),
            llmParams = LLMParams().copy(temperature = 0.3)
        ) {
            val fact = storage.getValue(currentQuestionKey)
            "Request from user next question: ${fact.question}"
        }

        /**
         * Node: Answer Validation
         * Purpose: Validates whether the user's answer is appropriate and complete for the question asked
         * Input: User's answer (String)
         * Output: Validation result (Boolean) - true if valid, false if needs to be re-asked
         */
        val nodeAnswerValidation by node<String, Boolean> { input ->
            llm.writeSession {
                // Update the current question with the user's answer
                val updatedQuestion = storage.getValue(currentQuestionKey).copy(answer = input)
                val requestToValidate = json.encodeToString(Question.serializer(), updatedQuestion)

                updatePrompt {
                    // System prompt defines validation criteria
                    system(nodeAnswerValidationSystemPrompt)
                    user(requestToValidate)
                }

                // Request structured validation result from LLM
                requestLLMStructured<QuestionValidationResult>(
                    examples = listOf(
                        QuestionValidationResult(
                            isValid = false,
                            reason = "The reason for deciding that this is not valid"
                        ),
                        QuestionValidationResult(
                            isValid = true,
                            reason = "The reason for deciding that this is valid"
                        ),
                    ),
                    // If LLM returns invalid JSON, automatically retry with fixing parser
                    fixingParser = StructureFixingParser(
                        fixingModel = model,
                        retries = 3
                    )
                ).fold(
                    onSuccess = { result ->
                        val validationResult = result.structure
                        // Save both the updated question and validation result
                        storage.set(currentQuestionKey, updatedQuestion)
                        storage.set(questionValidationResultKey, validationResult)

                        validationResult.isValid
                    },
                    onFailure = {
                        // If parsing fails after retries, consider answer invalid
                        return@writeSession false
                    }
                )
            }
        }

        /**
         * Node: Prepare Repeat Question
         * Purpose: When an answer is invalid, reformulates the question with context about why
         *          the previous answer was rejected, to help the user provide better information
         * Output: Reformulated question text (String)
         */
        val nodePrepareRepeatQuestion by node<Any, String> { input ->
            val currentQuestion = storage.getValue(currentQuestionKey)
            val validationResult = storage.getValue(questionValidationResultKey)

            val questionString = json.encodeToString(Question.serializer(), currentQuestion)
            val validationResultString = "{ reason: \"${validationResult.reason}\" }"

            llm.writeSession {
                updatePrompt {
                    // System prompt instructs LLM to rephrase question with validation context
                    system(nodePrepareRepeatQuestionSystemPrompt)
                    // Provide both the Q&A and the reason it was rejected
                    user(questionString + validationResultString)
                }

                // Generate a new, more specific question
                val question = Question(
                    question = requestLLMWithoutTools().content,
                    answer = ""
                )
                storage.set(currentQuestionKey, question)

                return@writeSession question.question
            }
        }

        /**
         * Node: Merge Answer With Draft
         * Purpose: Integrates the validated Q&A into the bug draft, updating relevant fields
         *          based on the information provided by the user
         * Output: Updated bug draft as JSON string
         */
        val nodeMergeAnswerWithDraft by node<Any, String> {
            val currentQuestion = storage.getValue(currentQuestionKey)
            val bugDraft = storage.getValue(bugDraftKey)
            val currentQuestionString = json.encodeToString(Question.serializer(), currentQuestion)
            val bugDriftString = json.encodeToString(BugDraft.serializer(), bugDraft)

            llm.writeSession {
                updatePrompt {
                    system(nodeMergeAnswerWithDraftSystemPrompt)
                    user(currentQuestionString + bugDriftString)
                }

                // Request LLM to return an updated BugDraft structure
                requestLLMStructured<BugDraft>(
                    examples = listOf(
                        // Example of a complete bug draft to guide the LLM
                        BugDraft(
                            draftId = "f8b4c9b1-3a2e-4e87-9a6b-8e52ad4c10a7",
                            title = "App crashes when opening settings",
                            summary = "The application crashes every time the user opens the Settings screen after login.",
                            steps = listOf(
                                "Launch the app",
                                "Log in with valid credentials",
                                "Tap on the Settings icon in the top-right corner"
                            ),
                            expected = "Settings screen should open and display available preferences.",
                            actual = "App crashes immediately with a 'NullPointerException' error.",
                            reproducibility = "always",
                            impact = "Affects around 40% of Android users according to crash analytics.",
                            platform = "android",
                            appVersion = "2.3.1",
                            osVersion = "Android 14",
                            deviceModel = "Pixel 7"
                        )
                    )
                ).fold(
                    onSuccess = {
                        val draftResult = it.structure
                        // Save the updated draft to storage
                        storage.set(bugDraftKey, draftResult)

                        json.encodeToString(BugDraft.serializer(), draftResult)
                    },
                    onFailure = {
                        error(it)
                        it.message.orEmpty()
                    }
                )
            }
        }

        /**
         * Node: Check Draft Completeness
         * Purpose: Determines if the bug draft has all required fields filled
         * Output: Boolean - true if draft is complete, false if more questions needed
         */
        val nodeCheckDraftCompleteness by node<String, Boolean> {
            val draft = storage.getValue(bugDraftKey)
            println("draft - $draft")
            println("isComplete = ${draft.isComplete()}")

            // Check if all required fields in the draft are populated
            draft.isComplete()
        }

        /**
         * Node: Report Creation
         * Purpose: Creates the final formatted bug report from the complete draft
         * Input: Boolean (completeness flag, not used)
         * Output: Final formatted bug report (String)
         */
        val nodeReportCreation by node<Boolean, String> {
            val draft = storage.getValue(bugDraftKey)
            val draftString = json.encodeToString(BugDraft.serializer(), draft)

            llm.writeSession {
                updatePrompt {
                    // System prompt instructs LLM on formatting the final report
                    system(nodeResultSystemPrompt)
                    user(draftString)
                }

                // Generate human-readable bug report
                requestLLMWithoutTools().content
            }
        }

        // ===== WORKFLOW GRAPH DEFINITION =====
        // Edges define the flow between nodes, creating a state machine for bug triage

        // Initial flow: Start -> Initialize storage -> Generate first question
        edge(nodeStart forwardTo nodePrepareStorage)
        edge(nodePrepareStorage forwardTo nodePrepareQuestion)

        // Question asking: Both initial and repeat questions lead to user interaction
        edge(nodePrepareQuestion forwardTo subgraphAskUser)
        edge(nodePrepareRepeatQuestion forwardTo subgraphAskUser)

        // Answer validation flow: User response is validated
        edge(subgraphAskUser forwardTo nodeAnswerValidation)
        // If answer is INVALID: reformulate question and ask again
        edge(nodeAnswerValidation forwardTo nodePrepareRepeatQuestion onCondition { isValidAnswer -> !isValidAnswer })
        // If answer is VALID: merge into draft
        edge(nodeAnswerValidation forwardTo nodeMergeAnswerWithDraft onCondition { isValidAnswer -> isValidAnswer })

        // Completeness check: After merging, check if we need more info
        edge(nodeMergeAnswerWithDraft forwardTo nodeCheckDraftCompleteness)
        // If draft is INCOMPLETE: generate next question and continue loop
        edge(nodeCheckDraftCompleteness forwardTo nodePrepareQuestion onCondition { isDraftComplete -> !isDraftComplete })
        // If draft is COMPLETE: create final report
        edge(nodeCheckDraftCompleteness forwardTo nodeReportCreation onCondition { isDraftComplete -> isDraftComplete })

        // Final step: Report creation leads to workflow completion
        edge(nodeReportCreation forwardTo nodeFinish)
    }

    /**
     * AI Agent instance configured with:
     * - OpenAI executor for LLM calls
     * - Bug triage strategy (workflow graph defined above)
     * - User interaction tools for communication
     * - Low temperature (0.1) for consistent, deterministic responses
     * - High iteration limit (1000) to handle complex bug reports
     */
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(config.openAiApiKey),
        strategy = bugTriageStrategy,
        toolRegistry = ToolRegistry {
            tools(userInteractionTools)
        },
        llmModel = model,
        temperature = 0.1, // Low temperature for consistent behavior
        maxIterations = 1000, // Allows for many Q&A rounds if needed
    ) {

    }

    /**
     * Executes the bug triage pipeline
     * @param input Initial user message or bug description
     * @return Formatted bug report with all collected information
     */
    suspend fun run(
        input: String,
    ): String {
        return agent.run(input)
    }
}