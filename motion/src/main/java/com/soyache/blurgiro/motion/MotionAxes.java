/*
 * Adapted from DuoFold-Android (https://github.com/jcx396905-gif/DuoFold-Android)
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 */
package com.soyache.blurgiro.motion;

/** Combines rotations expressed in the current screen coordinate system. */
public final class MotionAxes {
    private MotionAxes() {}

    /** Returns {forward/back around screen X, left/right around screen Y}. */
    public static float[] resolve(float sideTilt, float forwardTilt, boolean includeForward) {
        if (!Float.isFinite(sideTilt) || !Float.isFinite(forwardTilt)) return new float[]{0, 0};
        return new float[]{includeForward ? forwardTilt : 0, sideTilt};
    }
}
