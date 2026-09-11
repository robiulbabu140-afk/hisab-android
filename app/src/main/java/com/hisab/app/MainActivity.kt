package com.hisab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.navigation.HisabNavHost
import com.hisab.app.ui.theme.HisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HisabApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                HisabTheme {
                    HisabNavHost()
                }
            }
        }
    }
}
