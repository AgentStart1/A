/*
 * This is a private project. All rights reserved.
 */

import java.nio.file.Files

abstract class TestArtifactFiles {
    @get:javax.inject.Inject
    abstract val files: org.gradle.api.file.FileSystemOperations
}

plugins.withId("java") {
    extensions.getByType<JavaPluginExtension>().sourceSets.matching {
        it.name in setOf("test", "appiumTest", "windowsMediaTest", "e2eTest")
    }.all {
        val sourceSet = this
        val taskName = name
        val raw = layout.buildDirectory.dir("test-results/$taskName/artifacts")
        val report = layout.buildDirectory.dir("reports/$taskName")
        val aggregate = tasks.register<JavaExec>("${taskName}ArtifactReport") {
            group = "verification"
            description = "Generates collected artifact reports after $taskName."
            classpath = sourceSet.runtimeClasspath
            mainClass.set("com.storyteller_f.alphaheart.test.TestArtifactReportMain")
            args(raw.get().asFile.absolutePath, report.get().asFile.absolutePath)
            inputs.files(raw.map { it.asFileTree }).withPathSensitivity(PathSensitivity.RELATIVE)
            outputs.dir(report)
            mustRunAfter(taskName)
        }
        tasks.withType<Test>().matching { it.name == taskName }.configureEach {
            val artifactFiles = objects.newInstance<TestArtifactFiles>().files
            systemProperty("test.artifacts.dir", raw.get().asFile.absolutePath)
            outputs.dir(raw)
            // Cleanup runs only when Test executes, preserving cached or up-to-date raw results.
            doFirst {
                artifactFiles.delete { delete(raw) }
                Files.createDirectories(raw.get().asFile.toPath())
            }
            finalizedBy(aggregate)
        }
    }
}
