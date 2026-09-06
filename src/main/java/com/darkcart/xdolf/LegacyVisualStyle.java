package com.darkcart.xdolf;

/** Source-defined values from Tracers, RenderUtils and Nametags in the 1.12.2 client. */
final class LegacyVisualStyle {
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
    static int tagOffset(float distance,boolean sneaking) {
        int offset=-(int)distance;
        return sneaking?offset+4:Math.max(-14,offset);
    }
}
