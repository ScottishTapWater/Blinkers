package io.github.scottishtapwater.blinkers;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

final class DrawerDetector {
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String DRAWER_ROOT_ID = SYSTEM_UI_PACKAGE + ":id/legacy_window_root";
    private int drawerWindowId = -1;

    boolean isOpen(List<AccessibilityWindowInfo> windows, int screenHeight, boolean locked) {
        int detectedWindowId = -1;
        if (!locked) {
            for (AccessibilityWindowInfo window : windows) {
                if (window.getType() != AccessibilityWindowInfo.TYPE_SYSTEM) continue;
                Rect bounds = new Rect();
                window.getBoundsInScreen(bounds);
                if (bounds.height() <= screenHeight / 2) continue;

                // Keep tracking the window while its root disappears during the closing animation.
                if (window.getId() == drawerWindowId || hasDrawerRoot(window)) {
                    detectedWindowId = window.getId();
                    break;
                }
            }
        }
        drawerWindowId = detectedWindowId;
        return drawerWindowId != -1;
    }

    private boolean hasDrawerRoot(AccessibilityWindowInfo window) {
        AccessibilityNodeInfo root = window.getRoot();
        return root != null
            && SYSTEM_UI_PACKAGE.contentEquals(root.getPackageName() == null ? "" : root.getPackageName())
            && DRAWER_ROOT_ID.equals(root.getViewIdResourceName());
    }
}
