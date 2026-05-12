package com.kutira.kushala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kutira.kushala.ui.KutiraNavGraph
import com.kutira.kushala.ui.theme.KutiraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KutiraTheme {
                KutiraNavGraph()
            }
        }
    }
}
