package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxPlate
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val displayLang = if (language.isNotBlank()) language.uppercase() else "CODE"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SyntaxPlate)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceHighlight.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentEmerald)
                )
                Text(
                    text = displayLang,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isCopied = true
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("copy_code_button")
            ) {
                if (isCopied) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copied",
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code Content with syntax highlighting
        val highlighted = remember(code, language) { highlightSyntax(code) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
        }
    }
}

private fun highlightSyntax(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)

        val keywords = setOf(
            "val", "var", "fun", "class", "interface", "object", "return", "if", "else",
            "when", "import", "package", "override", "private", "public", "suspend",
            "sealed", "data", "const", "let", "def", "async", "await", "function", "try",
            "catch", "finally", "throw", "null", "true", "false", "this", "new", "type",
            "from", "export", "struct", "impl", "fn", "mut", "pub", "select", "from", "where"
        )

        val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
        keywordRegex.findAll(code).forEach { match ->
            addStyle(
                SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // Strings ("..." or '...')
        val stringRegex = Regex("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        // Numbers
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?([fFL])?\\b")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }

        // Function calls identifier(...)
        val funcRegex = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\()")
        funcRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxFunction), match.range.first, match.range.last + 1)
        }

        // Comments (// ... or /* ... */)
        val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
        }
    }
}
