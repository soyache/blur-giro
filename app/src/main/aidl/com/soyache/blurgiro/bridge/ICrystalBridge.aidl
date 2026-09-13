package com.soyache.blurgiro.bridge;
import android.os.Bundle;
import android.view.Surface;
import android.view.MotionEvent;
import com.soyache.blurgiro.bridge.ICaptureListener;
interface ICrystalBridge {
    Bundle probe() = 0;
    void start(in Surface target, in Bundle options, ICaptureListener listener) = 1;
    boolean inject(in MotionEvent event) = 2;
    Bundle status() = 3;
    void heartbeat() = 4;
    void stop() = 5;
    void destroy() = 16777114;
}
