package com.soyache.blurgiro.motion;

import org.junit.Test;
import static org.junit.Assert.*;

public class HoverBlurTest {
    @Test public void heldPoseBecomesClearWithoutMoreSensorEvents() {
        HoverBlur blur=new HoverBlur();
        assertEquals(1,blur.sample(0,.4f,0),0);
        assertEquals(1,blur.sample(0,.4f,.1),0);
        assertEquals(.5f,blur.sample(0,.4f,.36),.001f);
        assertEquals(0,blur.sample(0,.4f,.61),0);
    }
    @Test public void bothAxesResumeBlurAndSubthresholdNoiseDoesNot() {
        HoverBlur blur=new HoverBlur();blur.sample(.2f,.4f,0);
        for(int i=1;i<=100;i++)blur.sample(.2f+(i%2==0?.001f:-.001f),.4f,i*.01);
        assertEquals(0,blur.sample(.2f,.4f,1.01),0);
        assertEquals(1,blur.sample(.22f,.4f,1.02),0);
        assertEquals(0,blur.sample(.22f,.4f,1.7),0);
        assertEquals(1,blur.sample(.22f,.42f,1.71),0);
    }
    @Test public void gradualMovementAccumulatesAndResetStartsFresh() {
        HoverBlur blur=new HoverBlur();blur.sample(0,0,0);
        for(int i=1;i<=200;i++)assertTrue(blur.sample(0,i*.001f,i*.016)>.9f);
        blur.sample(0,.2f,5);blur.reset();
        assertEquals(1,blur.sample(0,.2f,6),0);
    }
    @Test public void invalidInputCannotPoisonFollowingFrames() {
        HoverBlur blur=new HoverBlur();
        assertEquals(0,blur.sample(Float.NaN,0,0),0);
        assertEquals(1,blur.sample(0,0,1),0);
        assertEquals(0,blur.sample(0,0,Double.POSITIVE_INFINITY),0);
        assertEquals(0,blur.sample(0,0,2),0);
    }
    @Test public void oldAndUnknownPreferencesUseOriginalEffect() {
        assertEquals(EffectStyle.CLASSIC,EffectStyle.fromId(null));
        assertEquals(EffectStyle.CLASSIC,EffectStyle.fromId("future-style"));
        assertEquals(EffectStyle.SETTLE,EffectStyle.fromId("settle"));
    }
}
