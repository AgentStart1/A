plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.napier)
    implementation(projects.shared)
    implementation(libs.kotlinx.datetime)
    implementation(projects.cloud.pdf)
    implementation(libs.markdown)
    implementation(libs.pdfbox.layout)
    implementation(libs.pdfbox)
    implementation(libs.highlights)
    implementation(libs.dss.pades)
    implementation(libs.dss.token)
    implementation(libs.dss.spi)
    implementation(libs.dss.cms.`object`)
    implementation(libs.dss.service)
    implementation(libs.dss.utils.apache.commons)
    implementation(libs.dss.pades.pdfbox)
}
