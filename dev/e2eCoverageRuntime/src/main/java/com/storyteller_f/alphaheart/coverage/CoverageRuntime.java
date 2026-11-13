/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.coverage;

import java.io.File;
import java.io.IOException;
import kotlinx.kover.offline.runtime.api.KoverRuntime;

/** Test-only boundary shared by all woven boolean counter stores and snapshot serialization. */
public final class CoverageRuntime {
    private static final Object COUNTERS = new Object();

    static {
        String directory = System.getProperty("e2e.coverage.directory", System.getenv("E2E_COVERAGE_DIRECTORY"));
        if (directory != null) JvmCoverageControl.start(new File(directory));
    }

    private CoverageRuntime() {}

    /** Preserves BASTORE behavior for both Java byte[] and boolean[] operands. */
    public static void store(Object array, int index, int value) {
        if (array instanceof boolean[] counters) {
            synchronized (COUNTERS) {
                counters[index] = (value & 1) != 0;
            }
        } else {
            ((byte[]) array)[index] = (byte) value;
        }
    }

    /** No counter writes can race Kover serialization; the filesystem marker is published afterwards. */
    public static void saveReport(File destination) throws IOException {
        synchronized (COUNTERS) {
            KoverRuntime.saveReport(destination);
        }
    }
}
