/*
 * Adapted from DuoFold-Android MipScreen
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 */
package com.soyache.blurgiro.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

/** Copies a live external screen texture to a mipmapped bitmap-style texture on the GPU. */
final class MipScreen {
    private final int program;
    private int texture,framebuffer,width,height;
    private int sourceWidth,sourceHeight;
    MipScreen(int copyProgram) {program=copyProgram;}
    int texture() {return texture;}

    void update(int external,float[] transform,int w,int h,FloatBuffer quad,int viewW,int viewH) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        if(texture==0 || sourceWidth!=w || sourceHeight!=h) {
            releaseTarget();
            sourceWidth=w;sourceHeight=h;
            String extensions=GLES20.glGetString(GLES20.GL_EXTENSIONS);
            boolean fullNpot=extensions!=null&&extensions.contains("GL_OES_texture_npot");
            width=fullNpot?w:powerOfTwo(w);height=fullNpot?h:powerOfTwo(h);
            int[] limit=new int[1];GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,limit,0);
            if(width>limit[0] || height>limit[0])throw new IllegalStateException("La pantalla supera el tamaño de textura de la GPU");
            int[] ids=new int[1];GLES20.glGenTextures(1,ids,0);texture=ids[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,width,height,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
            GLES20.glGenFramebuffers(1,ids,0);framebuffer=ids[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,texture,0);
            if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
                throw new IllegalStateException("No pude preparar la textura de lente");
            }
        }
        try {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer);
            GLES20.glViewport(0,0,width,height);
            GLES20.glUseProgram(program);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,external);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uScreen"),0);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uTextureMatrix"),1,false,transform,0);
            int pos=GLES20.glGetAttribLocation(program,"aPosition");
            GLES20.glEnableVertexAttribArray(pos);
            quad.position(0);GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,quad);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            GLES20.glDisableVertexAttribArray(pos);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            int error=GLES20.glGetError();
            if(error!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Error GL de textura de lente "+error);
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
            GLES20.glViewport(0,0,viewW,viewH);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }
    private static int powerOfTwo(int n) {int size=1;while(size<n)size<<=1;return size;}
    private void releaseTarget() {
        if(texture!=0)GLES20.glDeleteTextures(1,new int[]{texture},0);
        if(framebuffer!=0)GLES20.glDeleteFramebuffers(1,new int[]{framebuffer},0);
        texture=framebuffer=0;
    }
    void release() {releaseTarget();GLES20.glDeleteProgram(program);}
}
