package com.example.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle

@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(content)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is ContentBlock.Code -> {
                    CodeBlock(code = block.code, language = block.lang)
                }
                is ContentBlock.Table -> {
                    MarkdownTable(headers = block.headers, rows = block.rows)
                }
                is ContentBlock.LatexBlock -> {
                    LatexCard(formula = block.formula)
                }
                is ContentBlock.Heading -> {
                    Text(
                        text = block.text,
                        fontSize = when (block.level) {
                            1 -> 20.sp
                            2 -> 17.sp
                            else -> 15.sp
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 26.sp
                    )
                }
                is ContentBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AccentEmerald)
                        )
                        Text(
                            text = block.text,
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 20.sp
                        )
                    }
                }
                is ContentBlock.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = renderInlineStyles(block.text),
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                    }
                }
                is ContentBlock.Paragraph -> {
                    Text(
                        text = renderInlineStyles(block.text),
                        fontSize = 14.5.sp,
                        lineHeight = 23.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LatexCard(formula: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formula,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            color = AccentCyan,
            letterSpacing = 1.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
fun MarkdownTable(headers: List<String>, rows: List<List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .background(SurfaceHighlight)
                .padding(vertical = 8.dp)
        ) {
            for (header in headers) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = header,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Rows
        rows.forEachIndexed { index, rowCells ->
            val rowBg = if (index % 2 == 0) SurfaceGraphite else SurfaceElevated
            Row(
                modifier = Modifier
                    .background(rowBg)
                    .padding(vertical = 7.dp)
            ) {
                rowCells.forEachIndexed { cellIdx, cellText ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cellText,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

sealed class ContentBlock {
    data class Code(val code: String, val lang: String) : ContentBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ContentBlock()
    data class LatexBlock(val formula: String) : ContentBlock()
    data class Heading(val text: String, val level: Int) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
    data class Bullet(val text: String) : ContentBlock()
    data class Paragraph(val text: String) : ContentBlock()
}

fun parseMarkdownBlocks(text: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trim().startsWith("```")) {
            val lang = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(ContentBlock.Code(codeLines.joinToString("\n"), lang))
            i++
            continue
        }

        // Latex block $$...$$
        if (line.trim().startsWith("$$")) {
            val formulaLines = mutableListOf<String>()
            val singleLine = line.trim().removePrefix("$$").removeSuffix("$$")
            if (line.trim().endsWith("$$") && line.trim().length > 4) {
                blocks.add(ContentBlock.LatexBlock(singleLine))
                i++
                continue
            } else {
                i++
                while (i < lines.size && !lines[i].trim().endsWith("$$")) {
                    formulaLines.add(lines[i])
                    i++
                }
                blocks.add(ContentBlock.LatexBlock(formulaLines.joinToString(" ")))
                i++
                continue
            }
        }

        // Table detection (| col | col |)
        if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                tableLines.add(lines[i])
                i++
            }
            if (tableLines.size >= 2) {
                val headers = tableLines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                // filter out separator line |---|---|
                val dataRows = tableLines.drop(1).filter { !it.contains("---") }.map { r ->
                    r.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                }
                blocks.add(ContentBlock.Table(headers, dataRows))
                continue
            }
        }

        // Heading
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val hText = line.drop(level).trim()
            blocks.add(ContentBlock.Heading(hText, level))
            i++
            continue
        }

        // Quote
        if (line.trim().startsWith(">")) {
            blocks.add(ContentBlock.Quote(line.trim().removePrefix(">").trim()))
            i++
            continue
        }

        // Bullet
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
            blocks.add(ContentBlock.Bullet(line.trim().drop(2)))
            i++
            continue
        }

        // Skip blank lines
        if (line.isBlank()) {
            i++
            continue
        }

        // Standard paragraph
        blocks.add(ContentBlock.Paragraph(line))
        i++
    }

    return blocks
}

fun renderInlineStyles(text: String) = buildAnnotatedString {
    var cursor = 0
    val inlineRegex = Regex("(\\*\\*.*?\\*\\*|`.*?`|\\$.*?\\$)")
    val matches = inlineRegex.findAll(text)

    for (match in matches) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") -> {
                val inner = token.removePrefix("**").removeSuffix("**")
                val start = length
                append(inner)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary), start, length)
            }
            token.startsWith("`") && token.endsWith("`") -> {
                val inner = token.removePrefix("`").removeSuffix("`")
                val start = length
                append(" $inner ")
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = AccentEmerald,
                        background = SurfaceHighlight,
                        fontSize = 12.5.sp
                    ),
                    start,
                    length
                )
            }
            token.startsWith("$") && token.endsWith("$") -> {
                val inner = token.removePrefix("$").removeSuffix("$")
                val start = length
                append(inner)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan,
                        fontStyle = FontStyle.Italic
                    ),
                    start,
                    length
                )
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}
