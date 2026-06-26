package com.canyoucount.timeit

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.canyoucount.timeit.ui.TimeItNavGraph
import com.canyoucount.timeit.ui.theme.TimeItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeItTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeItNavGraph()
                }
            }
        }
    }
}
