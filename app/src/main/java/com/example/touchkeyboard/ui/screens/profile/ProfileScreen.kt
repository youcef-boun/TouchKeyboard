package com.touchkeyboard.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.touchkeyboard.ui.theme.BackgroundDark
import com.touchkeyboard.ui.viewmodels.ProfileViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.touchkeyboard.ui.screens.onboarding.UserGoal
import com.example.touchkeyboard.ui.viewmodels.HomeViewModel
import java.util.concurrent.TimeUnit


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel() ,
    homeviewModel : HomeViewModel = hiltViewModel()
) {
    val screenTimeState by homeviewModel.screenTimeState.collectAsState()
    val uiState by viewModel.profileState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog state
    val showGoalDialog = remember { mutableStateOf(false) }
    val showAgeDialog = remember { mutableStateOf(false) }

    // Goal options
    val goalOptions = listOf(
        UserGoal.BE_PRESENT,
        UserGoal.CONNECT_PEOPLE,
        UserGoal.FOCUS_WORK,
        UserGoal.CUSTOM
    )
    // Age range options
    val ageOptions = listOf(
        "Under 18",
        "18-24",
        "25-34",
        "35+"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // User Information Section
            Text(
                text = "YOUR INFORMATION",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Goal
                    ProfileItem(
                        title = "Goal",
                        value = uiState.userGoal.title,
                        onEdit = { showGoalDialog.value = true }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    // Age Range
                    ProfileItem(
                        title = "Age range",
                        value = uiState.ageRange,
                        onEdit = { showAgeDialog.value = true }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    // Screen Time (not editable)
                    ProfileItem(
                        title = "Screen time",
                        value = formatDuration(screenTimeState),
                        onEdit = {},
                        editable = false
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    // Keyboard Touch Count
                    ProfileItem(
                        title = "Keyboard touch count",
                        value = uiState.keyboardTouchCount,
                        onEdit = { /* Handle edit */ },
                        editable = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
            // Account Section
            Text(
                text = "ACCOUNT",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Subscription
                    ProfileItem(
                        title = "Subscription",
                        value = uiState.subscriptionTier,
                        onEdit = { /* Handle edit */ }
                    )
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    // Skip Settings
                    ProfileItem(
                        title = "Skip settings",
                        value = uiState.remainingSkips,
                        onEdit = { /* Handle edit */ }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
            // Help Section
            Text(
                text = "HELP",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // FAQs
                    ProfileItem(
                        title = "FAQs",
                        value = null,
                        onEdit = {
                            Toast.makeText(context, "FAQs coming soon!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Goal dialog
        if (showGoalDialog.value) {
            AlertDialog(
                onDismissRequest = { showGoalDialog.value = false },
                title = { Text("Select your goal") },
                text = {
                    Column {
                        goalOptions.forEach { goal ->
                            TextButton(
                                onClick = {
                                    viewModel.updateUserGoal(goal)
                                    showGoalDialog.value = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(goal.title)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showGoalDialog.value = false }) { Text("Cancel") }
                }
            )
        }
        // Age range dialog
        if (showAgeDialog.value) {
            AlertDialog(
                onDismissRequest = { showAgeDialog.value = false },
                title = { Text("Select your age range") },
                text = {
                    Column {
                        ageOptions.forEach { age ->
                            TextButton(
                                onClick = {
                                    viewModel.updateAgeRange(age)
                                    showAgeDialog.value = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(age)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAgeDialog.value = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ProfileItem(
    title: String,
    value: String?,
    onEdit: () -> Unit = {},
    editable: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (editable) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        else -> "$minutes min"
    }
}