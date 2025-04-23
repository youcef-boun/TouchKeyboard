package com.example.touchkeyboard.ui.screens.Block

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class BlockedAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra("blocked_app_name") ?: "this app"
        setContent {
            BlockedAppScreen(
                appName = appName,
                onContinue = {
                    // Redirect to Home after animation
                    val homeIntent = Intent(this, Class.forName("com.example.touchkeyboard.ui.MainActivity"))
                    homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun BlockedAppScreen(appName: String, onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val backgroundAlpha = 0.65f
    LaunchedEffect(Unit) {
        visible = true
        delay(1500)
        visible = false
        delay(300)
        onContinue()
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dimmed background
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
        )
        // Fade-in block UI
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Access to $appName is blocked",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Stay focused!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
