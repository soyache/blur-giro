package com.soyache.blurgiro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class StopReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        CrystalService service=CrystalService.current();
        if(service!=null) service.stopSession("Lo detuviste a mano");
    }
}
