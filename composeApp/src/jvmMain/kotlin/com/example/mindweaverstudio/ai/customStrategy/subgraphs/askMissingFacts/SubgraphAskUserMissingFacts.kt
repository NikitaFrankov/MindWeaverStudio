package com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts

import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemory
import ai.koog.agents.memory.feature.withMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.StructureFixingParser
import com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts.models.FactsRequest
import com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts.models.MissingFactsResponse
import com.example.mindweaverstudio.ai.pipelines.githubRelease.nodeMissingFactsSystemPrompt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates a subgraph that identifies missing facts in memory and requests them from the user.
 *
 * Flow:
 * 1. Cleanup - Clear storage from previous executions
 * 2. Compare required concepts with facts in memory using LLM
 * 3. If missing facts found - request them from user
 * 4. Generate result message with collected facts
 * 5. Save collected facts to memory
 *
 * @param name The name of the subgraph
 * @param llmModel The language model to use for analyzing missing facts
 * @param llmParams Parameters for LLM execution (temperature, max tokens, etc.)
 * @param userConnectionTools List of tools for communicating with the user
 * @param requiredConcepts List of concepts that must be present in memory
 * @param memorySubject The subject associated with the memory entries
 * @param memoryScope The scope of memory to search
 * @return A subgraph delegate that processes missing facts and returns a status message
 */

@OptIn(InternalAgentsApi::class)
inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.subgraphAskUserMissingFacts(
    name: String,
    llmModel: LLModel,
    llmParams: LLMParams,
    userConnectionTools: List<Tool<*, *>>,
    requiredConcepts: List<Concept>,
    memorySubject: MemorySubject,
    memoryScope: MemoryScope,
): AIAgentSubgraphDelegate<Input, String> {
    require(requiredConcepts.isNotEmpty()) { "requiredConcepts cannot be empty" }

    return subgraph(
        name = name,
        llmModel = llmModel,
        llmParams = llmParams,
        tools = userConnectionTools,
    ) {
        // Node 1: Cleanup storage from previous subgraph executions
        // This prevents data leakage between different invocations
        val nodeCleanup by node<Input, Input> { input ->
            storage.remove(requestingFactsKey)
            storage.remove(hasMissingFactsKey)
            input
        }

        // Node 2: Analyze which facts are missing from memory
        // Uses LLM to compare required concepts against loaded facts
        val nodeMissingFacts by node<Input, Boolean> {
            withMemory {
                // Load all facts from memory for the given subject and scope
                val loadedFacts = agentMemory.loadAll(
                    subject = memorySubject,
                    scope = memoryScope
                )
                val concepts = requiredConcepts.map { it.description }

                llm.writeSession {
                    updatePrompt {
                        // Instruct LLM to identify which concepts are not present in loaded facts
                        system(nodeMissingFactsSystemPrompt)

                        user("""
                              Required concepts: 
                              ${concepts.joinToString("\n- ", prefix = "- ")}
                              
                              Loaded facts from memory:
                              ${loadedFacts.joinToString("\n")}
                          """.trimIndent())
                    }

                    // Request structured output from LLM (list of missing facts)
                    requestLLMStructured<MissingFactsResponse>(
                        examples = listOf(
                            MissingFactsResponse(missingFacts = listOf()),
                            MissingFactsResponse(missingFacts = listOf("favorite language", "favorite IDE"))
                        ),
                        fixingParser = StructureFixingParser(
                            fixingModel = model,
                            retries = 3
                        )
                    ).fold(
                        onSuccess = {
                            val requiredFacts = it.structure.missingFacts

                            // Case 1: No missing facts - all required concepts are in memory
                            if (requiredFacts.isEmpty()) {
                                storage.set(hasMissingFactsKey, false)
                                return@fold false
                            }

                            // Case 2: Some facts are missing - store them and proceed to user request
                            storage.set(requestingFactsKey, FactsRequest(value = requiredFacts))
                            storage.set(hasMissingFactsKey, true)
                            return@fold true
                        },
                        onFailure = { error ->
                            // Case 3: LLM failed to analyze - fallback to requesting all concepts
                            // Better to ask for everything than to miss critical information
                            println("Failed to analyze missing facts: ${error.message}")
                            val allConceptDescriptions = requiredConcepts.map { it.description }
                            storage.set(requestingFactsKey, FactsRequest(value = allConceptDescriptions))
                            storage.set(hasMissingFactsKey, true)
                            return@fold true
                        },
                    )
                }
            }
        }

        // Node 3: Request missing facts from the user
        // This subgraph uses the userConnectionTool to interact with the user
        // Lower temperature (0.3) ensures more deterministic and focused responses
        val subgraphAskUser by subgraphWithTask<Any, String>(
            tools = userConnectionTools,
            llmParams = LLMParams().copy(temperature = 0.3)
        ) {
            val facts = storage.get(requestingFactsKey)?.value
            "Request from user next facts: $facts"
        }

        // Node 4: Generate result message based on whether facts were collected
        // Creates structured message for memory saving node
        val nodeResult by node<Any, String> { input ->
            val hasMissingFacts = storage.get(hasMissingFactsKey) ?: false

            if (!hasMissingFacts) {
                "Nothing to save"
            } else {
                val facts = storage.getValue(requestingFactsKey).value
                """
                    Need to save in memory:
                        Facts: $facts
                        Concepts to facts: $requiredConcepts
                        memorySubject: $memorySubject
                        memoryScope: $memoryScope
                """.trimIndent()
            }
        }

        // Node 5: Save collected facts to memory
        // Uses structured message from nodeResult to persist facts
        val nodeSaveMissingFacts by nodeSaveToMemory<String>(
            name = "nodeSaveMissingFacts",
            subject = memorySubject,
            scope = MemoryScopeType.AGENT,
            concepts = requiredConcepts,
            retrievalModel = llmModel
        )

        // Graph edges define the execution flow:
        // nodeStart -> nodeCleanup -> nodeMissingFacts
        //                                  |
        //                      [true] -----+------ [false]
        //                         |                   |
        //                   subgraphAskUser           |
        //                         |                   |
        //                         +----> nodeResult <-+
        //                                   |
        //                          nodeSaveMissingFacts
        //                                   |
        //                              nodeFinish

        edge(nodeStart forwardTo nodeCleanup)
        edge(nodeCleanup forwardTo nodeMissingFacts)
        edge(nodeMissingFacts forwardTo subgraphAskUser onCondition { isSuccessful -> isSuccessful })
        edge(nodeMissingFacts forwardTo nodeResult onCondition { isSuccessful -> !isSuccessful })
        edge(subgraphAskUser forwardTo nodeResult)
        edge(nodeResult forwardTo nodeSaveMissingFacts)
        edge(nodeSaveMissingFacts forwardTo nodeFinish)
    }
}