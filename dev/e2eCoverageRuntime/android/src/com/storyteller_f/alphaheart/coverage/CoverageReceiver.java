/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.coverage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

/** Compiled only into coverage debug APKs; only the privileged shell can request a snapshot. */
public final class CoverageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            throw new IllegalStateException("Coverage capture requires a debuggable test product");
        }
        try {
            File directory = new File(context.getFilesDir(), "e2e-coverage");
            Files.createDirectories(directory.toPath());
            CoverageRuntime.saveReport(new File(directory, "report.ic"));
            try (var descriptor = new BufferedInputStream(context.getAssets().open("e2e-coverage.properties"))) {
                Files.write(new File(directory, "classes.properties").toPath(), descriptor.readAllBytes());
            }
            setResultCode(-1);
            setResultData(Integer.toString(Process.myPid()));
        } catch (Exception failure) {
            throw new IllegalStateException("Android coverage snapshot failed", failure);
        }
    }
}
