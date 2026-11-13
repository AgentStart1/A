/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "com.storyteller_f.alphaheart.bot"
version = "unspecified"
application {
    mainClass.set("com.storyteller_f.alphaheart.built_in_bot.BuiltInBotKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.napier)
    implementation(projects.shared)
    implementation(projects.client.core)
    implementation(projects.api)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.google.genai)
}
