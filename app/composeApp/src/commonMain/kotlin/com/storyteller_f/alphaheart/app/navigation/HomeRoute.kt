/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("com.storyteller_f.alphaheart.app.pages.HomeRoute")
@Serializable
sealed interface HomeRoute : NavKey {
    @SerialName("com.storyteller_f.alphaheart.app.pages.HomeRoute.World")
    @Serializable
    data object World : HomeRoute {
        override fun toString(): String = HOME_START_DESTINATION_WORLD
    }

    @SerialName("com.storyteller_f.alphaheart.app.pages.HomeRoute.Communities")
    @Serializable
    data object Communities : HomeRoute {
        override fun toString(): String = HOME_START_DESTINATION_COMMUNITIES
    }

    @SerialName("com.storyteller_f.alphaheart.app.pages.HomeRoute.Rooms")
    @Serializable
    data object Rooms : HomeRoute {
        override fun toString(): String = HOME_START_DESTINATION_ROOMS
    }
}

internal const val HOME_START_DESTINATION_PREFERENCE_KEY = "home_start_destination"
internal const val HOME_START_DESTINATION_WORLD = "/world"
internal const val HOME_START_DESTINATION_COMMUNITIES = "/communities"
internal const val HOME_START_DESTINATION_ROOMS = "/rooms"

internal fun homeRouteFromPreference(value: String?): HomeRoute =
    when (value) {
        HOME_START_DESTINATION_COMMUNITIES -> HomeRoute.Communities
        HOME_START_DESTINATION_ROOMS -> HomeRoute.Rooms
        else -> HomeRoute.World
    }

internal fun homePageFromPreference(value: String?): Int =
    when (value) {
        HOME_START_DESTINATION_COMMUNITIES -> 1
        HOME_START_DESTINATION_ROOMS -> 2
        else -> 0
    }
