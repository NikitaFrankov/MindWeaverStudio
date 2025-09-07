package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.agents.ARCHITECT_VALIDATOR_OPTIMIZER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_REVIEWER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.agents.DETAILED_ARCHITECT_DESIGNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.agents.HIGH_LEVEL_ARCHITECT_AGENT
import com.example.mindweaverstudio.data.ai.agents.REASONING_AGENT
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.agents.REQUIREMENTS_ANALYST_AGENT

class PipelineFactory {
    val codeFixerPipelineAgents = listOf(CODE_FIXER_AGENT, CODE_TESTER_AGENT)
    val chatPipelineAgents = listOf(CHAT_AGENT, REASONING_AGENT)
    val codeCreatorPipelineAgents = listOf(CODE_CREATOR_AGENT)
    val codeReviewPipelineAgents = listOf(CODE_REVIEWER_AGENT)
    val githubReleasePipelineAgents = listOf(RELEASE_NOTES_GENERATION_AGENT, GITHUB_RELEASE_AGENT)
    val architecturePipelineAgents = listOf(REQUIREMENTS_ANALYST_AGENT, HIGH_LEVEL_ARCHITECT_AGENT, DETAILED_ARCHITECT_DESIGNER_AGENT, ARCHITECT_VALIDATOR_OPTIMIZER_AGENT)
}