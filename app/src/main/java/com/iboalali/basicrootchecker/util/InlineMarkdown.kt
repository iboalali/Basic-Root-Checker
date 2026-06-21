package com.iboalali.basicrootchecker.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders the limited inline Markdown the app catalog's "What's new" bullets use — `**bold**` and
 * `*italic*` — into an [AnnotatedString] for a Compose `Text`. Non-nested; an unbalanced or unknown
 * marker is left as literal text. This deliberately avoids a Markdown dependency: the feed emits no
 * links, headings, lists, or code spans (see the `apps.json` feed contract).
 */
fun parseInlineMarkdown(input: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < input.length) {
        when {
            input.startsWith("**", i) -> {
                val end = input.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(input.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    append("**"); i += 2
                }
            }

            input[i] == '*' -> {
                val end = input.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(input.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append('*'); i++
                }
            }

            else -> {
                append(input[i]); i++
            }
        }
    }
}
