package com.so2.cheat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(60, 120, 60, 60);

        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setText("SO2Cheat\n\n"
                + "1) Дай разрешение оверлея\n"
                + "2) Запусти сервис\n"
                + "3) Открой игру\n\n"
                + "Меню чита — шестерёнка в левом верхнем углу поверх игры.\n\n"
                + "Root: " + (FridaLauncher.hasRoot() ? "ДА" : "НЕТ"));
        ll.addView(tv);

        Button perm = new Button(this);
        perm.setText("Разрешение оверлея");
        perm.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        });
        ll.addView(perm);

        Button start = new Button(this);
        start.setText("Запустить сервис");
        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                return;
            }
            startForegroundService(new Intent(this, OverlayService.class));
            finish();
        });
        ll.addView(start);

        Button stopSvc = new Button(this);
        stopSvc.setText("Остановить сервис");
        stopSvc.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        ll.addView(stopSvc);

        setContentView(ll);
    }
}
