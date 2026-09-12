package com.cochelper.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text

/** 极简 Markdown 渲染：**粗体**、*斜体*、__下划线__、~~删除线~~。 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    SelectionContainer {
        Text(
            text = markdownToAnnotatedString(text),
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

fun markdownToAnnotatedString(text: String): AnnotatedString = buildAnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    val italic = SpanStyle(fontStyle = FontStyle.Italic)
    val underline = SpanStyle(textDecoration = TextDecoration.Underline)
    val strike = SpanStyle(textDecoration = TextDecoration.LineThrough)

    var i = 0
    val n = text.length
    while (i < n) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) { withStyle(bold) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text.startsWith("__", i) -> {
                val end = text.indexOf("__", i + 2)
                if (end > i) { withStyle(underline) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i) { withStyle(strike) { append(text.substring(i + 2, end)) }; i = end + 2 }
                else { append(text[i]); i++ }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end > i) { withStyle(italic) { append(text.substring(i + 1, end)) }; i = end + 1 }
                else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

/** 用 marker 包裹当前选中文本（无选中则插入 marker 对）。 */
fun applyMarkup(value: TextFieldValue, marker: String): TextFieldValue {
    val sel = value.selection
    val text = value.text
    val start = sel.min
    val end = sel.max
    val selected = text.substring(start, end)
    val newText = buildString {
        append(text.substring(0, start))
        append(marker)
        append(selected)
        append(marker)
        append(text.substring(end))
    }
    val newSelStart = start + marker.length
    val newSelEnd = if (start == end) newSelStart else newSelStart + selected.length
    return value.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newSelStart, newSelEnd),
    )
}
