package com.example.mindweaverstudio.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mindweaverstudio.ui.chat.utils.ChatStrings
import com.example.mindweaverstudio.ui.model.AssistantMessagePresentation

@Composable
fun AnswerSection(answerText: String) {
    Text(
        text = "${ChatStrings.ANSWER_LABEL} $answerText",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun StepsSection(steps: List<AssistantMessagePresentation.StepPoint>) {
    if (steps.isEmpty()) return
    
    Text(
        text = ChatStrings.STEPS_LABEL,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    steps.forEach { step ->
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "•",
                color = step.color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(24.dp)
            )
            Text(text = step.text)
        }
    }
}

@Composable
fun FactsSection(facts: List<AssistantMessagePresentation.FactPoint>) {
    if (facts.isEmpty()) return
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = ChatStrings.IMPORTANT_POINTS_LABEL,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    facts.forEach { fact ->
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "•",
                color = fact.color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(24.dp)
            )
            Text(text = fact.text)
        }
    }
}

@Composable
fun SummarySection(summaryText: String) {
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = "${ChatStrings.SUMMARY_LABEL} $summaryText",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun MetaSection(
    confidence: String?,
    source: String?
) {
    if (confidence == null && source == null) return
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        confidence?.let { conf ->
            Text(
                text = "${ChatStrings.CONFIDENCE_LABEL} $conf",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        source?.let { src ->
            Text(
                text = "${ChatStrings.SOURCE_LABEL} $src",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}