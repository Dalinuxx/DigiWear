package com.digiwear.companion.model

import androidx.annotation.DrawableRes
import com.digiwear.companion.R

data class WallpaperPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val resId: Int
)

object WallpaperPresets {
    val ALL = listOf(
        WallpaperPreset("cyber_metropolis", "Cyber Metropolis", "Neon Skyline & Circuit Floor", R.drawable.wp_cyber_metropolis),
        WallpaperPreset("gear_savannah", "Gear Savannah", "Ancient Cogs & Emerald Plains", R.drawable.wp_gear_savannah),
        WallpaperPreset("cyberspace_grid", "Cyberspace Grid", "AMOLED Wireframe Void", R.drawable.wp_cyberspace_grid),
        WallpaperPreset("magma_core", "Magma Core", "Industrial Cavern & Molten Grid", R.drawable.wp_magma_core)
    )
}
