package com.darkcart.xdolf;

/** Shared rendering values derived from the original Xdolf client. */
final class VisualStyle {
    static final double TRACER_WIDTH=1.5,TRAJECTORY_WIDTH=1.8,BOX_WIDTH=1;
    static final int CHEST_TRACER=0xFF1AFF00,BOX_EDGE=0x80000000;

    static int tracerColor(double distance,boolean friend) {
        if(friend)return 0xFF00FF00;
        if(distance<=6)return 0xFFFF0000;
        if(distance<=96)return 0xFFFF0000|(Math.round((float)distance/100f*255)<<8);
        return 0xFF1A99FF;
    }

    static String tag(String name,float health,boolean friend) {
        return (friend?"\u00a79":"")+name+" \u00a7a"+(int)(health/20*100)+"%";
    }

    static String tag(String name,float health,int armor,boolean friend) {
        String tag=(friend?"\u00a79":"")+name+" \u00a7a"+(int)(health/20*100)+"% HP";
        return armor>0?tag+" \u00a7b"+armor+" Armor":tag;
    }

    /** Real player tags are rendered in GUI space; the old world-space copy remains disabled. */
    static float tagScale(float distance) { return 0f; }

    /** Full size nearby, gently smaller at range, and clamped so distant names remain readable. */
    static float screenTagScale(float distance) {
        float scale=1f-Math.max(0f,distance-8f)/220f;
        return Math.max(0.68f,Math.min(1f,scale));
    }

    static int tagOffset(float distance,boolean sneaking) {
        int offset=-(int)distance;
        return sneaking?offset+4:Math.max(-14,offset);
    }
}
