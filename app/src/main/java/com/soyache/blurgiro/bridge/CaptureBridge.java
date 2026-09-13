/*
 * Adapted from DuoFold-Android CaptureBridge
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: same Shizuku UserService capture backends
 * (IWindowManager / ScreenCapture / ScreenCaptureInternal / SurfaceControl / legacy),
 * Spanish user-facing errors, package com.soyache.blurgiro.
 */
package com.soyache.blurgiro.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Runs only inside the Shizuku shell UserService, never inside the app process. */
public final class CaptureBridge extends ICrystalBridge.Stub {
    private static final int NONE=0,IWM_CAPTURE=1,SCREEN_CAPTURE=2,SURFACE_CAPTURE=3,LEGACY_SCREENSHOT=4;
    private final HandlerThread thread=new HandlerThread("CristalCapture");
    private final Handler handler;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private volatile boolean running;
    private volatile long heartbeat;
    private volatile long frames;
    private volatile String error="";
    private Surface target;
    private SurfaceControl exclude;
    private Object captureArgs;
    private Object windowManager;
    private Object inputManager;
    private IBinder displayToken;
    private Method capture,createCaptureListener,getCaptureBuffer,legacyScreenshot,inject;
    private boolean argsExclude;
    private String iwmCaptureOwner="android.window.ScreenCapture";
    private MotionEvent lastTouch;
    private ICaptureListener listener;
    private int width,height,interval,rotation,backend;

    public CaptureBridge() {thread.start();handler=new Handler(thread.getLooper());}
    public CaptureBridge(Context context) {this();}

    @Override public Bundle probe() {
        Bundle result=new Bundle();result.putInt("uid",Process.myUid());
        try {
            initInput();
            Throwable failure=null;
            try {initIwmCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initScreenCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initSurfaceCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initLegacyScreenshot();} catch(Throwable e) {
                failure=append(failure,e);
                throw new IllegalStateException("Ninguna API de captura compatible: "+describeTree(failure),failure);
            }
            result.putBoolean("captureApi",true);result.putBoolean("inputApi",inject!=null);
            result.putString("backend",backendName());
            result.putString("message","APIs de shell listas: "+backendName());
        } catch(Throwable e) {result.putString("message",describe(e));}
        return result;
    }

    private void initInput() throws Exception {
        try {
            Class<?> im=Class.forName("android.hardware.input.InputManagerGlobal");
            inputManager=im.getMethod("getInstance").invoke(null);
            inject=im.getMethod("injectInputEvent",InputEvent.class,int.class);
        } catch(ClassNotFoundException|NoSuchMethodException e) {
            Class<?> im=Class.forName("android.hardware.input.InputManager");
            inputManager=im.getMethod("getInstance").invoke(null);
            inject=im.getMethod("injectInputEvent",InputEvent.class,int.class);
        }
    }

    private void initIwmCapture() throws Exception {
        IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class)
                .invoke(null,"window");
        windowManager=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class)
                .invoke(null,binder);
        Throwable failure=null;
        for(String owner:new String[]{"android.window.ScreenCapture","android.window.ScreenCaptureInternal"}) {
            try {
                Class<?> api=Class.forName(owner);
                Class<?> args=Class.forName(owner+"$CaptureArgs");
                Class<?> listenerType=Class.forName(owner+"$ScreenCaptureListener");
                capture=Class.forName("android.view.IWindowManager")
                        .getMethod("captureDisplay",int.class,args,listenerType);
                createCaptureListener=api.getMethod("createSyncCaptureListener");
                getCaptureBuffer=Class.forName(owner+"$SynchronousScreenCaptureListener").getMethod("getBuffer");
                iwmCaptureOwner=owner;backend=IWM_CAPTURE;return;
            } catch(Throwable e) {failure=append(failure,e);}
        }
        if(failure instanceof Exception)throw (Exception)failure;
        throw new IllegalStateException("Captura IWindowManager no disponible",failure);
    }

    private void initSurfaceCapture() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        Class<?> args=Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
        capture=surfaceControl.getMethod("captureDisplay",args);
        backend=SURFACE_CAPTURE;
    }

    private void initScreenCapture() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        Class<?> screenCapture=Class.forName("android.window.ScreenCapture");
        Class<?> args=Class.forName("android.window.ScreenCapture$DisplayCaptureArgs");
        capture=screenCapture.getMethod("captureDisplay",args);
        backend=SCREEN_CAPTURE;
    }

    private void initLegacyScreenshot() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        legacyScreenshot=surfaceControl.getMethod("screenshot",IBinder.class,Surface.class,Rect.class,
                int.class,int.class,boolean.class,int.class);
        backend=LEGACY_SCREENSHOT;
    }

    @Override public void start(Surface surface,Bundle options,ICaptureListener callback) {
        heartbeat=SystemClock.uptimeMillis();
        handler.post(()-> {
            release();
            try {
                Bundle probe=probe();
                if(!probe.getBoolean("captureApi")||!probe.getBoolean("inputApi"))
                    throw new IllegalStateException(probe.getString("message"));
                listener=callback;target=surface;
                exclude=(SurfaceControl)options.getParcelable("exclude");
                width=options.getInt("width");height=options.getInt("height");
                rotation=options.getInt("rotation",0);
                interval=1000/Math.max(10,Math.min(60,options.getInt("fps",30)));
                if(target==null||!target.isValid()||exclude==null||!exclude.isValid())
                    throw new IllegalStateException("Capa del efecto no disponible; no capturo para evitar recursión.");
                buildCaptureArgs(options);
                if(backend!=LEGACY_SCREENSHOT) {
                    try {markExcluded(exclude);}
                    catch(Throwable markFailure) {
                        if(!argsExclude)throw new IllegalStateException("Esta ROM no puede excluir la capa del efecto",markFailure);
                    }
                }
                frames=0;error="";running=true;handler.post(frame);
            } catch(Throwable e) {fail(e);}
        });
    }

    private void markExcluded(SurfaceControl layer) throws Exception {
        try(SurfaceControl.Transaction transaction=new SurfaceControl.Transaction()) {
            SurfaceControl.Transaction.class.getMethod("setSkipScreenshot",SurfaceControl.class,boolean.class)
                    .invoke(transaction,layer,true);
            transaction.apply();
        }
    }

    private void buildCaptureArgs(Bundle options) throws Exception {
        captureArgs=null;argsExclude=false;
        if(backend==IWM_CAPTURE) {
            Class<?> builderClass=Class.forName(iwmCaptureOwner+"$CaptureArgs$Builder");
            Object builder=builderClass.getConstructor().newInstance();
            float sx=width/(float)Math.max(1,options.getInt("displayWidth",width));
            float sy=height/(float)Math.max(1,options.getInt("displayHeight",height));
            builderClass.getMethod("setFrameScale",float.class,float.class).invoke(builder,sx,sy);
            builderClass.getMethod("setExcludeLayers",SurfaceControl[].class)
                    .invoke(builder,(Object)new SurfaceControl[]{exclude});
            argsExclude=true;
            configureContentPolicy(builderClass,builder);
            captureArgs=builderClass.getMethod("build").invoke(builder);
        } else if(backend==SCREEN_CAPTURE||backend==SURFACE_CAPTURE) {
            String owner=backend==SCREEN_CAPTURE?"android.window.ScreenCapture":"android.view.SurfaceControl";
            Class<?> builderClass=Class.forName(owner+"$DisplayCaptureArgs$Builder");
            Object builder=builderClass.getConstructor(IBinder.class).newInstance(displayToken);
            builderClass.getMethod("setSize",int.class,int.class).invoke(builder,width,height);
            try {
                builderClass.getMethod("setExcludeLayers",SurfaceControl[].class)
                        .invoke(builder,(Object)new SurfaceControl[]{exclude});
                argsExclude=true;
            } catch(NoSuchMethodException ignored) {}
            configureContentPolicy(builderClass,builder);
            captureArgs=builderClass.getMethod("build").invoke(builder);
        }
    }

    private static void configureContentPolicy(Class<?> builderClass,Object builder) throws Exception {
        try {
            builderClass.getMethod("setCaptureSecureLayers",boolean.class).invoke(builder,false);
            builderClass.getMethod("setAllowProtected",boolean.class).invoke(builder,false);
        } catch(NoSuchMethodException legacyApiMissing) {
            builderClass.getMethod("setSecureContentPolicy",int.class).invoke(builder,0);
            builderClass.getMethod("setProtectedContentPolicy",int.class).invoke(builder,0);
        }
    }

    private final Runnable frame=new Runnable() {
        @Override public void run() {
            if(!running)return;
            if(SystemClock.uptimeMillis()-heartbeat>3000) {
                fail(new IllegalStateException("El cliente dejó de enviar latidos."));return;
            }
            long started=SystemClock.uptimeMillis();Bitmap bitmap=null;HardwareBuffer buffer=null;
            try {
                if(exclude==null||!exclude.isValid())throw new IllegalStateException("Se perdió la capa excluida.");
                if(backend==LEGACY_SCREENSHOT) {
                    legacyScreenshot.invoke(null,displayToken,target,new Rect(),width,height,false,rotation);
                } else {
                    Object screenshot;
                    if(backend==IWM_CAPTURE) {
                        Object sync=createCaptureListener.invoke(null);
                        capture.invoke(windowManager,0,captureArgs,sync);
                        screenshot=getCaptureBuffer.invoke(sync);
                    } else screenshot=capture.invoke(null,captureArgs);
                    if(screenshot==null)throw new IllegalStateException("El sistema negó la captura o no devolvió búfer.");
                    Class<?> sc=screenshot.getClass();
                    buffer=(HardwareBuffer)sc.getMethod("getHardwareBuffer").invoke(screenshot);
                    if(buffer==null)throw new IllegalStateException("Búfer de captura vacío.");
                    boolean secure=(boolean)sc.getMethod("containsSecureLayers").invoke(screenshot);
                    if(secure||(buffer.getUsage()&HardwareBuffer.USAGE_PROTECTED_CONTENT)!=0)
                        throw new IllegalStateException("Contenido protegido (FLAG_SECURE): vuelvo a la pantalla original.");
                    ColorSpace color=(ColorSpace)sc.getMethod("getColorSpace").invoke(screenshot);
                    bitmap=Bitmap.wrapHardwareBuffer(buffer,color);
                    if(bitmap==null)throw new IllegalStateException("No pude envolver el búfer de captura.");
                    if(!running)return;
                    Canvas canvas=target.lockHardwareCanvas();
                    try {
                        canvas.drawColor(0,PorterDuff.Mode.CLEAR);
                        canvas.drawBitmap(bitmap,null,new Rect(0,0,width,height),paint);
                    } finally {target.unlockCanvasAndPost(canvas);}
                }
                frames++;if(listener!=null)listener.onFrame(frames,SystemClock.uptimeMillis());
                // Android 10/11 have no per-layer exclusion API. Freeze the clean frame
                // captured while the app overlay is transparent, then animate that texture.
                if(backend==LEGACY_SCREENSHOT)return;
            } catch(Throwable e) {fail(e);return;}
            finally {if(bitmap!=null)bitmap.recycle();if(buffer!=null)buffer.close();}
            if(running)handler.postDelayed(this,Math.max(1,interval-(SystemClock.uptimeMillis()-started)));
        }
    };

    @Override public synchronized boolean inject(MotionEvent event) {
        if(!running||inject==null||event==null)return false;
        try {
            boolean ok=(boolean)inject.invoke(inputManager,event,0);if(!ok)return false;
            if(lastTouch!=null)lastTouch.recycle();
            lastTouch=(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)
                    ?null:MotionEvent.obtain(event);return true;
        } catch(Throwable e) {error=describe(e);return false;}
    }
    @Override public Bundle status() {
        Bundle b=new Bundle();b.putBoolean("running",running);b.putLong("frames",frames);
        b.putString("error",error);b.putString("backend",backendName());return b;
    }
    @Override public void heartbeat() {heartbeat=SystemClock.uptimeMillis();}
    @Override public void stop() {running=false;cancelTouch();handler.post(this::release);}
    private synchronized void cancelTouch() {
        if(lastTouch!=null) {
            try {lastTouch.setAction(MotionEvent.ACTION_CANCEL);inject.invoke(inputManager,lastTouch,0);}
            catch(Throwable ignored) {} finally {lastTouch.recycle();lastTouch=null;}
        }
    }
    private void fail(Throwable e) {
        error=describe(e);running=false;cancelTouch();
        try {if(listener!=null)listener.onError(error);}catch(Exception ignored){}
        release();
    }
    private void release() {
        running=false;handler.removeCallbacks(frame);cancelTouch();
        if(target!=null){target.release();target=null;}
        if(exclude!=null){exclude.release();exclude=null;}
        captureArgs=null;listener=null;
    }
    @Override public void destroy() {stop();handler.post(()->{thread.quitSafely();System.exit(0);});}
    private String backendName() {
        return switch(backend) {
            case IWM_CAPTURE -> iwmCaptureOwner.endsWith("Internal")
                    ?"IWindowManager / ScreenCaptureInternal":"IWindowManager";
            case SCREEN_CAPTURE -> "ScreenCapture";
            case SURFACE_CAPTURE -> "SurfaceControl";
            case LEGACY_SCREENSHOT -> "SurfaceControl legacy";
            default -> "unavailable";
        };
    }
    private static String describe(Throwable t) {
        while(t instanceof InvocationTargetException&&t.getCause()!=null)t=t.getCause();
        return t.getClass().getSimpleName()+": "+String.valueOf(t.getMessage());
    }
    private static IBinder resolveDisplayToken(Class<?> surfaceControl) throws Exception {
        Class<?> displayControl=null;
        try {displayControl=Class.forName("com.android.server.display.DisplayControl");}
        catch(ClassNotFoundException ignored) {}
        Object token=DisplayTokenResolver.resolve(surfaceControl,displayControl);
        if(!(token instanceof IBinder))throw new IllegalStateException("El token de pantalla física tiene un tipo inesperado");
        return (IBinder)token;
    }
    private static Throwable append(Throwable root,Throwable next) {
        if(root==null)return next;root.addSuppressed(next);return root;
    }
    private static String describeTree(Throwable root) {
        StringBuilder out=new StringBuilder(describe(root));
        for(Throwable item:root.getSuppressed())out.append(" | ").append(describe(item));
        return out.toString();
    }
}
