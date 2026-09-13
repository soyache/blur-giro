package com.soyache.blurgiro.motion;

import org.junit.Test;
import static org.junit.Assert.*;

public class BokehProjectionTest {
    private FoldModel.State state(float pitch,float roll,float aspect) {
        return FoldModel.forStyle(FoldModel.atAxes(pitch,roll,1,400,800),EffectStyle.BOKEH,aspect);
    }
    @Test public void neutralPoseAndEveryPinnedEdgeRemainAligned() {
        for(float aspect:new float[]{.45f,1,2.2f}) {
            assertArrayEquals(new float[]{.23f,.72f},FoldModel.sourceAt(.23f,.72f,state(0,0,aspect)),.00001f);
            for(float angle:new float[]{-.5f,.5f}) {
                float x=angle>0?1:0,y=angle>0?0:1;
                assertArrayEquals(new float[]{x,.3f},FoldModel.sourceAt(x,.3f,state(0,angle,aspect)),.00001f);
                assertArrayEquals(new float[]{.3f,y},FoldModel.sourceAt(.3f,y,state(angle,0,aspect)),.00001f);
            }
        }
    }
    @Test public void independentMatrixCompositionMatchesSourceRenderer() {
        for(float aspect:new float[]{.45f,1,2.2f})for(float pitch:new float[]{-.7f,0,.8f})
            for(float roll:new float[]{-.9f,0,.6f})for(float x:new float[]{0,.3f,1})for(float y:new float[]{0,.6f,1}) {
                double px=roll<=0?-aspect:aspect,py=pitch<=0?-1:1;
                double dx=(2*x-1)*aspect-px,dy=1-2*y-py;
                double wx=Math.cos(roll)*dx+px,z1=-Math.sin(roll)*dx;
                double wy=Math.cos(pitch)*dy+Math.sin(pitch)*z1+py;
                double wz=-Math.sin(pitch)*dy+Math.cos(pitch)*z1;
                double eye=1/Math.tan(Math.toRadians(15)),scale=eye/Math.max(.02*eye,eye-wz);
                assertArrayEquals(new float[]{(float)(.5+wx*scale/(2*aspect)),(float)(.5-wy*scale/2)},
                        FoldModel.sourceAt(x,y,state(pitch,roll,aspect)),.0001f);
            }
    }
    @Test public void newStyleUsesIndependentAxisLimitsAndClassicStaysTheDefault() {
        FoldModel.State bokeh=state(2,2,.45f);
        assertEquals(Math.toRadians(80),bokeh.angleX,.000001);
        assertEquals(Math.toRadians(60),bokeh.angleY,.000001);
        assertEquals(EffectStyle.CLASSIC,EffectStyle.fromId(null));
        assertEquals(EffectStyle.CLASSIC,EffectStyle.fromId("invalid"));
        FoldModel.State classic=FoldModel.atAxes(.6f,.7f,1,400,800);
        assertSame(classic,FoldModel.forStyle(classic,EffectStyle.CLASSIC));
    }
}
