package com.nautesh.challengeclock

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nautesh.challengeclock.ui.AppShell
import com.nautesh.challengeclock.ui.ChallengeClockTheme
import com.nautesh.challengeclock.ui.ThemeShowcase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // `adb shell am start -n com.nautesh.challengeclock/.MainActivity --ez showcase true` shows the theme check.
        // Debug builds only: this activity is exported, so any app could otherwise open it.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val showcase = debuggable && intent.getBooleanExtra("showcase", false)
        setContent { ChallengeClockTheme { if (showcase) ThemeShowcase() else AppShell() } }
    }
}
