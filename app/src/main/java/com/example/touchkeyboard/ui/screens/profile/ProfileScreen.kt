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
import java.time.Duration

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.profileState.collectAsState()


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
                        onEdit = { /* Handle edit */ }
                    )
                    
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    
                    // Age Range
                    ProfileItem(
                        title = "Age range",
                        value = uiState.ageRange,
                        onEdit = { /* Handle edit */ }
                    )
                    
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    
                    // Screen Time
                    ProfileItem(
                        title = "Screen time",
                       value = uiState.screenTimeAverage,
                        onEdit = { /* Handle edit */ }
                    )
                    
                    Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
                    
                    // Keyboard Touch Count
                    ProfileItem(
                        title = "Keyboard touch count",
                        value = uiState.keyboardTouchCount,
                        onEdit = { /* Handle edit */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                        onEdit = { /* Handle edit */ }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileItem(
    title: String,
    value: String?,
    onEdit: () -> Unit = {}
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


