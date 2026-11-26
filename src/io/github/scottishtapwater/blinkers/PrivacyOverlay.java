package io.github.scottishtapwater.blinkers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

final class PrivacyOverlay {
    private static final String TAG = "Blinkers";
    private final Context context;
    private final WindowManager windows;
    private final Consumer<Throwable> onFailure;
    private View overlay;
    private boolean protectionRequested;
    private boolean applied;
    private long lastAppliedUptime;

    PrivacyOverlay(Context context, WindowManager windows, Consumer<Throwable> onFailure) {
        this.context = context;
        this.windows = windows;
        this.onFailure = onFailure;
    }

    boolean isApplied() {
        return applied;
    }

    long getLastAppliedUptime() {
        return lastAppliedUptime;
    }

    void prepare() {
        if (overlay != null) return;
        View target = new View(context);
        target.setBackgroundColor(Color.TRANSPARENT);
        target.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        target.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right > left && bottom > top && protectionRequested) {
                target.post(() -> applyPrivacy(target));
            }
        });
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.setFitInsetsTypes(0);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setTitle(context.getString(R.string.app_name));
        overlay = target;
        try {
            windows.addView(target, params);
        } catch (RuntimeException error) {
            onFailure.accept(error);
        }
    }

    private void applyPrivacy(View target) {
        if (!protectionRequested || overlay != target || !target.isAttachedToWindow()
                || target.getWidth() == 0 || target.getHeight() == 0) return;
        try {
            // These undocumented Samsung methods are verified on the S26 Ultra's One UI 9 firmware.
            View.class.getMethod("semSetPrivacyDisplayView", float.class).invoke(target, 0f);
            View.class.getMethod("semSetPrivacyDisplayViewPosition", int.class, int.class,
                int.class, int.class).invoke(target, 0, 0, target.getWidth(), target.getHeight());
            target.invalidate();
            applied = true;
            lastAppliedUptime = SystemClock.uptimeMillis();
            Log.i(TAG, "Privacy applied to " + target.getWidth() + "x" + target.getHeight());
        } catch (ReflectiveOperationException | RuntimeException error) {
            onFailure.accept(error instanceof InvocationTargetException
                ? ((InvocationTargetException) error).getTargetException() : error);
        }
    }

    void setProtected(boolean enabled) {
        protectionRequested = enabled;
        if (enabled) {
            if (overlay != null && !applied) applyPrivacy(overlay);
        } else if (applied && overlay != null) {
            try {
                View.class.getMethod("semDisablePrivacyDisplayView").invoke(overlay);
                overlay.invalidate();
            } catch (ReflectiveOperationException | RuntimeException error) {
                Log.e(TAG, "Could not disable privacy", error);
                remove();
            } finally {
                applied = false;
            }
            Log.i(TAG, "Privacy disabled");
        }
    }

    void remove() {
        View target = overlay;
        overlay = null;
        protectionRequested = false;
        applied = false;
        if (target == null || !target.isAttachedToWindow()) return;
        try {
            View.class.getMethod("semDisablePrivacyDisplayView").invoke(target);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.e(TAG, "Could not disable privacy before removing the overlay", error);
        } finally {
            windows.removeView(target);
        }
        Log.i(TAG, "Privacy overlay removed");
    }
}
