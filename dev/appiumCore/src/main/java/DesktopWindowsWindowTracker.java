/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium;

import io.appium.java_client.AppiumDriver;
import java.util.List;
import java.util.Map;

/** Uses guest-side window discovery and mutations, including for remote Appium. */
public final class DesktopWindowsWindowTracker {
    private final AppiumDriver driver;
    private final long processId;
    private final String mainHandle;

    public DesktopWindowsWindowTracker(AppiumDriver driver, long processId) {
        this.driver = driver;
        this.processId = processId;
        mainHandle = driver.getWindowHandle();
        inspect(List.of(mainHandle));
    }

    public void synchronizeWindow() {
        // Tao's visible message-dispatch HWND has no application content or input surface.
        List<Map<String, Object>> windows = windows().stream()
                .filter(window -> !"Tao Thread Event Target".equals(window.get("className"))).toList();
        Map<String, Object> state = inspect(windows.stream().map(window -> (String) window.get("handle")).toList());
        List<?> owned = (List<?>) state.get("ownedHandles");
        String foreground = (String) state.get("foregroundHandle");
        String target = foreground != null ? foreground : windows.stream()
                .filter(window -> !mainHandle.equals(window.get("handle")))
                .map(window -> (String) window.get("handle"))
                .filter(owned::contains)
                .findFirst().orElse(mainHandle);
        if (!owned.contains(target)) throw new IllegalStateException("Owned test window is no longer visible");
        if (foreground == null || !target.equals(driver.getWindowHandle())) driver.switchTo().window(target);
    }

    /** Resize the owned native window on the Appium host using its actual DPI. */
    public void resizeWindow(int width, int height) {
        driver.executeScript("desktop: resizeWindow", Map.of(
                "processId", processId, "handle", mainHandle, "width", width, "height", height));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> windows() {
        return (List<Map<String, Object>>) driver.executeScript("windows: getWindows");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> inspect(List<String> handles) {
        return (Map<String, Object>) driver.executeScript("desktop: inspectWindows", Map.of(
                "processId", processId, "mainHandle", mainHandle, "handles", handles));
    }
}
