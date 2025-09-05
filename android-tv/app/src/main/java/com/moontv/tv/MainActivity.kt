package com.moontv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.moontv.tv.tvui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvRoot(this)
        }
    }
}

@Composable
fun TvRoot(activity: ComponentActivity) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(activity)
        }
    }
}

// Demo helpers removed; HomeScreen handles navigation and intents
