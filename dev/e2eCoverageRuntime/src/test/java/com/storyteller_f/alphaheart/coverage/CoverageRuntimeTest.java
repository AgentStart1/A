/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.coverage;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CoverageRuntimeTest {
    @Test public void preservesByteAndBooleanStoreSemantics() {
        byte[] bytes = new byte[1];
        boolean[] flags = new boolean[1];
        CoverageRuntime.store(bytes, 0, 257);
        CoverageRuntime.store(flags, 0, 2);
        assertEquals(1, bytes[0]);
        assertFalse(flags[0]);
        CoverageRuntime.store(flags, 0, 3);
        assertTrue(flags[0]);
    }

    @Test public void retainsArrayBoundaryFailures() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> CoverageRuntime.store(new boolean[0], 0, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> CoverageRuntime.store(new byte[0], 0, 1));
        assertThrows(NullPointerException.class, () -> CoverageRuntime.store(null, 0, 1));
    }
}
