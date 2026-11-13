/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs the prebuilt desktop entry point with isolated state, PID and file logging. */
public final class DesktopTestLauncher {
    private DesktopTestLauncher() {}

    public static void main(String[] arguments) throws Throwable {
        System.setProperty("java.io.tmpdir", System.getProperty("appium.desktop.tmpdir"));
        Files.writeString(Path.of(System.getProperty("appium.desktop.pid.file")),
                Long.toString(ProcessHandle.current().pid()), StandardCharsets.UTF_8);
        try (PrintStream log = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(System.getProperty("appium.desktop.log"), true)),
                true, StandardCharsets.UTF_8)) {
            System.setOut(log);
            System.setErr(log);
            try {
                Class.forName(arguments[0]).getMethod("main", String[].class)
                        .invoke(null, (Object) new String[0]);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }
    }
}
