package com.stockpilot.ai.ui.assistant

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.stockpilot.ai.domain.assistant.ChatProductLink
import com.stockpilot.ai.ui.theme.StockPilotTeal

@Composable
fun LinkedChatText(
    text: String,
    links: List<ChatProductLink>,
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) {
        Text(text = text, modifier = modifier)
        return
    }
    val sorted = remember(links) {
        links.filter { it.startIndex in 0..text.length && it.endIndex in 0..text.length }
            .sortedBy { it.startIndex }
    }
    val annotated = remember(text, sorted) {
        buildAnnotatedString {
            var cursor = 0
            for (link in sorted) {
                if (link.startIndex < cursor) continue
                if (link.startIndex > cursor) {
                    append(text.substring(cursor, link.startIndex))
                }
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "product_${link.productId}_${link.startIndex}",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = StockPilotTeal,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                        linkInteractionListener = { onProductClick(link.productId) },
                    ),
                ) {
                    append(text.substring(link.startIndex, link.endIndex))
                }
                cursor = link.endIndex
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}