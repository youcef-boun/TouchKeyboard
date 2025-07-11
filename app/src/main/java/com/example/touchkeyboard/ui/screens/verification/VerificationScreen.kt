package com.touchkeyboard.ui.screens.verification

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.clickable
import java.util.Calendar
import androidx.compose.material3.rememberModalBottomSheetState

@Composable
fun DurationOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun ScanningRay(modifier: Modifier = Modifier) {
    val RayColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier = modifier) {
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val infiniteTransition = rememberInfiniteTransition(label = "ScanningRay")
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "RayAnim"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .height(6.dp)
                .align(Alignment.TopCenter)
                .offset(y = with(LocalDensity.current) { (maxHeightPx * offsetY).toDp() })
                .background(Color(MaterialTheme.colorScheme.primary.value).copy(alpha = 0.45f), RoundedCornerShape(3.dp))
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel = hiltViewModel(),
    onVerificationComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val uiState by viewModel.verificationState.collectAsState()
    var handDetected by remember { mutableStateOf(false) }
    var detectionConfidence by remember { mutableStateOf(0f) }
    var lastDetectionLabel by remember { mutableStateOf(-1) }
    var lastDetectionConfidence by remember { mutableStateOf(0f) }
    var verificationFinished by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var isDetectionWindowActive by remember { mutableStateOf(false) }
    var isUnlockModalVisible by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableStateOf<Long?>(null) }

    // Duration for one full ray cycle (down and up)
    val rayCycleDurationMs = 2200L * 2

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerificationComplete()
        }
    }

    // Start detection window when verification starts
    LaunchedEffect(uiState.isVerifying) {
        if (uiState.isVerifying) {
            handDetected = false // reset only at the start
            verificationError = null
            isDetectionWindowActive = true
            verificationFinished = false
            isUnlockModalVisible = false
            delay(rayCycleDurationMs)
            isDetectionWindowActive = false
            if (handDetected) {
                verificationFinished = true
                isUnlockModalVisible = true
            } else {
                verificationError = "No hand detected. Please try again."
                viewModel.setVerifying(false)
            }
        }
    }

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
                text = "Keyboard Touch Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To unlock your apps, place your hands on your keyboard and press the verify button.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                // Always render CameraPreview as the base layer
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        onHandDetected = { confidence ->
                            if (isDetectionWindowActive && uiState.isVerifying && !verificationFinished) {
                                handDetected = true
                                detectionConfidence = confidence
                                lastDetectionLabel = 0
                                lastDetectionConfidence = confidence
                                Log.d(
                                    "VerificationScreen",
                                    "onHandDetected called: label=0, confidence=$confidence, windowActive=$isDetectionWindowActive"
                                )
                            }
                        }
                    )

                    // Overlay for real-time detection info (for debugging)
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                        Text(
                            text = "Detection: label=$lastDetectionLabel, conf=${
                                String.format(
                                    "%.2f",
                                    lastDetectionConfidence
                                )
                            }, windowActive=$isDetectionWindowActive",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            ).padding(4.dp)
                        )
                    }

                    // Overlay scanning ray if verifying
                    if (uiState.isVerifying && !verificationFinished) {
                        ScanningRay(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlay success message if verification finished
                    if (verificationFinished) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Hand on keyboard detected",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }

                    // Overlay error message if verification failed
                    if (verificationError != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = verificationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!verificationFinished) {
                PrimaryButton(
                    text = "Verify",
                    onClick = {
                        viewModel.setVerifying(true)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Calculate rest of day duration
    val calculateRestOfDayDuration: () -> Long = {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.timeInMillis - System.currentTimeMillis()
    }

    // Unlock duration modal
    if (isUnlockModalVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { isUnlockModalVisible = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Unlock your apps",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Duration options
                DurationOption(
                    text = "15 minutes",
                    isSelected = selectedDuration == 15 * 60 * 1000L,
                    onClick = { selectedDuration = 15 * 60 * 1000L }
                )
                DurationOption(
                    text = "30 minutes",
                    isSelected = selectedDuration == 30 * 60 * 1000L,
                    onClick = { selectedDuration = 30 * 60 * 1000L }
                )


                DurationOption(
                    text = "Rest of the day",
                    isSelected = selectedDuration == calculateRestOfDayDuration(),
                    onClick = { selectedDuration = calculateRestOfDayDuration() }
                )



                DurationOption(
                    text = "5 seconds for testing",
                    isSelected = selectedDuration == 5 * 1000L,
                    onClick = { selectedDuration = 5 * 1000L }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Confirm",
                    onClick = {
                        if (selectedDuration == null) {
                            Log.d("VerificationScreen", "Confirm pressed with no duration selected!")
                        } else {
                            Log.d("VerificationScreen", "Confirm pressed with duration: $selectedDuration")
                            viewModel.startVerification(selectedDuration!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedDuration != null
                )
            }
        }
    }



}