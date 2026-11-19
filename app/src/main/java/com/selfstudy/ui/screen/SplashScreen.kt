package com.selfstudy.ui.screen

import android.graphics.Movie
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val SplashNavy = Color(0xFF001D39)
private val SplashText = Color(0xFFBDD8E9)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.94f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutBack),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(scaleAnim)
                .alpha(alphaAnim)
        ) {
            AnimatedSplashGif(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SelfStudy",
                style = MaterialTheme.typography.headlineLarge,
                color = SplashText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Belajar lebih teratur, raih hasil maksimal",
                style = MaterialTheme.typography.bodyMedium,
                color = SplashText.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun AnimatedSplashGif(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var movie by remember { mutableStateOf<Movie?>(null) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(context) {
        movie = withContext(Dispatchers.IO) {
            context.resources.openRawResource(com.selfstudy.R.raw.splash_animation).use {
                Movie.decodeStream(it)
            }
        }
    }

    LaunchedEffect(movie) {
        val activeMovie = movie ?: return@LaunchedEffect
        val startMillis = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMillis -> elapsedMillis = frameMillis - startMillis }
        }
    }

    val activeMovie = movie
    if (activeMovie == null) {
        Spacer(modifier = modifier.aspectRatio(16f / 9f))
    } else {
        val frameWidth = activeMovie.width().coerceAtLeast(1)
        val frameHeight = activeMovie.height().coerceAtLeast(1)
        Canvas(modifier = modifier.aspectRatio(frameWidth.toFloat() / frameHeight.toFloat())) {
            val duration = activeMovie.duration().coerceAtLeast(1)
            activeMovie.setTime((elapsedMillis % duration).toInt())
            val scaleFactor = minOf(size.width / frameWidth, size.height / frameHeight)
            val offsetX = (size.width - frameWidth * scaleFactor) / 2f
            val offsetY = (size.height - frameHeight * scaleFactor) / 2f
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val saveCount = nativeCanvas.save()
                nativeCanvas.translate(offsetX, offsetY)
                nativeCanvas.scale(scaleFactor, scaleFactor)
                activeMovie.draw(nativeCanvas, 0f, 0f)
                nativeCanvas.restoreToCount(saveCount)
            }
        }
    }
}
