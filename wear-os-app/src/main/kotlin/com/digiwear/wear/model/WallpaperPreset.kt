package com.digiwear.wear.model

import androidx.annotation.DrawableRes
import com.digiwear.wear.R

data class WallpaperPreset(
    val id: String,
    val title: String,
    @DrawableRes val resId: Int
)

object WallpaperPresets {
    val ALL = listOf(
        WallpaperPreset("cyber_metropolis", "Cyber Metropolis", R.drawable.wp_cyber_metropolis),
        WallpaperPreset("gear_savannah", "Gear Savannah", R.drawable.wp_gear_savannah),
        WallpaperPreset("cyberspace_grid", "Cyberspace Grid", R.drawable.wp_cyberspace_grid),
        WallpaperPreset("magma_core", "Magma Core", R.drawable.wp_magma_core)
    )
}
