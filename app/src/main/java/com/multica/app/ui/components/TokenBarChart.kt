package com.multica.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multica.app.data.model.DailyUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * v0.3.35 老板 2026-06-09 优化：
 *  1. 颜色渐变：用量越大柱顶越深蓝（顶端 dark #2563EB → 底端 light #93C5FD）
 *  2. 3 个日期 label：左 = 第 1 天，中 = 第 15 天，右 = 第 30 天
 *  3. 保持不卡片化（紧贴顶部），占位高度 64+12 = 76dp
 *
 * 视觉：
 *  [  ▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌ ]  1.23M  30d 总
 *   06-10              06-24            07-09  45.2K 当天
 */
@Composable
fun TokenBarChart(
    data: List<DailyUsage>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF2563EB),       // 顶端 deep blue-600
    barColorLight: Color = Color(0xFF93C5FD),  // 底端 light blue-300
    labelColor: Color = Color(0xFF9CA3AF),     // 灰
    axisColor: Color = Color(0xFF374151),      // 暗灰
    height: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val max = (data.maxOfOrNull { it.total } ?: 0L).coerceAtLeast(1L)
    val total = data.sumOf { it.total }
    val today = data.lastOrNull()?.total ?: 0L

    fun fmtTokens(v: Long): String = when {
        v >= 1_000_000L -> "%.1fM".format(v / 1_000_000.0)
        v >= 1_000L -> "%.1fK".format(v / 1_000.0)
        else -> v.toString()
    }

    // 3 个日期 label：data[0] / data[n/2] / data[n-1]
    val dateFmt = DateTimeFormatter.ofPattern("MM-dd")
    val n = data.size
    val leftDate = data.getOrNull(0)?.date?.let { runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull() } ?: ""
    val midDate = data.getOrNull(n / 2)?.date?.let { runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull() } ?: ""
    val rightDate = data.getOrNull(n - 1)?.date?.let { runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull() } ?: ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 柱状图（30 根，带颜色渐变）
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
            ) {
                val count = data.size.coerceAtLeast(1)
                val barGap = 2.dp.toPx()
                val barW = (size.width - barGap * (count - 1)) / count
                val baselineY = size.height
                // 底轴线
                drawLine(
                    color = axisColor,
                    start = Offset(0f, baselineY),
                    end = Offset(size.width, baselineY),
                    strokeWidth = 1f,
                )
                data.forEachIndexed { i, d ->
                    val h = size.height * (d.total.toFloat() / max.toFloat())
                    val x = i * (barW + barGap)
                    // 渐变 brush：top dark → bottom light
                    val brush = Brush.verticalGradient(
                        colors = listOf(barColor, barColorLight),
                        startY = baselineY - h,
                        endY = baselineY,
                    )
                    drawRect(
                        brush = brush,
                        topLeft = Offset(x, baselineY - h),
                        size = Size(barW, h),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // 右侧数字 label
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = fmtTokens(total),
                    color = labelColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "30d 总",
                    color = labelColor.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fmtTokens(today),
                    color = barColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "当天",
                    color = labelColor.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                )
            }
        }
        // 3 个日期 label：左/中/右
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = leftDate, color = labelColor, fontSize = 9.sp)
            Text(text = midDate, color = labelColor, fontSize = 9.sp)
            Text(text = rightDate, color = labelColor, fontSize = 9.sp)
        }
    }
}
