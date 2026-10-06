package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVAILABLE_MODELS
import com.example.data.model.ModelInfo
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.OledBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorSheet(
    sheetState: SheetState,
    currentModelId: String,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceGraphite,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEURAL CORE ARCHITECTURE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentEmerald,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Select Engine Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Models list
            for (model in AVAILABLE_MODELS) {
                val isSelected = model.id == currentModelId
                ModelItemCard(
                    model = model,
                    isSelected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectModel(model.id)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModelItemCard(
    model: ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceElevated else OledBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AccentEmerald else SurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("model_option_${model.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selection Indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) AccentEmerald else SurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OledBackground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = model.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // Context Window Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceHighlight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = model.contextWindow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan
                    )
                }
            }

            Text(
                text = model.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Model ID tag
        Text(
            text = model.badge,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (isSelected) AccentEmerald else TextSubtle
        )
    }
}
