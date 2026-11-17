package com.so2.cheat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class OverlayService extends Service implements OverlayView.Callback {
    private WindowManager wm;
    private OverlayView view;
    private WindowManager.LayoutParams lp;
    private volatile boolean running = true;
    private static final int PORT = 13338;
    private volatile PrintWriter toScript;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, makeNotification());

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new OverlayView(this);
        view.setCallback(this);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;

        try { wm.addView(view, lp); } catch (Exception e) { stopSelf(); return; }

        new Thread(this::socketServer).start();
        new Thread(this::menuWatcher).start();
    }

    private void socketServer() {
        try (ServerSocket ss = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"))) {
            while (running) {
                Socket s = ss.accept();
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter w = new PrintWriter(s.getOutputStream(), true);
                toScript = w;
                view.setConnected(true);
                String line;
                while ((line = r.readLine()) != null) view.setPayload(line);
                view.setConnected(false);
                toScript = null;
            }
        } catch (IOException ignored) {}
    }

    private void menuWatcher() {
        boolean last = false;
        while (running) {
            boolean cur = view.isMenuOpen();
            if (cur != last) {
                lp.flags = cur ? (lp.flags & ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) : (lp.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
                last = cur;
            }
            try { Thread.sleep(80); } catch (InterruptedException e) { break; }
        }
    }

    private Notification makeNotification() {
        String ch = "so2";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(ch) == null) nm.createNotificationChannel(new NotificationChannel(ch, "SO2Cheat", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        return b.setContentTitle("SO2Cheat").setContentText("Оверлей работает").setSmallIcon(android.R.drawable.ic_menu_view).build();
    }

    @Override public void onConfigChanged(String json) {
        PrintWriter w = toScript; if (w != null) try { w.println(json); } catch (Exception ignored) {}
    }
    @Override public void onStartCheat() {
        view.setStatus("Запуск чита...");
        FridaLauncher.start(this, new FridaLauncher.Result() {
            @Override public void ok() { view.setStatus("OK: чит запущен"); }
            @Override public void err(String reason) { view.setStatus("Ошибка: " + reason); }
        });
    }
    @Override public void onStopCheat() { FridaLauncher.stop(this); view.setStatus("Чит остановлен"); }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() {
        running = false; FridaLauncher.stop(this);
        if (view != null && wm != null) try { wm.removeView(view); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
