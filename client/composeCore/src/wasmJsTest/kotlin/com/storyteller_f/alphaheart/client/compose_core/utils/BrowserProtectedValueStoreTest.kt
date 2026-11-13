/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

internal class BrowserProtectedValueStoreTest {
    @Test
    internal fun readsEmptyStorage() =
        runTest {
            withStore { store, _ -> assertNull(store.read("account:one")) }
        }

    @Test
    internal fun persistsAcrossConnectionRecreation() =
        runTest {
            withStore { store, database ->
                store.write("account:one", SECRET)
                store.close()
                val restarted = BrowserProtectedValueStore(database)
                try {
                    assertEquals(SECRET, restarted.read("account:one"))
                } finally {
                    restarted.close()
                }
            }
        }

    @Test
    internal fun isolatesAccountNamespaces() =
        runTest {
            withStore { store, _ ->
                store.write("account:one", SECRET)
                store.write("account:two", "another synthetic key")
                assertEquals(SECRET, store.read("account:one"))
                assertEquals("another synthetic key", store.read("account:two"))
                assertNull(store.read("account:three"))
            }
        }

    @Test
    internal fun storesCiphertextWithoutPlaintext() =
        runTest {
            withStore { store, database ->
                store.write("account:one", SECRET)
                val result = inspectStorage(database.toJsString(), "plaintext".toJsString()).await()
                assertEquals("encrypted", result.toString())
            }
        }

    @Test
    internal fun rejectsMasterKeyExport() =
        runTest {
            withStore { store, database ->
                store.write("account:one", SECRET)
                val result = inspectStorage(database.toJsString(), "export".toJsString()).await()
                assertEquals("non-extractable", result.toString())
            }
        }

    @Test
    internal fun rejectsTamperedCiphertext() =
        runTest {
            withStore { store, database ->
                store.write("account:one", SECRET)
                assertEquals(SECRET, store.read("account:one"))
                inspectStorage(database.toJsString(), "tamper".toJsString()).await()
                assertFails { store.read("account:one") }
            }
        }

    @Test
    internal fun rejectsCiphertextFromAnotherNamespace() =
        runTest {
            withStore { store, database ->
                store.write("account:one", SECRET)
                assertEquals(SECRET, store.read("account:one"))
                inspectStorage(database.toJsString(), "relocate".toJsString()).await()
                assertFails { store.read("account:two") }
            }
        }

    private suspend fun withStore(block: suspend (BrowserProtectedValueStore, String) -> Unit) {
        val database = uniqueDatabase().toString()
        val store = BrowserProtectedValueStore(database)
        try {
            block(store, database)
        } finally {
            withContext(NonCancellable) {
                try {
                    store.close()
                } finally {
                    deleteDatabase(database.toJsString()).await()
                }
            }
        }
    }

    private companion object {
        const val SECRET = "synthetic-private-key"
    }
}

private fun uniqueDatabase(): JsString = js("'protected-storage-test-' + crypto.randomUUID()")

private external interface StorageInspector : JsAny {
    fun deleteDatabase(name: JsString): Promise<JsAny?>
    fun inspectStorage(name: JsString, action: JsString): Promise<JsString>
}

private fun deleteDatabase(name: JsString): Promise<JsAny?> = storageInspector.deleteDatabase(name)

private fun inspectStorage(name: JsString, action: JsString): Promise<JsString> =
    storageInspector.inspectStorage(name, action)

// Inspect or corrupt browser records; encryption/decryption stays in compiled production Kotlin.
private val storageInspector: StorageInspector =
    js(
        """({
            deleteDatabase(name) {
                return new Promise((resolve, reject) => {
                    const request = indexedDB.deleteDatabase(name);
                    request.onsuccess = () => resolve(null);
                    request.onerror = () => reject(request.error);
                    request.onblocked = () => reject(new Error('Test database cleanup blocked: ' + name));
                });
            },
            async inspectStorage(name, action) {
                const db = await new Promise((resolve, reject) => {
                    const request = indexedDB.open(name, 1);
                    request.onsuccess = () => resolve(request.result);
                    request.onerror = () => reject(request.error);
                });
                const load = key => new Promise((resolve, reject) => {
                    const tx = db.transaction('entries', 'readonly');
                    const request = tx.objectStore('entries').get(key);
                    tx.oncomplete = () => resolve(request.result);
                    tx.onabort = () => reject(tx.error);
                });
                try {
                    if (action === 'export') {
                        const key = await load('master');
                        if (key.extractable !== false) throw new Error('Master key is extractable');
                        try {
                            await crypto.subtle.exportKey('raw', key);
                        } catch (error) {
                            if (error.name !== 'InvalidAccessError') throw error;
                            return 'non-extractable';
                        }
                        throw new Error('Master key export succeeded');
                    }
                    const record = await load('value:account:one');
                    if (action === 'plaintext') {
                        const secret = 'synthetic-private-key';
                        if (Object.values(localStorage).some(value => value.includes(secret))) {
                            throw new Error('Plaintext found in localStorage');
                        }
                        if (new TextDecoder().decode(record.data).includes(secret)) {
                            throw new Error('Plaintext found in IndexedDB');
                        }
                        return 'encrypted';
                    }
                    const data = new Uint8Array(record.data);
                    if (action === 'tamper') data[0] ^= 1;
                    await new Promise((resolve, reject) => {
                        const tx = db.transaction('entries', 'readwrite');
                        tx.objectStore('entries').put({iv:record.iv, data},
                            action === 'relocate' ? 'value:account:two' : 'value:account:one');
                        tx.oncomplete = resolve;
                        tx.onabort = () => reject(tx.error);
                    });
                    return 'modified';
                } finally {
                    db.close();
                }
            }
        })""",
    )
