package io.github.scottishtapwater.blinkers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    private Button enable;
    private Button disable;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding * 2, padding, padding);
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        layout.addView(title);
        TextView instructions = new TextView(this);
        instructions.setText(R.string.instructions);
        instructions.setTextSize(18);
        instructions.setPadding(0, padding, 0, padding);
        layout.addView(instructions);
        addButton(layout, R.string.accessibility_settings,
            () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        enable = addButton(layout, R.string.enable_protection, () -> setProtection(true));
        disable = addButton(layout, R.string.disable_protection, () -> setProtection(false));
        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, padding, 0, 0);
        layout.addView(status);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private Button addButton(LinearLayout layout, int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        layout.addView(button);
        return button;
    }

    private void setProtection(boolean enabled) {
        PrivacyService service = PrivacyService.getConnectedService();
        if (service != null) service.setAutomaticEnabled(enabled);
        refreshStatus();
    }

    @Override public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && status != null) refreshStatus();
    }

    private void refreshStatus() {
        PrivacyService service = PrivacyService.getConnectedService();
        status.setText(service == null ? getString(R.string.status_service_disabled) : service.getStatus());
        boolean enabled = service != null && service.isAutomaticEnabled();
        enable.setEnabled(service != null && !enabled);
        disable.setEnabled(enabled);
    }
}
