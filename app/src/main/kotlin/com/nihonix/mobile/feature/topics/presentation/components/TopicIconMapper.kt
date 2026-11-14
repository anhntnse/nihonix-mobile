package com.nihonix.mobile.feature.topics.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object TopicIconMapper {

    /**
     * Maps backend Material Icon decimal codepoints (from topic-icons.data.ts)
     * and slug hints to Compose Material ImageVectors.
     */
    fun resolveIcon(iconCode: Int?, slug: String? = null): ImageVector {
        slug?.lowercase()?.let { normalizedSlug ->
            when {
                normalizedSlug.contains("history") -> return Icons.Default.History
                normalizedSlug.contains("linguistics") || normalizedSlug.contains("globalization") -> return Icons.Default.Language
                normalizedSlug == "places" -> return Icons.Default.Place
            }
        }

        return when (iconCode) {
            59389 -> Icons.Default.WavingHand
            58835 -> Icons.Default.Numbers
            59573 -> Icons.Default.Schedule
            59470 -> Icons.Default.Groups
            58718 -> Icons.Default.Restaurant
            59574 -> Icons.Default.Event
            58378 -> Icons.Default.Palette
            61108 -> Icons.Default.AccessibilityNew
            58004 -> Icons.Default.WbSunny
            58845 -> Icons.Default.School
            58653 -> Icons.Default.DirectionsCar
            59141 -> Icons.Default.Checkroom
            58819 -> Icons.Default.SportsEsports
            59595 -> Icons.Default.ShoppingCart
            58670 -> Icons.Default.Explore
            57813 -> Icons.Default.LocalHospital
            59641 -> Icons.Default.Work
            57965 -> Icons.Default.Flight
            58570 -> Icons.Default.Pets
            60145 -> Icons.Default.Park
            59409 -> Icons.Default.Mood
            58727 -> Icons.Default.Public
            58768 -> Icons.Default.Home
            60253 -> Icons.Default.AcUnit
            60227 -> Icons.Default.Celebration
            58286 -> Icons.Default.Movie
            58159 -> Icons.Default.Computer
            58824 -> Icons.Default.SportsSoccer
            57411 -> Icons.Default.Tv
            59682 -> Icons.AutoMirrored.Filled.MenuBook
            57519 -> Icons.Default.BusinessCenter
            57527 -> Icons.AutoMirrored.Filled.Chat
            59541 -> Icons.Default.RestaurantMenu
            59817 -> Icons.Default.AccountBalance
            58363 -> Icons.Default.MusicNote
            59987 -> Icons.Default.Science
            59504 -> Icons.Default.Gavel
            else -> Icons.AutoMirrored.Filled.MenuBook
        }
    }
}
