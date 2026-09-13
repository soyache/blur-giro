/*
 * Adapted from DuoFold-Android FoldView
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro OpenGL ES reprojection of the live capture texture.
 */
package com.soyache.blurgiro.render;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.Surface;
import com.soyache.blurgiro.BuildConfig;
import com.soyache.blurgiro.motion.BokehProjection;
import com.soyache.blurgiro.motion.EffectStyle;
import com.soyache.blurgiro.motion.FoldModel;
import com.soyache.blurgiro.motion.HoverBlur;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class CrystalView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public interface Listener {
        void onReady(Surface input,int width,int height);
        void onFirstFrame();
        void onError(String message);
    }
    private final Listener listener;
    private final FloatBuffer quad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] matrix=new float[16];
    private volatile FoldModel.State state=FoldModel.atAxes(0,0,1,400,800);
    private volatile FoldModel.State drawnState=FoldModel.atAxes(0,0,1,400,800);
    private volatile EffectStyle effectStyle=EffectStyle.CLASSIC;
    private EffectStyle drawnStyle=EffectStyle.CLASSIC;
    private final HoverBlur hoverBlur=new HoverBlur();
    private float displayedHover=1;
    private long lastDrawNanos;
    private SurfaceTexture texture;
    private Surface input;
    private int program,textureId,width,height;
    private int bokehProgram,captureWidth,captureHeight;
    private MipScreen mipScreen;
    private boolean prefilterDirty=true;
    private volatile boolean newFrame,hasFrame;
    private boolean firstFrameReported;
    private volatile boolean stopped;
    private final Runnable settleFrame=()-> {if(!stopped)requestRender();};
    private volatile boolean saveDebugFrame;
    public void saveDebugFrame() {if(BuildConfig.DEBUG){saveDebugFrame=true;requestRender();}}
    public CrystalView(Context context,Listener listener) {
        super(context); this.listener=listener;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,0,0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setPreserveEGLContextOnPause(true);
        quad.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);
        setRenderer(this); setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
    public void setFold(float forwardRadians,float sideRadians,float intensity) {
        float density=getResources().getDisplayMetrics().density;
        float widthPoints=Math.max(1,getWidth())/density,heightPoints=Math.max(1,getHeight())/density;
        state=FoldModel.atAxes(forwardRadians,sideRadians,intensity,widthPoints,heightPoints);requestRender();
    }
    public FoldModel.State inputState() { return drawnState; }
    public void setEffectStyle(EffectStyle style) {
        effectStyle=style==null?EffectStyle.CLASSIC:style;requestRender();
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
        try {
            bokehProgram=0;mipScreen=null;prefilterDirty=true;
            program=GLES20.glCreateProgram();
            GLES20.glAttachShader(program,compile(GLES20.GL_VERTEX_SHADER,asset("fold.vert")));
            GLES20.glAttachShader(program,compile(GLES20.GL_FRAGMENT_SHADER,asset("fold.frag")));
            GLES20.glLinkProgram(program);
            int[] status=new int[1]; GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,status,0);
            if(status[0]==0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
            int[] ids=new int[1]; GLES20.glGenTextures(1,ids,0); textureId=ids[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            texture=new SurfaceTexture(textureId);
            texture.setOnFrameAvailableListener(t->{newFrame=true; requestRender();});
            input=new Surface(texture); hasFrame=false;
        } catch(Exception e) { post(()->listener.onError("GPU: "+e.getMessage())); }
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
        GLES20.glViewport(0,0,w,h); width=w; height=h;
        if(texture==null || stopped) return;
        captureWidth=Math.min(w,1080); captureHeight=Math.round(h*(captureWidth/(float)w));
        prefilterDirty=true;
        texture.setDefaultBufferSize(captureWidth,captureHeight);
        post(()-> { if(!stopped) listener.onReady(input,captureWidth,captureHeight); });
    }
    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0,0,0,0); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if(texture==null || stopped || program==0) return;
        try {
            if(newFrame) { newFrame=false; texture.updateTexImage(); texture.getTransformMatrix(matrix); hasFrame=true;prefilterDirty=true; }
            if(!hasFrame) return;
            EffectStyle style=effectStyle;
            FoldModel.State s=FoldModel.forStyle(state,style,width/(float)Math.max(1,height));
            long now=System.nanoTime();
            if(style!=drawnStyle) {hoverBlur.reset();displayedHover=1;lastDrawNanos=0;drawnStyle=style;}
            float targetHover=style==EffectStyle.SETTLE?hoverBlur.sample(s.angleX,s.angleY,now/1e9):1;
            float dt=lastDrawNanos==0?0:Math.min(.1f,(now-lastDrawNanos)/1e9f);
            lastDrawNanos=now;
            float response=1-(float)Math.exp(-dt/.045f);
            displayedHover+=response*(targetHover-displayedHover);
            if(Math.abs(displayedHover-targetHover)<.001f)displayedHover=targetHover;
            if(style==EffectStyle.BOKEH) {
                drawBokeh(s);
            } else {
            GLES20.glUseProgram(program);
            int pos=GLES20.glGetAttribLocation(program,"aPosition");
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,quad);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glUniform1i(location("uScreen"),0);
            GLES20.glUniformMatrix4fv(location("uTextureMatrix"),1,false,matrix,0);
            GLES20.glUniform2f(location("uResolution"),width,height);
            GLES20.glUniform2f(location("uAngles"),s.angleX,s.angleY);
            GLES20.glUniform2f(location("uSizePoints"),s.widthPoints,s.heightPoints);
            scalar("uIntensity",s.intensity);
            GLES20.glUniform1i(location("uEffectStyle"),style.shaderId);
            scalar("uHoverBlur",displayedHover);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            GLES20.glDisableVertexAttribArray(pos);
            }
            drawnState=s;
            removeCallbacks(settleFrame);
            if(style==EffectStyle.SETTLE && (targetHover>0 || displayedHover>0))postDelayed(settleFrame,16);
            if(!firstFrameReported) {
                firstFrameReported=true;
                post(listener::onFirstFrame);
            }
            if(saveDebugFrame && BuildConfig.DEBUG) {
                saveDebugFrame=false;
                ByteBuffer pixels=ByteBuffer.allocateDirect(width*height*4);
                GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels);
                int[] argb=new int[width*height];
                for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                    int offset=((height-1-y)*width+x)*4;
                    argb[y*width+x]=0xff000000|((pixels.get(offset)&255)<<16)|((pixels.get(offset+1)&255)<<8)|(pixels.get(offset+2)&255);
                }
                android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(argb,width,height,android.graphics.Bitmap.Config.ARGB_8888);
                new Thread(()-> {
                    try {
                        java.io.File pending=new java.io.File(getContext().getFilesDir(),"debug-frame.pending.png");
                        try(java.io.FileOutputStream out=new java.io.FileOutputStream(pending)) {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
                        }
                        if(!pending.renameTo(new java.io.File(getContext().getFilesDir(),"debug-frame.png")))
                            throw new java.io.IOException("Cannot publish completed frame");
                    } catch(java.io.IOException ignored) {}
                    finally {bitmap.recycle();}
                },"CristalFrameExport").start();
            }
        } catch(Exception e) { post(()->listener.onError("Fotograma GPU: "+e.getMessage())); }
    }
    public void releaseResources() {
        stopped=true;
        removeCallbacks(settleFrame);
        queueEvent(()-> {
            if(input!=null) {input.release();input=null;}
            if(texture!=null) {texture.release();texture=null;}
            if(program!=0) GLES20.glDeleteProgram(program);
            if(bokehProgram!=0)GLES20.glDeleteProgram(bokehProgram);
            if(mipScreen!=null) {mipScreen.release();mipScreen=null;}
            if(textureId!=0) GLES20.glDeleteTextures(1,new int[]{textureId},0);
        });
    }
    private void drawBokeh(FoldModel.State s) throws Exception {
        if(bokehProgram==0)bokehProgram=createProgram("bokeh.vert","bokeh.frag");
        if(mipScreen==null)mipScreen=new MipScreen(createProgram("fold.vert","copy.frag"));
        if(prefilterDirty) {
            mipScreen.update(textureId,matrix,captureWidth,captureHeight,quad,width,height);
            prefilterDirty=false;
        }
        if(Math.abs(s.angleX)+Math.abs(s.angleY)<.0001f)return;
        GLES20.glUseProgram(bokehProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mipScreen.texture());
        GLES20.glUniform1i(GLES20.glGetUniformLocation(bokehProgram,"uTexture"),1);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(bokehProgram,"uModelMatrix"),1,false,s.bokehMatrix,0);
        float aspect=width/(float)height;
        GLES20.glUniform2f(GLES20.glGetUniformLocation(bokehProgram,"uScreenHalfSize"),aspect,1);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(bokehProgram,"uPhotoHalfSize"),aspect,1);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(bokehProgram,"uPhotoOffset"),0,0);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(bokehProgram,"uTexelSize"),1f/captureWidth,1f/captureHeight);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(bokehProgram,"uCameraDistance"),BokehProjection.CAMERA_DISTANCE);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(bokehProgram,"uAperture"),s.intensity);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(bokehProgram,"uMaxBlurPixels"),160);
        int pos=GLES20.glGetAttribLocation(bokehProgram,"aPosition");
        GLES20.glEnableVertexAttribArray(pos);quad.position(0);
        GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDisableVertexAttribArray(pos);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }
    private int createProgram(String vertex,String fragment) throws Exception {
        int result=GLES20.glCreateProgram();
        int vs=compile(GLES20.GL_VERTEX_SHADER,asset(vertex)),fs=compile(GLES20.GL_FRAGMENT_SHADER,asset(fragment));
        GLES20.glAttachShader(result,vs);GLES20.glAttachShader(result,fs);GLES20.glLinkProgram(result);
        GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs);
        int[] status=new int[1];GLES20.glGetProgramiv(result,GLES20.GL_LINK_STATUS,status,0);
        if(status[0]==0) {String info=GLES20.glGetProgramInfoLog(result);GLES20.glDeleteProgram(result);throw new IllegalStateException(info);}
        return result;
    }
    private int location(String name) { return GLES20.glGetUniformLocation(program,name); }
    private void scalar(String name,float value) { GLES20.glUniform1f(location(name),value); }
    private String asset(String name) throws Exception {
        try(InputStream in=getContext().getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];int read;
            while((read=in.read(buffer))!=-1)out.write(buffer,0,read);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private int compile(int type,String source) {
        int shader=GLES20.glCreateShader(type); GLES20.glShaderSource(shader,source); GLES20.glCompileShader(shader);
        int[] status=new int[1]; GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,status,0);
        if(status[0]==0) throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader)); return shader;
    }
}
