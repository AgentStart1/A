/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.shared.commonJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.JsNumber
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString

internal actual fun createFileEngine(): FileEngine = OpfsFileEngine()

internal class OpfsFileEngine : FileEngine {
    private val bridge = opfsBridge
    override fun location(key: String): String = "downloads/$key"

    override suspend fun size(path: String): Long? =
        opfsCall {
            bridge.size(
                path.toJsString(),
            ).await()?.toDouble()?.toLong()
        }

    override suspend fun read(path: String, limit: Long): ByteArray =
        opfsCall {
            require(limit in 0..Int.MAX_VALUE.toLong()) { "Unsupported read size" }
            bridge.read(path.toJsString(), limit.toInt()).await().toByteArray()
        }

    override suspend fun openWriter(path: String, append: Boolean): DownloadFileWriter {
        val pending = bridge.open(path.toJsString(), append)
        val writer =
            try {
                opfsCall { pending.await() }
            } catch (failure: CancellationException) {
                try {
                    withContext(NonCancellable) { opfsCall { pending.await().close().await() } }
                } catch (closeFailure: Throwable) {
                    failure.addSuppressed(closeFailure)
                }
                throw failure
            }
        return object : DownloadFileWriter {
            override suspend fun write(bytes: ByteArray) =
                opfsCall {
                    writer.write(bytes.toInt8Array()).await()
                    Unit
                }
            override suspend fun close() =
                opfsCall {
                    writer.close().await()
                    Unit
                }
        }
    }

    override suspend fun listFiles(path: String): List<DownloadFileEntry> =
        opfsCall {
            commonJson.decodeFromString<List<DownloadFileEntry>>(bridge.list(path.toJsString()).await().toString())
        }

    override suspend fun delete(path: String) =
        opfsCall {
            bridge.remove(path.toJsString()).await()
            Unit
        }

    override suspend fun acquire(key: String): DownloadFileLease {
        val lease = opfsCall { bridge.lock(key.toJsString()) }
        try {
            opfsCall { lease.ready().await() }
            currentCoroutineContext().ensureActive()
        } catch (failure: Throwable) {
            try {
                withContext(NonCancellable) { opfsCall { lease.close().await() } }
            } catch (closeFailure: Throwable) {
                failure.addSuppressed(closeFailure)
            }
            throw failure
        }
        return DownloadFileLease { opfsCall { lease.close().await() } }
    }

    override suspend fun extractArchive(path: String) =
        opfsCall {
            extractDownloadArchive(path) { directory -> extractArchiveInWorker(path, directory) }
        }

    /** Only explicitly registered font entries participate in this cache budget. */
    suspend fun retainFont(path: String, maxFiles: Int = 12, maxBytes: Int = 128 * 1024 * 1024) =
        opfsCall {
            bridge.retain(path.toJsString(), maxFiles, maxBytes).await()
            Unit
        }
}

private suspend fun <T> opfsCall(block: suspend () -> T): T =
    try {
        block()
    } catch (failure: JsException) {
        currentCoroutineContext().ensureActive()
        throw IllegalStateException(
            "Browser storage operation failed: ${failure.message}",
            BrowserStorageFailure(failure),
        )
    }

private class BrowserStorageFailure(failure: JsException) : RuntimeException(failure.message)

private external interface OpfsWriter : JsAny {
    fun write(bytes: Int8Array): Promise<JsAny?>
    fun close(): Promise<JsAny?>
}

private external interface OpfsLease : JsAny {
    fun ready(): Promise<JsAny?>
    fun close(): Promise<JsAny?>
}

private external interface OpfsBridge : JsAny {
    fun size(path: JsString): Promise<JsNumber?>
    fun read(path: JsString, limit: Int): Promise<Int8Array>
    fun open(path: JsString, append: Boolean): Promise<OpfsWriter>
    fun list(path: JsString): Promise<JsString>
    fun remove(path: JsString): Promise<JsAny?>
    fun lock(key: JsString): OpfsLease
    fun retain(path: JsString, maxFiles: Int, maxBytes: Int): Promise<JsAny?>
}

private val opfsBridge: OpfsBridge =
    js(
        """(() => {
        const parts = path => {
            const values = path.split('/');
            if (values[0] !== 'downloads' || values.some(p => !p || p === '.' || p === '..' || /[\\:\0]/.test(p))) {
                throw new Error('Invalid download-cache location');
            }
            return values;
        };
        async function parent(path, create) {
            const values = parts(path);
            let directory = await navigator.storage.getDirectory();
            for (const value of values.slice(0, -1)) directory = await directory.getDirectoryHandle(value, {create});
            return [directory, values[values.length - 1]];
        }
        async function file(path, create = false) {
            const [directory, name] = await parent(path, create);
            return directory.getFileHandle(name, {create});
        }
        async function remove(path) {
            try {
                const [directory, name] = await parent(path, false);
                await directory.removeEntry(name, {recursive: true});
            } catch (error) { if (error.name !== 'NotFoundError') throw error; }
        }
        async function size(path) {
            try { return (await (await file(path)).getFile()).size; }
            catch (error) { if (error.name === 'NotFoundError' || error.name === 'TypeMismatchError') return null; throw error; }
        }
        async function list(path) {
            const output = [];
            async function visit(handle, prefix) {
                if (handle.kind === 'file') output.push({path: prefix, size: (await handle.getFile()).size});
                else for await (const [name, child] of handle.entries()) await visit(child, prefix + '/' + name);
            }
            try {
                const [directory, name] = await parent(path, false);
                await visit(await directory.getDirectoryHandle(name), path);
            } catch (error) { if (error.name !== 'NotFoundError') throw error; }
            return JSON.stringify(output);
        }
        function lock(key) {
            const controller = new AbortController();
            let release, ready, reject;
            const unlocked = new Promise(resolve => release = resolve);
            const entered = new Promise((resolve, fail) => {ready = resolve; reject = fail;});
            entered.catch(() => {});
            const finished = navigator.locks.request('a-downloads:' + key, {signal: controller.signal}, async () => {
                ready();
                await unlocked;
            });
            finished.catch(reject);
            return {
                ready: () => entered,
                async close() {
                    release();
                    controller.abort();
                    try { await finished; } catch (error) { if (error.name !== 'AbortError') throw error; }
                }
            };
        }
        async function writeJson(path, value) {
            const stream = await (await file(path, true)).createWritable();
            try { await stream.write(JSON.stringify(value)); } finally { await stream.close(); }
        }
        async function readJson(path, fallback) {
            try { return JSON.parse(await (await (await file(path)).getFile()).text()); }
            catch (error) { if (error.name === 'NotFoundError') return fallback; throw error; }
        }
        return {
            size, list, remove, lock,
            async read(path, limit) {
                const blob = await (await file(path)).getFile();
                if (blob.size > limit) throw new Error('File exceeds the size limit');
                return new Int8Array(await blob.arrayBuffer());
            },
            async open(path, append) {
                const handle = await file(path, true);
                const offset = append ? (await handle.getFile()).size : 0;
                const stream = await handle.createWritable({keepExistingData: append});
                try { if (append) await stream.seek(offset); }
                catch (error) { await stream.abort(); throw error; }
                return {write: bytes => stream.write(bytes), close: () => stream.close()};
            },
            async retain(path, maxFiles, maxBytes) {
                const values = parts(path);
                if (values.length !== 6 || values[1] !== 'public-resources-v1') {
                    throw new Error('Font retention requires a public resource location');
                }
                const server = values[2];
                const root = values.slice(0, 3).join('/');
                const lease = lock('font-cache:public-resources-v1:' + server);
                try {
                    await lease.ready();
                    const indexPath = root + '/font-cache.json';
                    const index = (await readJson(indexPath, [])).filter(item =>
                        item.path !== path && item.path.startsWith(root + '/') && parts(item.path).length === 6);
                    const entries = JSON.parse(await list(path + '.extracted'));
                    const sourceBytes = await size(path);
                    if (sourceBytes === null) return;
                    const decodedBytes = entries.reduce((n, item) => n + item.size, 0);
                    index.push({path, sourceBytes, decodedBytes});
                    let sources = index.reduce((n, item) => n + item.sourceBytes, 0);
                    let decoded = index.reduce((n, item) => n + item.decodedBytes, 0);
                    while (index.length > maxFiles || sources > maxBytes || decoded > maxBytes) {
                        const old = index.shift();
                        sources -= old.sourceBytes;
                        decoded -= old.decodedBytes;
                        const fields = parts(old.path);
                        const entryLease = lock('download:' + fields[2] + ':' + fields[3]);
                        try {
                            await entryLease.ready();
                            await remove(old.path.substring(0, old.path.lastIndexOf('/')));
                        } finally { await entryLease.close(); }
                    }
                    await writeJson(indexPath, index);
                } finally { await lease.close(); }
            }
        };
    })()""",
    )
