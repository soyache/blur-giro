package com.soyache.blurgiro;

import android.app.Application;
import com.soyache.blurgiro.bridge.BridgeClient;

public final class CristalGiroApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        BridgeClient.get(this);
    }
}
