package com.aurum.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurum.scanner.ScanProgress

@Composable
fun ScanningProgressBanner(
    scanProgress: ScanProgress,
    modifier: Modifier = Modifier
) {
    if (!scanProgress.isScanning) return

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bannerBg = if (isDark) Color(0xFF1E222A) else MaterialTheme.colorScheme.primaryContainer
    val bannerContent = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bannerBg,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = bannerContent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scanning Music Library...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = bannerContent
                    )
                }
                Text(
                    text = "${scanProgress.scannedCount} / ${scanProgress.totalCount} (${(scanProgress.fraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = bannerContent.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { scanProgress.fraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = bannerContent,
                trackColor = bannerContent.copy(alpha = 0.2f),
            )
            if (scanProgress.currentItemName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scanProgress.currentItemName,
                    style = MaterialTheme.typography.labelSmall,
                    color = bannerContent.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
