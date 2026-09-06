package com.darkcart.xdolf;

/** Golden cases derived from the supplied old source plus the modern screen-space parity layer. */
public final class LegacyVisualStyleTest {
    private static void equal(Object expected,Object actual) {
        if(!expected.equals(actual))throw new AssertionError("Expected "+expected+"; got "+actual);
    }
    private static void close(float expected,float actual) {
        if(Math.abs(expected-actual)>0.0001f)throw new AssertionError("Expected "+expected+"; got "+actual);
    }
    public static void main(String[] args) {
        equal(0xFFFF0000,LegacyVisualStyle.tracerColor(6,false));
        equal(0xFFFF8000,LegacyVisualStyle.tracerColor(50,false));
        equal(0xFFFFF500,LegacyVisualStyle.tracerColor(96,false));
        equal(0xFF1A99FF,LegacyVisualStyle.tracerColor(97,false));
        equal(0xFF00FF00,LegacyVisualStyle.tracerColor(200,true));
        equal("Player \u00a7a75%",LegacyVisualStyle.tag("Player",15,false));
        equal("\u00a79Friend \u00a7a100%",LegacyVisualStyle.tag("Friend",20,true));
        equal("Player \u00a7a75% HP \u00a7b18 Armor",LegacyVisualStyle.tag("Player",15,18,false));
        equal("\u00a79Friend \u00a7a100% HP \u00a7b20 Armor",LegacyVisualStyle.tag("Friend",20,20,true));
        close(0f,LegacyVisualStyle.tagScale(60));
        close(1f,LegacyVisualStyle.screenTagScale(8));
        close(0.8090909f,LegacyVisualStyle.screenTagScale(50));
        close(0.68f,LegacyVisualStyle.screenTagScale(200));
        equal(-14,LegacyVisualStyle.tagOffset(100,false));
        equal(-96,LegacyVisualStyle.tagOffset(100,true));
        equal(-3,LegacyVisualStyle.tagOffset(3.9f,false));
        equal(1.5,LegacyVisualStyle.TRACER_WIDTH);
        equal(1.8,LegacyVisualStyle.TRAJECTORY_WIDTH);
        equal(0x80000000,LegacyVisualStyle.BOX_EDGE);
        System.out.println("Legacy visual style golden cases passed");
    }
}
