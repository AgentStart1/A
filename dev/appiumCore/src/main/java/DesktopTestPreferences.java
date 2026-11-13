/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/** Each test app runs in a fresh JVM; never read or write the host user's preferences. */
public final class DesktopTestPreferences implements PreferencesFactory {
    private final Preferences user = new MemoryPreferences(null, "");
    private final Preferences system = new MemoryPreferences(null, "");

    @Override
    public Preferences userRoot() { return user; }

    @Override
    public Preferences systemRoot() { return system; }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new LinkedHashMap<>();

        private MemoryPreferences(AbstractPreferences parent, String name) { super(parent, name); }

        @Override
        protected void putSpi(String key, String value) { values.put(key, value); }

        @Override
        protected String getSpi(String key) { return values.get(key); }

        @Override
        protected void removeSpi(String key) { values.remove(key); }

        @Override
        protected void removeNodeSpi() { values.clear(); }

        @Override
        protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }

        @Override
        protected String[] childrenNamesSpi() { return new String[0]; }

        @Override
        protected AbstractPreferences childSpi(String name) { return new MemoryPreferences(this, name); }

        @Override
        protected void syncSpi() {}

        @Override
        protected void flushSpi() {}
    }
}
