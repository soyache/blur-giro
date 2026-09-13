/*
 * Adapted from DuoFold-Android (https://github.com/jcx396905-gif/DuoFold-Android)
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 */
package com.soyache.blurgiro.motion;

/** Edge-pivoted optical plane. World height is two units, camera FOV is 30 degrees. */
public final class BokehProjection {
    private BokehProjection() {}
    public static final float CAMERA_DISTANCE=(float)(1/Math.tan(Math.toRadians(15)));

    public static float[] matrix(float pitch,float roll,float aspect) {
        float px=roll<=0?-aspect:aspect,py=pitch<=0?-1:1;
        float cx=(float)Math.cos(pitch),sx=-(float)Math.sin(pitch);
        float cy=(float)Math.cos(roll),sy=(float)Math.sin(roll);
        return new float[]{
            cy,sx*sy,-cx*sy,0,
            0,cx,sx,0,
            sy,-sx*cy,cx*cy,0,
            px-cy*px,py-sx*sy*px-cx*py,cx*sy*px-sx*py,1
        };
    }
    public static float[] sourceAt(float x,float y,float[] m,float aspect) {
        float px=(2*x-1)*aspect,py=1-2*y;
        float wx=m[0]*px+m[4]*py+m[12];
        float wy=m[1]*px+m[5]*py+m[13];
        float wz=m[2]*px+m[6]*py+m[14];
        float scale=CAMERA_DISTANCE/Math.max(.02f*CAMERA_DISTANCE,CAMERA_DISTANCE-wz);
        return new float[]{.5f+wx*scale/(2*aspect),.5f-wy*scale/2};
    }
}
