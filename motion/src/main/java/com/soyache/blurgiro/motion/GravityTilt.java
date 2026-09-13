/*
 * Adapted from DuoFold-Android (https://github.com/jcx396905-gif/DuoFold-Android)
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 */
package com.soyache.blurgiro.motion;

/** Converts an accelerometer/gravity vector into screen-relative forward and side angles. */
public final class GravityTilt {
    private GravityTilt() {}

    public static float[] angles(float x,float y,float z,int rotation) {
        float screenX=x,screenY=y;
        switch(rotation) {
            case 1: screenX=y;screenY=-x;break;
            case 2: screenX=-x;screenY=-y;break;
            case 3: screenX=-y;screenY=x;break;
            default: break;
        }
        float forward=-(float)Math.atan2(z,Math.hypot(screenX,screenY));
        float side=(float)Math.atan2(screenX,Math.hypot(screenY,z));
        return new float[]{forward,side};
    }
}
