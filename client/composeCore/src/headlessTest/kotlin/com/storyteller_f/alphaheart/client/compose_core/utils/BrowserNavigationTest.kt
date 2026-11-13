/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrowserNavigationTest {
    @Test
    fun routeTypeFieldSurvivesSerialization() {
        val module =
            SerializersModule {
                polymorphic(NavKey::class) { subclass(NavigationPayload::class) }
            }
        val json = createBrowserNavigationJson(module)
        val serializer = PolymorphicSerializer(NavKey::class)
        val route = NavigationPayload("worker")
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }

    @Test
    fun browserBackAndForwardRestoreTheStack() {
        val fixture = Fixture()
        fixture.navigate("home", "room", "topic")
        fixture.history.go(-1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "room"), fixture.routes)
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "room", "topic"), fixture.routes)
        assertEquals(3, fixture.history.entries.size)
    }

    @Test
    fun appBackMovesHistoryWithoutAddingEntries() {
        val fixture = Fixture()
        fixture.navigate("home", "room", "topic")
        fixture.navigate("home")
        fixture.history.deliverPop()
        assertEquals(0, fixture.history.position)
        assertEquals(3, fixture.history.entries.size)
        fixture.history.go(2)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "room", "topic"), fixture.routes)
    }

    @Test
    fun newNavigationAfterBackReplacesForwardBranch() {
        val fixture = Fixture()
        fixture.navigate("home", "room", "topic")
        fixture.history.go(-1)
        fixture.history.deliverPop()
        fixture.navigate("home", "room", "settings")
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "room", "settings"), fixture.routes)
        assertEquals(3, fixture.history.entries.size)
    }

    @Test
    fun reloadRestoresTheCurrentRouteAndForwardHistory() {
        val fixture = Fixture()
        fixture.navigate("home", "room", "topic")
        fixture.history.go(-1)
        fixture.history.deliverPop()
        fixture.controller.unregister("app", fixture.binding)
        val reloaded = Fixture(fixture.history)
        assertEquals(listOf("home", "room"), reloaded.routes)
        reloaded.history.go(1)
        reloaded.history.deliverPop()
        assertEquals(listOf("home", "room", "topic"), reloaded.routes)
    }

    @Test
    fun rootBackLeavesTheDocumentWithoutPoppingRoot() {
        val fixture = Fixture()
        fixture.history.entries.add(0, null)
        fixture.history.position = 1
        fixture.history.go(-1)
        fixture.history.deliverPop()
        assertNull(fixture.history.read())
        assertEquals(listOf("home"), fixture.routes)
    }

    @Test
    fun nestedNavigationRestoresBothStacks() {
        val fixture = Fixture()
        fixture.navigate("home", "login")
        var nested = listOf("select")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/login", binding)
        nested = listOf("select", "input")
        fixture.controller.update("app/login", binding, nested)
        fixture.history.go(-1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "login"), fixture.routes)
        assertEquals(listOf("select"), nested)
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("select", "input"), nested)
    }

    @Test
    fun parentBackSkipsChildHistoryAndForwardRestoresIt() {
        val fixture = Fixture()
        fixture.navigate("home", "login")
        var nested = listOf("select")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/login", binding)
        nested = listOf("select", "input")
        fixture.controller.update("app/login", binding, nested)
        fixture.navigate("home")
        fixture.history.deliverPop()
        fixture.controller.unregister("app/login", binding)
        assertEquals(0, fixture.history.position)
        fixture.history.go(2)
        fixture.history.deliverPop()
        nested = listOf("select")
        fixture.controller.register("app/login", binding)
        assertEquals(listOf("home", "login"), fixture.routes)
        assertEquals(listOf("select", "input"), nested)
    }

    @Test
    fun navigationWhileAppBackIsPendingIsNotLost() {
        val fixture = Fixture()
        fixture.navigate("home", "room")
        fixture.navigate("home")
        fixture.navigate("home", "settings")
        fixture.history.deliverPop()
        assertEquals(listOf("home", "settings"), fixture.routes)
        assertEquals(2, fixture.history.entries.size)
    }

    @Test
    fun reopeningThePoppedRouteBeforePopCompletesIsNotLost() {
        val fixture = Fixture()
        fixture.navigate("home", "room")
        fixture.navigate("home")
        fixture.navigate("home", "room")
        fixture.history.deliverPop()
        assertEquals(listOf("home", "room"), fixture.routes)
        assertEquals(2, fixture.history.entries.size)
    }

    @Test
    fun replacedDetailCanGoBackAndForward() {
        val fixture = Fixture()
        fixture.navigate("home", "users", "user-one")
        fixture.navigate("home", "users", "user-two")
        fixture.history.go(-1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "users", "user-one"), fixture.routes)
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "users", "user-two"), fixture.routes)
        fixture.navigate("home", "users")
        fixture.history.deliverPop()
        assertEquals(1, fixture.history.position)
    }

    @Test
    fun replacingAncestorsCreatesTheirOwnHistory() {
        val fixture = Fixture()
        fixture.navigate("home", "room", "topic")
        fixture.navigate("home", "other", "topic")
        val length = fixture.history.entries.size
        fixture.navigate("home", "other")
        fixture.history.deliverPop()
        assertEquals(listOf("home", "other"), fixture.routes)
        assertEquals(3, fixture.history.position)
        assertEquals(length, fixture.history.entries.size)
    }

    @Test
    fun mountingSavedNestedStackCreatesItsHistory() {
        val fixture = Fixture()
        fixture.navigate("home", "login")
        var nested = listOf("select", "input")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/login", binding)
        fixture.history.go(-1)
        fixture.history.deliverPop()
        assertEquals(listOf("select"), nested)
        assertEquals(listOf("home", "login"), fixture.routes)
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("select", "input"), nested)
    }

    @Test
    fun parentBackRestoresLatestNestedPage() {
        val fixture = Fixture()
        fixture.navigate("home", "files")
        var nested = listOf("uploaded")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/files", binding)
        nested = listOf("uploaded", "upload")
        fixture.controller.update("app/files", binding, nested)
        fixture.navigate("home", "files", "preview")
        fixture.controller.unregister("app/files", binding)
        fixture.navigate("home", "files")
        fixture.history.deliverPop()
        nested = listOf("uploaded")
        fixture.controller.register("app/files", binding)
        assertEquals(2, fixture.history.position)
        assertEquals(listOf("uploaded", "upload"), nested)
        fixture.history.go(1)
        fixture.history.deliverPop()
        assertEquals(listOf("home", "files", "preview"), fixture.routes)
    }

    @Test
    fun pendingParentBackDoesNotPublishRemountedChild() {
        val fixture = Fixture()
        fixture.navigate("home", "files")
        var nested = listOf("uploaded", "upload")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/files", binding)
        fixture.navigate("home", "files", "preview")
        fixture.controller.unregister("app/files", binding)
        val length = fixture.history.entries.size
        fixture.navigate("home", "files")
        fixture.controller.register("app/files", binding)
        fixture.controller.update("app/files", binding, nested)
        assertEquals(length, fixture.history.entries.size)
        fixture.history.deliverPop()
        assertEquals(listOf("uploaded", "upload"), nested)
        assertEquals(2, fixture.history.position)
    }

    @Test
    fun deepChildNavigationUpdatesEveryAncestor() {
        val fixture = Fixture()
        fixture.navigate("home", "files")
        var nested = listOf("uploaded", "folder")
        val binding = BrowserNavigationBinding({ nested }, { nested = it })
        fixture.controller.register("app/files", binding)
        var detail = listOf("list")
        val detailBinding = BrowserNavigationBinding({ detail }, { detail = it })
        fixture.controller.register("app/files/folder", detailBinding)
        detail = listOf("list", "selected")
        fixture.controller.update("app/files/folder", detailBinding, detail)
        nested = listOf("uploaded", "folder", "preview")
        fixture.controller.update("app/files", binding, nested)
        fixture.controller.unregister("app/files/folder", detailBinding)
        nested = listOf("uploaded", "folder")
        fixture.controller.update("app/files", binding, nested)
        fixture.history.deliverPop()
        detail = listOf("list")
        fixture.controller.register("app/files/folder", detailBinding)
        assertEquals(listOf("list", "selected"), detail)
    }

    @Test
    fun unregisterStopsHistoryCallbacks() {
        val fixture = Fixture()
        fixture.controller.unregister("app", fixture.binding)
        assertNull(fixture.history.onPop)
    }

    @Test
    fun emptyStackCannotCreateOrRestoreAnEmptyRoot() {
        val fixture = Fixture()
        fixture.controller.update("app", fixture.binding, emptyList())
        assertEquals(1, fixture.history.entries.size)
        val emptyStack = BrowserNavigationStack(emptyList(), emptyList())
        val emptyEntry = BrowserNavigationEntry("app", stacks = mapOf("app" to emptyStack))
        fixture.history.entries[0] = Json.encodeToString(emptyEntry)
        fixture.history.deliverPop()
        assertEquals(listOf("home"), fixture.routes)
    }
}

private class Fixture(val history: FakeBrowserHistory = FakeBrowserHistory()) {
    var routes = listOf("home")
    val binding = BrowserNavigationBinding({ routes }, { routes = it })
    val controller = BrowserNavigationController("app", history).also { it.register("app", binding) }

    fun navigate(vararg keys: String) {
        routes = keys.toList()
        controller.update("app", binding, routes)
    }
}

private class FakeBrowserHistory : BrowserHistory {
    val entries = mutableListOf<String?>(null)
    var position = 0
    var onPop: (() -> Unit)? = null
    private var target = 0

    override fun read(): String? = entries[position]
    override fun replace(value: String) {
        entries[position] = value
    }
    override fun push(value: String) {
        while (entries.lastIndex > position) entries.removeAt(entries.lastIndex)
        entries.add(value)
        position++
    }
    override fun go(delta: Int) {
        target = (position + delta).coerceIn(0, entries.lastIndex)
    }
    override fun listen(onPop: () -> Unit): () -> Unit {
        this.onPop = onPop
        return { this.onPop = null }
    }
    fun deliverPop() {
        position = target
        onPop?.invoke()
    }
}

@Serializable
private data class NavigationPayload(val type: String) : NavKey
