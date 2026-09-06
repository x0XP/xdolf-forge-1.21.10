package com.darkcart.xdolf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

/** Maps Xdolf/LWJGL-style key names onto current GLFW key codes. */
final class KeyNames {
    private static final Map<String,Integer> KEYS=new HashMap<>();
    private static final Map<Integer,String> NAMES=new HashMap<>();
    static {
        for(var field:GLFW.class.getFields()) {
            String name=field.getName();
            if(!name.startsWith("GLFW_KEY_")||name.equals("GLFW_KEY_LAST")||name.equals("GLFW_KEY_UNKNOWN"))continue;
            try { int value=field.getInt(null);name=name.substring(9);KEYS.put(normalize(name),value);NAMES.put(value,name); }
            catch(IllegalAccessException e) {throw new ExceptionInInitializerError(e);}
        }
        alias("LSHIFT","LEFT_SHIFT");alias("RSHIFT","RIGHT_SHIFT");
        alias("LCONTROL","LEFT_CONTROL");alias("RCONTROL","RIGHT_CONTROL");
        alias("LCTRL","LEFT_CONTROL");alias("RCTRL","RIGHT_CONTROL");
        alias("LMENU","LEFT_ALT");alias("RMENU","RIGHT_ALT");alias("LALT","LEFT_ALT");alias("RALT","RIGHT_ALT");
        alias("LMETA","LEFT_SUPER");alias("RMETA","RIGHT_SUPER");alias("LWIN","LEFT_SUPER");alias("RWIN","RIGHT_SUPER");
        alias("RETURN","ENTER");alias("BACK","BACKSPACE");alias("ESC","ESCAPE");alias("GRAVE","GRAVE_ACCENT");
        alias("CAPITAL","CAPS_LOCK");alias("PRIOR","PAGE_UP");alias("NEXT","PAGE_DOWN");
        alias("SCROLL","SCROLL_LOCK");alias("SYSRQ","PRINT_SCREEN");
        alias("ADD","KP_ADD");alias("SUBTRACT","KP_SUBTRACT");alias("MULTIPLY","KP_MULTIPLY");alias("DIVIDE","KP_DIVIDE");
        alias("DECIMAL","KP_DECIMAL");alias("NUMPADENTER","KP_ENTER");alias("NUMPADEQUALS","KP_EQUAL");
        alias("LBRACKET","LEFT_BRACKET");alias("RBRACKET","RIGHT_BRACKET");
        for(int i=0;i<=9;i++)alias("NUMPAD"+i,"KP_"+i);
        KEYS.put("NONE",-1);NAMES.put(-1,"NONE");
    }
    private static String normalize(String s) {return s.toUpperCase(Locale.ROOT).replace("_","").replace("-","");}
    private static void alias(String alias,String target) {int key=KEYS.get(normalize(target));KEYS.put(normalize(alias),key);}
    static int parse(String name) {var key=KEYS.get(normalize(name));if(key==null)throw new IllegalArgumentException("Invalid key.");return key;}
    static String name(int key) {return NAMES.getOrDefault(key,"NONE");}
    static int read(String value,int fallback) {try {int key=Integer.parseInt(value);return NAMES.containsKey(key)?key:fallback;}catch(RuntimeException e){return fallback;}}
}
