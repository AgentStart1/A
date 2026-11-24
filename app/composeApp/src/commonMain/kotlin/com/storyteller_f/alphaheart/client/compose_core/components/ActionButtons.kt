/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnAddFavorite
import com.storyteller_f.alphaheart.app.common.OnAddSubscription
import com.storyteller_f.alphaheart.app.common.OnRemoveFavorite
import com.storyteller_f.alphaheart.app.common.OnRemoveSubscription
import com.storyteller_f.alphaheart.app.favorite_action_label
import com.storyteller_f.alphaheart.app.subscription_action_label
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.IconRes
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.removeFavorite
import com.storyteller_f.alphaheart.client.core.removeSubscription
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
fun FavoriteButton(favoriteId: PrimaryKey?, infoTuple: ObjectTuple) {
    val dialogController = LocalGlobalTask.current
    val taskId = "favorite-${infoTuple.objectId}"
    val state = dialogController.stateMap[taskId]
    val icon =
        if (state is LoadingState.Loading) {
            IconRes.Loading
        } else if (favoriteId != null) {
            IconRes.Vector(actionIcon(ActionGlyph.HEART, selected = true))
        } else {
            IconRes.Vector(actionIcon(ActionGlyph.HEART))
        }
    ButtonNav(icon, stringResource(Res.string.favorite_action_label), semanticDescription = "favorite-action") {
        dialogController.launch(taskId) {
            use {
                request {
                    if (favoriteId != null) {
                        removeFavorite(infoTuple.objectId, infoTuple.objectType).onSuccess {
                            emitEvent(OnRemoveFavorite(infoTuple))
                        }
                    } else {
                        addFavorite(NewFavorite(infoTuple.objectType, infoTuple.objectId)).onSuccess {
                            emitEvent(OnAddFavorite(infoTuple))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionButton(subscriptionId: PrimaryKey?, infoTuple: ObjectTuple) {
    val dialogController = LocalGlobalTask.current
    val taskId = "subscription-${infoTuple.objectId}"
    val state = dialogController.stateMap[taskId]
    val icon =
        if (state is LoadingState.Loading) {
            IconRes.Loading
        } else if (subscriptionId != null) {
            IconRes.Vector(actionIcon(ActionGlyph.BELL, selected = true))
        } else {
            IconRes.Vector(actionIcon(ActionGlyph.BELL))
        }
    ButtonNav(icon, stringResource(Res.string.subscription_action_label), semanticDescription = "subscribe-action") {
        dialogController.launch(taskId) {
            use {
                request {
                    if (subscriptionId != null) {
                        removeSubscription(infoTuple.objectId, infoTuple.objectType).onSuccess {
                            emitEvent(OnRemoveSubscription(infoTuple))
                        }
                    } else {
                        addSubscription(
                            NewSubscription(infoTuple.objectId, infoTuple.objectType),
                        ).onSuccess {
                            emitEvent(OnAddSubscription(infoTuple))
                        }
                    }
                }
            }
        }
    }
}
