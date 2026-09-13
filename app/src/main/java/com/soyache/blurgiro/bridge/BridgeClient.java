/*
 * Adapted from DuoFold-Android BridgeClient
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: Shizuku UserService client with Spanish status strings.
 */
package com.soyache.blurgiro.bridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;

public final class BridgeClient {
    private static BridgeClient instance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final CopyOnWriteArrayList<Runnable> observers=new CopyOnWriteArrayList<>();
    private final Shizuku.UserServiceArgs args;
    private volatile ICrystalBridge bridge;
    private volatile String message="Esperando Shizuku";
    private boolean binding;
    private BridgeClient(Context context) {
        args=new Shizuku.UserServiceArgs(new ComponentName(context,CaptureBridge.class))
                .daemon(false).processNameSuffix("capture").debuggable(true).version(1);
        Shizuku.addBinderReceivedListenerSticky(()->main.post(this::connect));
        Shizuku.addBinderDeadListener(()->main.post(()-> {
            bridge=null; binding=false; message="Shizuku se desconectó"; notifyObservers();
        }));
        Shizuku.addRequestPermissionResultListener((code,result)-> {
            if(result==PackageManager.PERMISSION_GRANTED) connect();
            else { message="No obtuve autorización de Shizuku"; notifyObservers(); }
        });
    }
    public static synchronized BridgeClient get(Context context) {
        if(instance==null) instance=new BridgeClient(context.getApplicationContext()); return instance;
    }
    public ICrystalBridge service() { return bridge; }
    public String message() { return message; }
    public void addObserver(Runnable observer) { observers.add(observer); }
    public void removeObserver(Runnable observer) { observers.remove(observer); }
    public void request() {
        try {
            if(!Shizuku.pingBinder()) {message="Primero inicia Shizuku";notifyObservers();return;}
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(41);
            else connect();
        } catch(Exception e) {message=e.toString();notifyObservers();}
    }
    public void connect() {
        try {
            if(bridge!=null || binding || !Shizuku.pingBinder()) return;
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) {
                message="Shizuku está en marcha; falta autorizar";notifyObservers();return;
            }
            binding=true; message="Conectando el servicio de captura";notifyObservers();
            Shizuku.bindUserService(args,selfConnection);
        } catch(Exception e) {binding=false;message=e.toString();notifyObservers();}
    }
    private final ServiceConnection selfConnection=new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name,IBinder binder) {
            ICrystalBridge candidate=ICrystalBridge.Stub.asInterface(binder);
            worker.execute(()-> {
                try {
                    Bundle probe=candidate.probe();
                    boolean ready=probe.getBoolean("captureApi") && probe.getBoolean("inputApi");
                    main.post(()-> {binding=false;bridge=ready?candidate:null;
                        message=ready?"Shizuku listo · UID "+probe.getInt("uid")+" · "+probe.getString("backend")
                                :probe.getString("message");notifyObservers();});
                } catch(Exception e) {main.post(()->{binding=false;message=e.toString();notifyObservers();});}
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            bridge=null;binding=false;message="El servicio de captura se desconectó";notifyObservers();
        }
    };
    private void notifyObservers() { main.post(()->observers.forEach(Runnable::run)); }
}
