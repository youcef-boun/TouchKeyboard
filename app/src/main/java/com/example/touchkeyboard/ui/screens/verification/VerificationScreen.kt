package com.touchkeyboard.ui.screens.verification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.touchkeyboard.R
import com.example.touchkeyboard.ui.screens.verification.CameraPreview
import com.touchkeyboard.ui.components.PrimaryButton
import com.example.touchkeyboard.ui.theme.BackgroundDark
import com.touchkeyboard.ui.viewmodels.VerificationViewModel

@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel = hiltViewModel(),
    onVerificationComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val uiState by viewModel.verificationState.collectAsState()

    // Effect to navigate back when verification is complete
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerificationComplete()
        }
    }

    // Reset state when leaving the screen
    DisposableEffect(key1 = viewModel) {
        onDispose {
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = if (uiState.isVerified) "Verification Complete!" else "Keyboard Touch Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.isVerified)
                    "You've successfully verified that you're touching your keyboard. Your apps are now unlocked!"
                else
                    "To unlock your apps, place your hands on your keyboard and press the verify button.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            // Show error message if there is one
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Camera preview or verification animation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isVerified -> {
                            Image(
                                painter = painterResource(id = R.drawable.verification_success),
                                contentDescription = "Verification successful",
                                modifier = Modifier.size(120.dp)
                            )
                        }
                        uiState.isVerifying -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        else -> {
                            var handDetected by remember { mutableStateOf(false) }
                            var detectionConfidence by remember { mutableStateOf(0f) }
                            if (!handDetected) {
                                CameraPreview(
                                    modifier = Modifier.size(180.dp),
                                    onHandDetected = { confidence ->
                                        handDetected = true
                                        detectionConfidence = confidence
                                    }
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Hand on keyboard detected",
                                        color = Color.Green,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Text(
                                        text = "Your apps are now unblocked",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isVerified) {
                PrimaryButton(
                    text = "Continue",
                    onClick = onVerificationComplete,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryButton(
                    text = if (uiState.isVerifying) "Verifying..." else "Verify Now",
                    onClick = { viewModel.startVerification() },
                    enabled = !uiState.isVerifying,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        if (uiState.isVerifying) {
                            // If currently verifying, just cancel
                            onCancel()
                        } else {
                            // Otherwise try to skip with remaining skips
                            viewModel.skipVerification()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isVerifying) "Cancel" else "Skip",
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}