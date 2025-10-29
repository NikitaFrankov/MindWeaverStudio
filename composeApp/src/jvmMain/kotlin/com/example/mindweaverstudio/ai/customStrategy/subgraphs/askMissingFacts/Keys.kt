package com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts

import ai.koog.agents.core.agent.entity.createStorageKey
import com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts.models.FactsRequest

val requestingFactsKey = createStorageKey<FactsRequest>("required facts to request from user")
val hasMissingFactsKey = createStorageKey<Boolean>("has missing facts flag")