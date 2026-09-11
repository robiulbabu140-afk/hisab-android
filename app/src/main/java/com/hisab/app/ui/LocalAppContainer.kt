package com.hisab.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.hisab.app.AppContainer

/**
 * Screens read repositories straight off this CompositionLocal (set once in MainActivity)
 * rather than through per-screen ViewModels — there's no configuration-change-survival need
 * here since every repository already exposes Room Flows, and it keeps the screen count
 * manageable for a first version.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer provides ...)")
}
