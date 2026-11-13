/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.coverage;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/** JVM-only blocking file control, isolated from application coroutines and absent from normal products. */
final class JvmCoverageControl {
    private JvmCoverageControl() {}

    static void start(File root) {
        try {
            // Reflection keeps ProcessHandle out of the Android runtime's referenced API surface.
            Class<?> handle = Class.forName("java.lang.ProcessHandle");
            Object current = handle.getMethod("current").invoke(null);
            long pid = (Long) handle.getMethod("pid").invoke(current);
            Path directory = root.toPath().resolve(Long.toString(pid));
            Files.createDirectories(directory);
            var resource = JvmCoverageControl.class.getClassLoader().getResource("e2e-coverage.properties");
            if (resource == null) throw new IOException("Missing original-bytecode build identity");
            try (var descriptor = new BufferedInputStream(resource.openStream())) {
                Files.write(directory.resolve("classes.properties"), descriptor.readAllBytes());
            }
            WatchService watcher = directory.getFileSystem().newWatchService();
            directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            // This daemon owns only the platform's blocking WatchService. It never owns business work.
            Thread control = new Thread(() -> watch(directory, watcher), "e2e-coverage-control");
            control.setDaemon(true);
            Files.writeString(directory.resolve("running"), "file-control", StandardCharsets.UTF_8);
            control.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                save(directory);
                try {
                    Files.deleteIfExists(directory.resolve("running"));
                } catch (IOException failure) {
                    fail(directory, failure);
                }
            }, "e2e-coverage-shutdown"));
            Files.writeString(directory.resolve("ready"), "kover-offline-serialized-counters", StandardCharsets.UTF_8);
        } catch (ReflectiveOperationException | IOException failure) {
            throw new IllegalStateException("Cannot initialize E2E coverage control", failure);
        }
    }

    private static void watch(Path directory, WatchService watcher) {
        try (watcher) {
            while (true) {
                WatchKey key = watcher.take();
                key.pollEvents();
                if (Files.deleteIfExists(directory.resolve("request"))) save(directory);
                if (!key.reset()) return;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException failure) {
            fail(directory, failure);
        }
    }

    private static synchronized void save(Path directory) {
        try {
            Files.deleteIfExists(directory.resolve("complete"));
            CoverageRuntime.saveReport(directory.resolve("report.pending.ic").toFile());
            Files.move(directory.resolve("report.pending.ic"), directory.resolve("report.ic"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(directory.resolve("report.ic")) == 0) throw new IOException("Empty coverage report");
            Files.writeString(directory.resolve("complete.pending"), "serialized-counter-snapshot", StandardCharsets.UTF_8);
            Files.move(directory.resolve("complete.pending"), directory.resolve("complete"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException failure) {
            fail(directory, failure);
        }
    }

    private static void fail(Path directory, Exception failure) {
        try {
            Files.writeString(directory.resolve("error"), failure.getClass().getName() + ": " + failure.getMessage(), StandardCharsets.UTF_8);
        } catch (IOException secondary) {
            failure.addSuppressed(secondary);
            throw new IllegalStateException("Coverage failure could not be recorded", failure);
        }
    }
}
