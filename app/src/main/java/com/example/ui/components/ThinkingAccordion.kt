package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle

@Composable
fun ThinkingAccordion(
    thinkingProcess: String,
    durationText: String = "~ 18s",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
    ) {
        // Accordion Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expanded = !expanded
                }
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("thinking_accordion_toggle"),
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
                        .background(AccentCyan)
                )
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "[ $durationText Thinking Process ]",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }

        // Expanded Reasoning Monospace Trace
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGraphite)
                    .border(width = 0.5.dp, color = SurfaceBorder)
                    .padding(12.dp)
            ) {
                Text(
                    text = thinkingProcess,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
