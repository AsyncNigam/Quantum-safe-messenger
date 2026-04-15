package com.nigdroid.quantummessenger  // ← make sure this matches YOUR package

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    // This tells Kotlin "there is a function implemented in C++, not here"
    external fun stringFromJNI(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the compiled C++ library by its name (matches add_library in CMakeLists)
        System.loadLibrary("native-lib")

        val messageFromCpp = stringFromJNI()

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = messageFromCpp)
            }
        }
    }
}