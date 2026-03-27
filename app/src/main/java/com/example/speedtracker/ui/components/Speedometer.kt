package com.example.speedtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedtracker.ui.theme.PrimaryOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Speedometer(
    speed: Double,
    theme: String = "Standard",
    maxSpeed: Double = 180.0,
    modifier: Modifier = Modifier.size(280.dp)
) {
    val animatedSpeed = remember { Animatable(0f) }

    LaunchedEffect(speed) {
        animatedSpeed.animateTo(
            targetValue = speed.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val themeConfig = getThemeConfig(theme)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // 1. Draw Background Arc
            drawArc(
                color = themeConfig.backgroundColor.copy(alpha = 0.2f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(radius * 0.1f, radius * 0.1f),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw Color Zones (Only for Standard/Sport)
            if (theme != "Future") {
                drawSpeedZones(radius, themeConfig)
            }

            // 3. Draw Markings and Ticks
            drawMarkings(radius, maxSpeed, themeConfig)

            // 4. Draw Needle
            drawNeedle(center, radius, animatedSpeed.value, maxSpeed, themeConfig)
            
            // 5. Center Hub
            drawCircle(
                color = Color.Black,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = themeConfig.accentColor,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // 6. Digital Speed Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 100.dp)
        ) {
            Text(
                text = String.format("%.0f", animatedSpeed.value),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black
                ),
                color = themeConfig.textColor
            )
            Text(
                text = "KM/H",
                style = MaterialTheme.typography.labelMedium,
                color = themeConfig.textColor.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }
    }
}

data class SpeedometerThemeConfig(
    val backgroundColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val needleColor: Color,
    val majorTickColor: Color,
    val minorTickColor: Color
)

private fun getThemeConfig(theme: String): SpeedometerThemeConfig {
    return when (theme) {
        "Sport" -> SpeedometerThemeConfig(
            backgroundColor = Color(0xFFD32F2F),
            accentColor = Color(0xFFF44336),
            textColor = Color.White,
            needleColor = Color.Red,
            majorTickColor = Color.White,
            minorTickColor = Color.White.copy(alpha = 0.6f)
        )
        "Future" -> SpeedometerThemeConfig(
            backgroundColor = Color(0xFF00BCD4),
            accentColor = Color(0xFF18FFFF),
            textColor = Color(0xFF18FFFF),
            needleColor = Color(0xFF18FFFF),
            majorTickColor = Color(0xFF18FFFF).copy(alpha = 0.8f),
            minorTickColor = Color(0xFF18FFFF).copy(alpha = 0.3f)
        )
        else -> SpeedometerThemeConfig(
            backgroundColor = Color.DarkGray,
            accentColor = PrimaryOrange,
            textColor = Color.White,
            needleColor = PrimaryOrange,
            majorTickColor = Color.White,
            minorTickColor = Color.Gray
        )
    }
}

private fun DrawScope.drawSpeedZones(radius: Float, config: SpeedometerThemeConfig) {
    val strokeWidth = 8.dp.toPx()
    val zoneSize = Size(radius * 1.8f, radius * 1.8f)
    val topLeft = Offset(radius * 0.1f, radius * 0.1f)

    drawArc(
        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
        startAngle = 150f,
        sweepAngle = (40f / 180f) * 240f,
        useCenter = false,
        topLeft = topLeft,
        size = zoneSize,
        style = Stroke(width = strokeWidth)
    )

    drawArc(
        color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
        startAngle = 150f + (40f / 180f) * 240f,
        sweepAngle = (40f / 180f) * 240f,
        useCenter = false,
        topLeft = topLeft,
        size = zoneSize,
        style = Stroke(width = strokeWidth)
    )

    drawArc(
        color = Color(0xFFFF5252).copy(alpha = 0.6f),
        startAngle = 150f + (80f / 180f) * 240f,
        sweepAngle = (100f / 180f) * 240f,
        useCenter = false,
        topLeft = topLeft,
        size = zoneSize,
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawMarkings(radius: Float, maxSpeed: Double, config: SpeedometerThemeConfig) {
    val center = Offset(size.width / 2, size.height / 2)
    
    for (i in 0..180 step 20) {
        val angleInDegrees = 150f + (i.toFloat() / maxSpeed.toFloat()) * 240f
        val angleInRad = angleInDegrees * PI / 180f
        
        val startLocation = Offset(
            (center.x + (radius * 0.85f) * cos(angleInRad)).toFloat(),
            (center.y + (radius * 0.85f) * sin(angleInRad)).toFloat()
        )
        val endLocation = Offset(
            (center.x + (radius * 0.95f) * cos(angleInRad)).toFloat(),
            (center.y + (radius * 0.95f) * sin(angleInRad)).toFloat()
        )
        
        drawLine(
            color = config.majorTickColor,
            start = startLocation,
            end = endLocation,
            strokeWidth = 2.dp.toPx()
        )
    }

    for (i in 0..180 step 5) {
        if (i % 20 == 0) continue
        val angleInDegrees = 150f + (i.toFloat() / maxSpeed.toFloat()) * 240f
        val angleInRad = angleInDegrees * PI / 180f
        
        val startLocation = Offset(
            (center.x + (radius * 0.90f) * cos(angleInRad)).toFloat(),
            (center.y + (radius * 0.90f) * sin(angleInRad)).toFloat()
        )
        val endLocation = Offset(
            (center.x + (radius * 0.95f) * cos(angleInRad)).toFloat(),
            (center.y + (radius * 0.95f) * sin(angleInRad)).toFloat()
        )
        
        drawLine(
            color = config.minorTickColor,
            start = startLocation,
            end = endLocation,
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawNeedle(center: Offset, radius: Float, speed: Float, maxSpeed: Double, config: SpeedometerThemeConfig) {
    val angleInDegrees = 150f + (speed / maxSpeed.toFloat()) * 240f
    
    rotate(degrees = angleInDegrees, pivot = center) {
        drawLine(
            color = config.needleColor,
            start = center,
            end = Offset(center.x + radius * 0.8f, center.y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
