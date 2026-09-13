/*
 * Adapted from DuoFold-Android FoldService
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: accessibility overlay + Shizuku capture session, Spanish status.
 */
package com.soyache.blurgiro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import com.soyache.blurgiro.bridge.BridgeClient;
import com.soyache.blurgiro.bridge.ICaptureListener;
import com.soyache.blurgiro.bridge.ICrystalBridge;
import com.soyache.blurgiro.motion.EffectStyle;
import com.soyache.blurgiro.motion.FoldModel;
import com.soyache.blurgiro.render.CrystalView;
import com.soyache.blurgiro.sensor.TiltSensor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class CrystalService extends AccessibilityService {
    public static final String PREFS="cristalgiro";
    private static volatile CrystalService instance;
    public static CrystalService current() { return instance; }
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ExecutorService touches=Executors.newSingleThreadExecutor();
    private final AtomicInteger queuedTouches=new AtomicInteger();
    private BridgeClient client;
    private CrystalView view;
    private TiltSensor sensor;
    private WindowManager wm;
    private boolean active,captureReady,intercepting;
    private volatile int generation;
    private long lastFrame,started,trialEnd,frames;
    private float progress=1,intensity=1;
    private String status="Aún no está activo";
    private final Runnable bridgeChanged=()-> {
        if(active && client.service()==null) stopSession("Shizuku se desconectó. Vuelve a iniciarlo y autoriza.");
    };
    private final BroadcastReceiver screenOff=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) { stopSession("Pantalla apagada"); }
    };
    @Override protected void onServiceConnected() {
        instance=this;wm=getSystemService(WindowManager.class);client=BridgeClient.get(this);
        client.addObserver(bridgeChanged);client.connect();setIntercept(false);
        if(Build.VERSION.SDK_INT>=33)
            registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
    public boolean isActive() { return active; }
    public String status() { return status; }
    public float progress() { return progress; }
    public long frames() { return frames; }
    public String diagnostics() { return "active="+active+" ready="+captureReady+" intercept="+intercepting
            +" size="+(view==null?"none":view.getWidth()+"x"+view.getHeight())+" progress="+progress
            +" frames="+frames+" touches="+touchCount+" status="+status; }
    private long touchCount;
    private boolean discardGesture;
    private boolean debugTilt;
    public void testAngle(float degrees) {
        testAxes(0,degrees);
    }
    public void testAxes(float forwardDegrees,float sideDegrees) {
        if(BuildConfig.DEBUG && view!=null) {debugTilt=true;view.setFold(
                (float)Math.toRadians(forwardDegrees),(float)Math.toRadians(sideDegrees),intensity);}
    }
    public void saveDebugFrame() {if(BuildConfig.DEBUG && view!=null)view.saveDebugFrame();}
    public void setEffectStyle(EffectStyle style) {if(view!=null)view.setEffectStyle(style);}
    public void calibrate() { if(sensor!=null) {sensor.calibrate();progress=1;view.setFold(0,0,intensity);} }
    public void startSession(boolean trial) {
        if(active) return;
        ICrystalBridge bridge=client.service();
        if(bridge==null) {status=SetupMessages.needShizuku();Toast.makeText(this,status,Toast.LENGTH_LONG).show();return;}
        if(getSystemService(android.view.accessibility.AccessibilityManager.class).isTouchExplorationEnabled()) {
            status="Hay un servicio de exploración táctil activo. Apágalo para evitar conflictos.";
            Toast.makeText(this,status,Toast.LENGTH_LONG).show();return;
        }
        active=true;captureReady=false;generation++;lastFrame=0;frames=0;
        debugTilt=false;
        touchCount=0;
        started=SystemClock.uptimeMillis();trialEnd=trial?started+10000:0;
        intensity=getSharedPreferences(PREFS,0).getFloat("intensity",1);
        float range=getSharedPreferences(PREFS,0).getFloat("range",80);
        boolean includeZ=getSharedPreferences(PREFS,0).getBoolean("zAxis",false);
        status="Obteniendo la pantalla real (sin diálogo de grabar)";
        try {
            addOverlay();
            sensor=new TiltSensor(this,range,includeZ,(p,forwardDegrees,sideDegrees)-> {
                progress=p;
                if(view!=null && active && !debugTilt) view.setFold(
                        (float)Math.toRadians(forwardDegrees),(float)Math.toRadians(sideDegrees),intensity);
            });
            if(!sensor.start()) throw new IllegalStateException("No hay sensor de actitud disponible");
            showNotification();main.post(watchdog);
        } catch(Exception e) {stopSession(SetupMessages.captureFailed(e.toString()));}
    }
    private void addOverlay() {
        int overlayGeneration=generation;
        view=new CrystalView(this,new CrystalView.Listener() {
            @Override public void onReady(Surface surface,int w,int h) {
                if(active && overlayGeneration==generation) beginCapture(surface,w,h);
            }
            @Override public void onFirstFrame() {
                if(Build.VERSION.SDK_INT<31 && active && overlayGeneration==generation && !captureReady) {
                    captureReady=true;
                    if(view!=null)view.setAlpha(1f);
                    status=runningStatus();
                }
            }
            @Override public void onError(String message) {
                if(overlayGeneration==generation) stopSession(SetupMessages.captureFailed(message));
            }
        });
        view.setEffectStyle(EffectStyle.fromId(getSharedPreferences(PREFS,0).getString("effectStyle","classic")));
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.START;lp.setTitle("CristalGiro optical layer");
        lp.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30
                ?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                :WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        if(Build.VERSION.SDK_INT>=30)lp.setFitInsetsTypes(0);
        if(Build.VERSION.SDK_INT<31)view.setAlpha(0f);
        wm.addView(view,lp);
    }
    private void beginCapture(Surface surface,int w,int h) {
        if(!active || view==null) return;
        setIntercept(false);captureReady=false;started=SystemClock.uptimeMillis();lastFrame=0;
        SurfaceControl exclude=view.getSurfaceControl();
        if(exclude==null || !exclude.isValid()) {stopSession("No pude obtener la capa de efecto; cancelo para evitar recursión.");return;}
        Bundle options=new Bundle();options.putParcelable("exclude",exclude);
        options.putInt("width",w);options.putInt("height",h);options.putInt("fps",30);
        options.putInt("displayWidth",view.getWidth());options.putInt("displayHeight",view.getHeight());
        options.putInt("rotation",view.getDisplay()==null?Surface.ROTATION_0:view.getDisplay().getRotation());
        int session=generation;
        io.execute(()-> {
            try {
                ICrystalBridge bridge=client.service();
                if(bridge==null || session!=generation) return;
                bridge.start(surface,options,new ICaptureListener.Stub() {
                    @Override public void onFrame(long sequence,long timestampMs) {
                        main.post(()-> {
                            if(!active || session!=generation) return;
                            lastFrame=SystemClock.uptimeMillis();frames=sequence;
                            if(Build.VERSION.SDK_INT>=31 && !captureReady) {
                                captureReady=true;
                                setIntercept(true);status=runningStatus();
                            }
                        });
                    }
                    @Override public void onError(String message) {main.post(()->{if(session==generation) stopSession(SetupMessages.captureFailed(message));});}
                });
            } catch(Exception e) {main.post(()->{if(session==generation) stopSession(SetupMessages.captureFailed(e.toString()));});}
        });
    }
    private final Runnable watchdog=new Runnable() {
        @Override public void run() {
            if(!active) return;
            long now=SystemClock.uptimeMillis();
            if(trialEnd>0 && now>=trialEnd) {stopSession("Fin de la prueba de 10 s. Pantalla original restaurada.");return;}
            boolean liveCapture=Build.VERSION.SDK_INT>=31;
            if((liveCapture && lastFrame>0 && now-lastFrame>1500) || (lastFrame==0 && now-started>5000)) {
                stopSession(SetupMessages.captureFailed("La captura dejó de actualizarse."));return;
            }
            ICrystalBridge bridge=client.service();
            if(bridge==null) {stopSession("Shizuku se desconectó. Inícialo y autoriza de nuevo.");return;}
            io.execute(()->{try{bridge.heartbeat();}catch(Exception e){main.post(()->stopSession("Se cortó el latido de Shizuku."));}});
            main.postDelayed(this,500);
        }
    };
    @Override public void onMotionEvent(MotionEvent event) {
        touchCount++;
        if(!active || !captureReady || view==null) return;
        if(event.getPointerCount()>=3) {stopSession("Saliste con tres dedos. Pantalla original restaurada.");return;}
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN) discardGesture=false;
        int n=event.getPointerCount();
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[n];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[n];
        FoldModel.State state=view.inputState();
        float width=Math.max(1,view.getWidth()),height=Math.max(1,view.getHeight());
        for(int i=0;i<n;i++) {
            props[i]=new MotionEvent.PointerProperties();event.getPointerProperties(i,props[i]);
            coords[i]=new MotionEvent.PointerCoords();event.getPointerCoords(i,coords[i]);
            float[] uv=FoldModel.sourceAt(coords[i].x/width,coords[i].y/height,state);
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN &&
                    (uv[0]<0 || uv[0]>1 || uv[1]<0 || uv[1]>1)) {
                discardGesture=true;return;
            }
            coords[i].x=FoldModel.clamp(uv[0])*width;coords[i].y=FoldModel.clamp(uv[1])*height;
        }
        MotionEvent mapped=MotionEvent.obtain(event.getDownTime(),event.getEventTime(),event.getAction(),n,props,coords,
                event.getMetaState(),event.getButtonState(),event.getXPrecision(),event.getYPrecision(),
                event.getDeviceId(),event.getEdgeFlags(),InputDevice.SOURCE_TOUCHSCREEN,event.getFlags());
        if(discardGesture) {mapped.recycle();return;}
        if(queuedTouches.incrementAndGet()>16) {
            queuedTouches.decrementAndGet();mapped.recycle();stopSession("La cola táctil se saturó. Pantalla original restaurada.");return;
        }
        int session=generation;
        touches.execute(()-> {
            try {
                ICrystalBridge bridge=client.service();
                if(session==generation && (bridge==null || !bridge.inject(mapped)))
                    main.post(()->{if(session==generation) stopSession("El sistema rechazó el toque. En algunos Xiaomi abre Depuración USB (ajustes de seguridad).");});
            } catch(Exception e) {main.post(()->{if(session==generation)stopSession("Se cortó la inyección táctil.");});}
            finally {mapped.recycle();queuedTouches.decrementAndGet();}
        });
    }
    private void setIntercept(boolean enabled) {
        if(Build.VERSION.SDK_INT<34) {intercepting=false;return;}
        AccessibilityServiceInfo info=getServiceInfo();
        if(info==null) return;
        info.setMotionEventSources(enabled?InputDevice.SOURCE_TOUCHSCREEN:0);
        setServiceInfo(info);intercepting=enabled;
    }
    public void stopSession(String reason) {
        setIntercept(false);active=false;captureReady=false;generation++;status=reason;
        android.util.Log.i("CristalGiro", "Stopped: "+reason+"; frames="+frames+" touches="+touchCount);
        main.removeCallbacks(watchdog);
        if(sensor!=null) {sensor.stop();sensor=null;}
        if(view!=null) {
            CrystalView old=view;view=null;
            old.releaseResources();old.onPause();
            try {wm.removeViewImmediate(old);}catch(Exception ignored){}
        }
        ICrystalBridge bridge=client==null?null:client.service();
        if(bridge!=null) io.execute(()->{try{bridge.stop();}catch(Exception ignored){}});
        getSystemService(NotificationManager.class).cancel(81);
        getSharedPreferences(PREFS,0).edit().putString("lastStatus",reason).apply();
    }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if(active) {
            setIntercept(false);captureReady=false;lastFrame=0;started=SystemClock.uptimeMillis();generation++;
            if(sensor!=null)sensor.calibrate();
            CrystalView old=view;view=null;
            if(old!=null) {
                old.releaseResources();old.onPause();
                try {wm.removeViewImmediate(old);}catch(Exception ignored){}
            }
            ICrystalBridge bridge=client==null?null:client.service();
            if(bridge!=null)io.execute(()->{try{bridge.stop();}catch(Exception ignored){}});
            try {addOverlay();status="Cambió la orientación; recalibrando";}
            catch(Exception e){stopSession("Falló el cambio de orientación: "+e.getMessage());}
        }
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {stopSession("El servicio de accesibilidad se interrumpió");}
    @Override public boolean onUnbind(Intent intent) {stopSession("Se apagó el servicio de accesibilidad");return super.onUnbind(intent);}
    @Override public void onDestroy() {
        stopSession("Servicio detenido");instance=null;
        if(client!=null)client.removeObserver(bridgeChanged);
        try{unregisterReceiver(screenOff);}catch(Exception ignored){}
        io.shutdown();touches.shutdown();super.onDestroy();
    }
    private void showNotification() {
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("cristal_live","Efecto CristalGiro",NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getBroadcast(this,1,new Intent(this,StopReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,"cristal_live").setSmallIcon(R.drawable.ic_crystal)
                .setContentTitle("CristalGiro está activo").setContentText(Build.VERSION.SDK_INT>=34
                        ?"Inclina el teléfono · tres dedos para salir":"Inclina el teléfono · usa Detener en la notificación")
                .setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"Detener",stop).build()).build();
        nm.notify(81,notification);
    }
    private String runningStatus() {
        return Build.VERSION.SDK_INT>=34?"En marcha · tres dedos a la vez para salir":"En marcha · usa el botón de la notificación para salir";
    }
}
