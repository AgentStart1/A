/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.app.pages.community.CommunityRefCell
import com.storyteller_f.alphaheart.app.pages.room.RoomRefCell
import com.storyteller_f.alphaheart.app.pages.user.UserRefCell
import com.storyteller_f.shared.type.toPrimaryKeyOrNull

internal data class TopicRoute(val pattern: String, val builder: @Composable (Map<String, String>) -> Unit) {
    companion object {
        /** Resolves [string] to a reference renderer and its path parameters. */
        fun parseRefUri(
            string: String,
        ): Pair<
            @Composable (
                (
                    Map<String, String>,
                ) -> Unit
            )?,
            MutableMap<String, String>,
            > {
            val target = string.split("/")
            val matched =
                ROUTE.firstNotNullOfOrNull { route ->
                    route.match(target)?.let { parameters ->
                        route.builder to parameters
                    }
                }
            return matched ?: Pair(null, mutableMapOf())
        }
    }
}

private fun TopicRoute.match(target: List<String>): MutableMap<String, String>? {
    val patternSegments = pattern.split("/")
    if (target.size != patternSegments.size) return null
    val parameters = mutableMapOf<String, String>()
    patternSegments.zip(target).forEach { (patternSegment, targetSegment) ->
        if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
            parameters[patternSegment.removeSurrounding("{", "}")] = targetSegment
        } else if (patternSegment != targetSegment) {
            return null
        }
    }
    return parameters
}

private val ROUTE: MutableList<TopicRoute> =
    mutableListOf(
        TopicRoute("/topic/{id}") { params ->
            params["id"]?.toPrimaryKeyOrNull()?.let {
                TopicRefCell(it)
            }
        },
        TopicRoute("/topic/a/{aid}") { params ->
            params["aid"]?.let {
                TopicRefCell(it)
            }
        },
        TopicRoute("/room/{id}") { params ->
            params["id"]?.toPrimaryKeyOrNull()?.let {
                RoomRefCell(it)
            }
        },
        TopicRoute("/room/a/{aid}") { params ->
            params["aid"]?.let {
                RoomRefCell(it)
            }
        },
        TopicRoute("/community/{id}") { params ->
            params["id"]?.toPrimaryKeyOrNull()?.let {
                CommunityRefCell(it)
            }
        },
        TopicRoute("/community/a/{aid}") { params ->
            params["aid"]?.let {
                CommunityRefCell(it)
            }
        },
        TopicRoute("/user/{id}") { params ->
            params["id"]?.toPrimaryKeyOrNull()?.let {
                UserRefCell(it)
            }
        },
        TopicRoute("/user/a/{aid}") { params ->
            params["aid"]?.let {
                UserRefCell(it)
            }
        },
    )
