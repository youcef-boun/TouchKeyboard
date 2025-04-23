package com.example.touchkeyboard.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.touchkeyboard.permissions.UsagePermissionManager
import com.example.touchkeyboard.usage.AppUsageInfo
import com.example.touchkeyboard.usage.HourlyUsage
import com.example.touchkeyboard.usage.UsageStatsCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val usageStatsCollector = UsageStatsCollector(application)
    private val permissionManager = UsagePermissionManager(application)

    private val _screenTimeState = MutableStateFlow(0L)
    val screenTimeState: StateFlow<Long> = _screenTimeState.asStateFlow()

    private val _appUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageInfo>> = _appUsageList.asStateFlow()

    private val _hourlyUsage = MutableStateFlow<List<HourlyUsage>>(emptyList())
    val hourlyUsage: StateFlow<List<HourlyUsage>> = _hourlyUsage.asStateFlow()

    private val _weeklyStats = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val weeklyStats: StateFlow<Map<Int, Long>> = _weeklyStats.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        checkPermissionAndLoadData()
    }

    private fun checkPermissionAndLoadData() {
        viewModelScope.launch {
            _hasUsagePermission.value = permissionManager.hasUsagePermission()
            if (_hasUsagePermission.value) {
                loadUsageData()
            }
        }
    }

    fun loadUsageData() {
        viewModelScope.launch {
            // Get today's total screen time
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            _screenTimeState.value = usageStatsCollector.getTotalScreenTime(startTime, endTime)

            // Get app usage breakdown
            _appUsageList.value = usageStatsCollector.getDailyUsageStats()
                .sortedByDescending { it.totalTimeInForeground }

            // Get hourly breakdown
            _hourlyUsage.value = usageStatsCollector.getHourlyBreakdown()

            // Get weekly stats
            _weeklyStats.value = usageStatsCollector.getWeeklyStats()
        }
    }

    fun refreshData() {
        checkPermissionAndLoadData()
    }
}
