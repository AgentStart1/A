/*
 * This is a private project. All rights reserved.
 */

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule
import javax.inject.Inject

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

abstract class WasmNodeImportMetaRule @Inject constructor(name: String) : KotlinWebpackRule(name) {
    abstract val loaderPath: Property<String>

    init {
        enabled.convention(true)
        test.convention("/\\.import-object\\.mjs$/")
    }

    protected override fun loaders(): List<KotlinWebpackRule.Loader> =
        listOf(KotlinWebpackRule.Loader("require.resolve(${json(loaderPath.get())})", emptyMap(), emptyList()))
}

val browserConfig = rootProject.layout.projectDirectory.dir("config/karma")

val importMetaLoader = rootProject.layout.projectDirectory.file("config/webpack/wasm-node-import-meta.cjs")

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            testTask {
                inputs.file(importMetaLoader)
                inputs.dir(browserConfig)
                useKarma {
                    useConfigDirectory(browserConfig.asFile)
                    useChromeHeadless()
                    webpackConfig.rules {
                        rule(WasmNodeImportMetaRule::class.java, "wasmNodeImportMeta") {
                            loaderPath.set(importMetaLoader.asFile.invariantSeparatorsPath)
                        }
                    }
                }
            }
        }
    }
}
