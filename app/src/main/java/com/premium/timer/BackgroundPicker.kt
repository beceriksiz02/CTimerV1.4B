@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.premium.timer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premium.timer.timing.DisplayPrecision
import com.premium.timer.timing.TimerFontChoice
import com.premium.timer.timing.VisualStyle

private val swatches = listOf(
    Color.Black,          // pitch black - always first, always available
    Color(0xFF0B0B0F),
    Color(0xFF121212),
    AccentPurple,
    AccentTeal,
    AccentAmber,
    AccentRose,
    Color(0xFF2D3436),
    Color(0xFF130F40),
    Color(0xFF1E272E)
)

private val styleLabels = mapOf(
    VisualStyle.DIGITAL to "Digital",
    VisualStyle.ANALOG to "Analog",
    VisualStyle.MINIMAL to "Minimal",
    VisualStyle.THICK_DISK to "Disk",
    VisualStyle.SEGMENTED_RING to "Segments"
)

@Composable
fun BackgroundPickerSheet(
    settings: BackgroundSettings,
    onDismiss: () -> Unit
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            settings.imageUri = uri
            settings.type = BackgroundType.IMAGE
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceCard) {
        Column(Modifier.padding(20.dp).padding(bottom = 24.dp)) {
            Text("Customize", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("Visual style", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(VisualStyle.values().toList()) { style ->
                    val selected = settings.visualStyle == style
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) settings.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = settings.accent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { settings.visualStyle = style }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(styleLabels[style] ?: style.name, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Display precision", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(DisplayPrecision.values().toList()) { p ->
                    val selected = settings.precision == p
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) settings.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = settings.accent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { settings.precision = p }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(p.label, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Font", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TimerFontChoice.values().toList()) { font ->
                    val selected = settings.fontChoice == font
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) settings.accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = settings.accent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { settings.fontChoice = font }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(font.label, color = Color.White, fontSize = 13.sp, fontFamily = font.family)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Solid color", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(swatches) { c ->
                    val selected = settings.type == BackgroundType.COLOR && settings.color == c
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) settings.accent else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable {
                                settings.color = c
                                settings.type = BackgroundType.COLOR
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Custom photo", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                   imagePicker.launch(
    PickVisualMediaRequest(
        ActivityResultContracts.PickVisualMedia.ImageOnly
    )
)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (settings.type == BackgroundType.IMAGE) "Change photo" else "Choose from gallery")
            }
            if (settings.type == BackgroundType.IMAGE) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    settings.type = BackgroundType.COLOR
                    settings.imageUri = null
                }) {
                    Text("Remove photo, use solid color instead")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Accent color", color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(PremiumPalette) { c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c)
                            .border(
                                width = if (settings.accent == c) 3.dp else 0.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { settings.accent = c }
                    )
                }
            }
        }
    }
}
