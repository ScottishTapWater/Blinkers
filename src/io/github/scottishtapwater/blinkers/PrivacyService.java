package io.github.scottishtapwater.blinkers;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public final class PrivacyService extends AccessibilityService {
    private static final String TAG = "Blinkers";
    private static final String PREFERENCES = "settings";
    private static final String AUTOMATIC_ENABLED = "automatic";
    private static PrivacyService connectedService;
    private final DrawerDetector drawerDetector = new DrawerDetector();
    private SharedPreferences preferences;
    private WindowManager windows;
    private KeyguardManager keyguard;
    private PrivacyOverlay overlay;
    private boolean automaticEnabled;
    private boolean drawerOpen;
    private String activationError;
    private long lastDetectionUptime;
    private long lastDetectionDuration;
    private long lastDetectionEventAge;

    static PrivacyService getConnectedService() {
        return connectedService;
    }

    @Override protected void onServiceConnected() {
        windows = getSystemService(WindowManager.class);
        keyguard = getSystemService(KeyguardManager.class);
        overlay = new PrivacyOverlay(this, windows, this::onPrivacyFailed);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        automaticEnabled = preferences.getBoolean(AUTOMATIC_ENABLED, false);
        connectedService = this;
        updateProtection();
    }

    void setAutomaticEnabled(boolean enabled) {
        automaticEnabled = enabled;
        activationError = null;
        preferences.edit().putBoolean(AUTOMATIC_ENABLED, enabled).apply();
        updateProtection();
    }

    private void updateProtection() {
        updateProtection(SystemClock.uptimeMillis());
    }

    private void updateProtection(long eventTime) {
        long started = SystemClock.uptimeMillis();
        boolean open = drawerDetector.isOpen(getWindows(),
            windows.getCurrentWindowMetrics().getBounds().height(), keyguard.isKeyguardLocked());
        if (open && !drawerOpen) {
            lastDetectionUptime = SystemClock.uptimeMillis();
            lastDetectionDuration = lastDetectionUptime - started;
            lastDetectionEventAge = started - eventTime;
        }
        if (open != drawerOpen) Log.i(TAG, "Drawer open=" + open);
        drawerOpen = open;
        if (automaticEnabled) {
            overlay.prepare();
            overlay.setProtected(automaticEnabled && drawerOpen);
        } else {
            overlay.remove();
        }
    }

    String getStatus() {
        if (activationError != null) return getString(R.string.status_failed, activationError);
        if (!automaticEnabled) return getString(R.string.status_disabled);
        if (!drawerOpen) return getString(R.string.status_waiting);
        return getString(overlay.isApplied() ? R.string.status_active : R.string.status_activating);
    }

    boolean isAutomaticEnabled() {
        return automaticEnabled;
    }

    private void onPrivacyFailed(Throwable error) {
        Log.e(TAG, "Privacy activation failed", error);
        overlay.remove();
        automaticEnabled = false;
        preferences.edit().putBoolean(AUTOMATIC_ENABLED, false).apply();
        activationError = error.toString();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (overlay != null) updateProtection(event.getEventTime());
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (overlay != null) updateProtection();
    }

    @Override public void onInterrupt() { }

    @Override public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void disconnect() {
        connectedService = null;
        if (overlay != null) {
            overlay.remove();
            overlay = null;
        }
    }

    @Override protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] args) {
        writer.println(getStatus());
        writer.println("Automatic=" + automaticEnabled + " drawerOpen=" + drawerOpen
            + " privacyApplied=" + (overlay != null && overlay.isApplied()));
        writer.println("Last opening: detectedAt=" + lastDetectionUptime
            + " scanMs=" + lastDetectionDuration + " eventAgeMs=" + lastDetectionEventAge);
        if (overlay != null) writer.println("Last privacy API application=" + overlay.getLastAppliedUptime());
    }
}
