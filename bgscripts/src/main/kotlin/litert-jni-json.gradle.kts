/*
 * This is a private project. All rights reserved.
 */
import org.gradle.api.attributes.Attribute
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.VersionCatalogsExtension

val jniJsonEncoding = Attribute.of("com.storytellerf.litert.jni-json", String::class.java)

dependencies {
    artifactTypes.maybeCreate(ArtifactTypeDefinition.JAR_TYPE).attributes.attribute(jniJsonEncoding, "original")
    registerTransform(LiteRtJniJsonTransform::class) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            .attribute(jniJsonEncoding, "original")
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            .attribute(jniJsonEncoding, "escaped")
    }
}

configurations.configureEach {
    attributes.attribute(jniJsonEncoding, "escaped")
}

val patchedLiteRt = configurations.create("patchedLiteRt") {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
}
dependencies.add(patchedLiteRt.name, extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("litertlm-jvm").get())

tasks.register<Sync>("exportPatchedLiteRtJar") {
    from(patchedLiteRt.incoming.artifactView {
        componentFilter {
            it is ModuleComponentIdentifier && it.group == "com.google.ai.edge.litertlm" && it.module == "litertlm-jvm"
        }
    }.files)
    into(layout.buildDirectory.dir("litert-jni-json"))
}
