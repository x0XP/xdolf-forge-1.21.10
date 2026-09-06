package com.x0xp.xdolf;

/** Golden cases for Xdolf's retained visual behaviour. */
public final class VisualStyleTest {
    private static void equal(Object expected,Object actual) {
        if(!expected.equals(actual))throw new AssertionError("Expected "+expected+"; got "+actual);
    }
    private static void close(float expected,float actual) {
        if(Math.abs(expected-actual)>0.0001f)throw new AssertionError("Expected "+expected+"; got "+actual);
    }
    public static void main(String[] args) {
        equal(0xFFFF0000,VisualStyle.tracerColor(6,false));
        equal(0xFFFF8000,VisualStyle.tracerColor(50,false));
        equal(0xFFFFF500,VisualStyle.tracerColor(96,false));
        equal(0xFF1A99FF,VisualStyle.tracerColor(97,false));
        equal(0xFF00FF00,VisualStyle.tracerColor(200,true));
        equal("Player \u00a7a75%",VisualStyle.tag("Player",15,false));
        equal("\u00a79Friend \u00a7a100%",VisualStyle.tag("Friend",20,true));
        equal("Player \u00a7a75% HP",VisualStyle.tag("Player",15,0,false));
        equal("Player \u00a7a75% HP \u00a7b18 Armor",VisualStyle.tag("Player",15,18,false));
        equal("\u00a79Friend \u00a7a100% HP \u00a7b20 Armor",VisualStyle.tag("Friend",20,20,true));
        close(0f,VisualStyle.tagScale(60));
        close(1f,VisualStyle.screenTagScale(8));
        close(0.8090909f,VisualStyle.screenTagScale(50));
        close(0.68f,VisualStyle.screenTagScale(200));
        equal(-14,VisualStyle.tagOffset(100,false));
        equal(-96,VisualStyle.tagOffset(100,true));
        equal(-3,VisualStyle.tagOffset(3.9f,false));
        equal(1.5,VisualStyle.TRACER_WIDTH);
        equal(1.8,VisualStyle.TRAJECTORY_WIDTH);
        equal(0x80000000,VisualStyle.BOX_EDGE);
        System.out.println("Xdolf visual style golden cases passed");
    }
}
