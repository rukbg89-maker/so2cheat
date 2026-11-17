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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class OverlayService extends Service implements OverlayView.Callback {
    private WindowManager wm;
    private OverlayView view;
    private View gearView;
    private WindowManager.LayoutParams lpView;
    private WindowManager.LayoutParams lpGear;
    private volatile boolean running = true;
    private static final int PORT = 13338;
    private volatile PrintWriter toScript;
    private boolean menuOpen = false;

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

        lpView = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lpView.gravity = Gravity.TOP | Gravity.LEFT;

        gearView = new View(this) {
            @Override protected void onDraw(android.graphics.Canvas c) {
                android.graphics.Paint p = new android.graphics.Paint();
                p.setColor(0xCC101018);
                p.setStyle(android.graphics.Paint.Style.FILL);
                p.setAntiAlias(true);
                c.drawCircle(50, 50, 50, p);
                p.setColor(0xFFFFFFFF);
                c.drawCircle(50, 50, 28, p);
            }
        };
        
        gearView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                menuOpen = !menuOpen;
                view.setMenuOpen(menuOpen);
                lpView.flags = menuOpen
                        ? (lpView.flags & ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        : (lpView.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                try { wm.updateViewLayout(view, lpView); } catch (Exception ignored) {}
            }
            return true;
        });

        lpGear = new WindowManager.LayoutParams(100, 100, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lpGear.gravity = Gravity.TOP | Gravity.LEFT;
        
        // СДВИГАЕМ КНОПКУ В БЕЗОПАСНУЮ ЗОНУ (вправо и ниже)
        lpGear.x = 200;
        lpGear.y = 400; 

        try {
            wm.addView(view, lpView);
            wm.addView(gearView, lpGear);
        } catch (Exception e) { stopSelf(); return; }

        new Thread(this::socketServer).start();
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
    @Override public void onMenuToggle(boolean open) { }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() {
        running = false; FridaLauncher.stop(this);
        if (view != null && wm != null) try { wm.removeView(view); } catch (Exception ignored) {}
        if (gearView != null && wm != null) try { wm.removeView(gearView); } catch (Exception ignored) {}
        super.onDestroy();
    }
                                                     }
