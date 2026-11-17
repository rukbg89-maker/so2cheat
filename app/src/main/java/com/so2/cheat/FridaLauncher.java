package com.so2.cheat;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FridaLauncher {
    private static final String TAG = "FridaLauncher";
    public static final String GAME_PKG = "com.axlebolt.standoff2";

    public interface Result {
        void ok();
        void err(String reason);
    }

    private static Process serverProc;
    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            return line != null && line.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    public static void start(Context ctx, Result cb) {
        new Thread(() -> {
            if (!hasRoot()) {
                cb.err("нет рута (нужен Magisk)");
                return;
            }
            try {
                File server  = extractAsset(ctx, "frida-server");
                File inject  = extractAsset(ctx, "frida-inject");
                File script  = extractAsset(ctx, "cheat.js");
                if (server == null || inject == null || script == null) {
                    cb.err("нет frida-файлов в assets");
                    return;
                }
                server.setExecutable(true, false);
                inject.setExecutable(true, false);

                if (serverProc == null || !serverProc.isAlive()) {
                    serverProc = new ProcessBuilder("su", "-c", server.getAbsolutePath() + " -l 127.0.0.1:27042").redirectErrorStream(true).start();
                    Thread.sleep(1500);
                }

                run("su -c 'am force-stop " + GAME_PKG + "'");
                Thread.sleep(400);
                run("su -c 'monkey -p " + GAME_PKG + " -c android.intent.category.LAUNCHER 1'");
                Thread.sleep(9000);

                Process ip = new ProcessBuilder("su", "-c", inject.getAbsolutePath() + " -H 127.0.0.1:27042 -n " + GAME_PKG + " -s " + script.getAbsolutePath()).redirectErrorStream(true).start();
                BufferedReader r = new BufferedReader(new InputStreamReader(ip.getInputStream()));
                String line;
                while ((line = r.readLine()) != null) Log.i(TAG, "inject: " + line);

                if (ip.waitFor() == 0) {
                    running.set(true);
                    cb.ok();
                } else {
                    cb.err("ошибка инжекта");
                }
            } catch (Exception e) {
                cb.err(e.getMessage() == null ? "unknown" : e.getMessage());
            }
        }).start();
    }

    public static void stop(Context ctx) {
        running.set(false);
        if (serverProc != null) { serverProc.destroy(); serverProc = null; }
        run("su -c 'killall frida-server'");
        run("su -c 'killall frida-inject'");
        run("su -c 'am force-stop " + GAME_PKG + "'");
    }

    private static File extractAsset(Context ctx, String name) {
        try {
            File out = new File(ctx.getFilesDir(), name);
            if (out.exists() && out.length() > 0) return out;
            try (InputStream in = ctx.getAssets().open(name); OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    private static void run(String cmd) {
        try { Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd}).waitFor(); } catch (Exception ignored) {}
    }
}
