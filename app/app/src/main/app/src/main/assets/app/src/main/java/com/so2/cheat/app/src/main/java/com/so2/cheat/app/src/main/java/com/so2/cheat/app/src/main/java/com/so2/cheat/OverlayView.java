package com.so2.cheat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {
    public interface Callback {
        void onConfigChanged(String json);
        void onStartCheat();
        void onStopCheat();
    }

    private Callback cb;
    private final Paint box   = new Paint();
    private final Paint hpBar = new Paint();
    private final Paint bg    = new Paint();
    private final Paint txt   = new Paint();
    private final Paint btnOn = new Paint();
    private final Paint btnOff= new Paint();
    private final Paint gear  = new Paint();
    private final Paint gearB = new Paint();

    private volatile List<float[]> ents = new ArrayList<>();
    private volatile boolean connected = false;
    private volatile String status = "готов";
    private boolean menuOpen = false;

    private boolean esp = true;
    private boolean aim = false;
    private int aimBone = 0;
    private int aimFov = 200;
    private int aimSmooth = 6;

    private final List<RectF> btnRects = new ArrayList<>();
    private final List<String> btnIds = new ArrayList<>();
    private RectF gearRect = new RectF();

    public OverlayView(Context c) {
        super(c);
        box.setColor(0xFFFF3C3C); box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(4f); box.setAntiAlias(true);
        hpBar.setColor(0xFF3CDC3C); hpBar.setStyle(Paint.Style.FILL);
        bg.setColor(0xEE101018); bg.setStyle(Paint.Style.FILL);
        txt.setColor(Color.WHITE); txt.setTextSize(34f); txt.setAntiAlias(true);
        btnOn.setColor(0xFF2FBF58); btnOn.setStyle(Paint.Style.FILL);
        btnOff.setColor(0xFF7A2A2A); btnOff.setStyle(Paint.Style.FILL);
        gear.setColor(0xFFFFFFFF); gear.setStyle(Paint.Style.FILL);
        gearB.setColor(0xCC101018); gearB.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setCallback(Callback c) { cb = c; }
    public boolean isMenuOpen() { return menuOpen; }
    public void setConnected(boolean v) { connected = v; invalidate(); }
    public void setStatus(String s)    { status = s; invalidate(); }

    public void setPayload(String json) {
        List<float[]> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new float[]{(float)o.getDouble("x"), (float)o.getDouble("y"), (float)o.getDouble("w"), (float)o.getDouble("h"), (float)o.getDouble("hp")});
            }
        } catch (Exception ignored) {}
        ents = out;
        postInvalidateOnAnimation();
    }

    private void emitConfig() {
        if (cb == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("esp", esp); o.put("aim", aim); o.put("aimBone", aimBone); o.put("aimFov", aimFov); o.put("aimSmooth", aimSmooth);
            cb.onConfigChanged(o.toString());
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        for (float[] e : ents) {
            float x = e[0], y = e[1], w = e[2], h = e[3], hp = e[4];
            c.drawRect(x, y, x + w, y + h, box);
            float f = Math.max(0f, Math.min(1f, hp / 100f));
            c.drawRect(x - 8f, y + h * (1f - f), x - 4f, y + h, hpBar);
        }

        float gx = 60f, gy = 160f, gr = 52f;
        c.drawCircle(gx, gy, gr, gearB); c.drawCircle(gx, gy, gr * 0.55f, gear);
        gearRect.set(gx - gr, gy - gr, gx + gr, gy + gr);

        btnRects.clear(); btnIds.clear();
        if (menuOpen) {
            float mw = 760f, mh = 900f;
            float x0 = (getWidth() - mw) / 2f, y0 = (getHeight() - mh) / 2f;
            c.drawRoundRect(x0, y0, x0 + mw, y0 + mh, 28f, 28f, bg);
            float y = y0 + 70f;
            c.drawText("SO2 Cheat", x0 + 40f, y, txt); y += 60f;
            c.drawText("status: " + (connected ? "connected" : "no link"), x0 + 40f, y, txt); y += 30f;
            c.drawText("root: " + status, x0 + 40f, y, txt); y += 70f;
            y = toggle(c, x0, y, mw, "ESP: " + onoff(esp), esp, "esp");
            y = toggle(c, x0, y, mw, "Aim: " + onoff(aim), aim, "aim");
            y = toggle(c, x0, y, mw, "Bone: " + (aimBone == 0 ? "head" : "body"), aimBone == 0, "bone");
            y = toggle(c, x0, y, mw, "FOV: " + aimFov, true, "fov");
            y = toggle(c, x0, y, mw, "Smooth: " + aimSmooth, true, "smooth");
            y = toggle(c, x0, y, mw, "START", true, "start");
            y = toggle(c, x0, y, mw, "STOP",  false, "stop");
            y = toggle(c, x0, y, mw, "Close", true, "close");
        }
        invalidate();
    }

    private String onoff(boolean b) { return b ? "on" : "off"; }

    private float toggle(Canvas c, float x0, float y, float mw, String label, boolean on, String id) {
        float bh = 84f;
        RectF r = new RectF(x0 + 40f, y, x0 + mw - 40f, y + bh);
        c.drawRoundRect(r, 18f, 18f, on ? btnOn : btnOff);
        c.drawText(label, r.left + 30f, r.top + bh * 0.65f, txt);
        btnRects.add(r); btnIds.add(id);
        return y + bh + 18f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) return menuOpen;
        float x = ev.getX(), y = ev.getY();
        if (gearRect.contains(x, y)) { menuOpen = !menuOpen; invalidate(); return true; }
        if (!menuOpen) return false;
        for (int i = 0; i < btnRects.size(); i++) {
            if (btnRects.get(i).contains(x, y)) { handle(btnIds.get(i)); return true; }
        }
        return true;
    }

    private void handle(String id) {
        switch (id) {
            case "esp": esp = !esp; break;
            case "aim": aim = !aim; break;
            case "bone": aimBone = (aimBone + 1) % 2; break;
            case "fov": aimFov = (aimFov >= 600) ? 100 : aimFov + 100; break;
            case "smooth": aimSmooth = (aimSmooth >= 20) ? 2 : aimSmooth + 2; break;
            case "start": if (cb != null) cb.onStartCheat(); break;
            case "stop": if (cb != null) cb.onStopCheat(); break;
            case "close": menuOpen = false; break;
        }
        emitConfig();
        invalidate();
    }
}
