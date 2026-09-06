package com.x0xp.xdolf;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-render vertical offset for Xdolf TTF text inside Minecraft chat containers. */
public final class ChatTextContext {
    private static final Deque<Integer> Y_OFFSETS = new ArrayDeque<>();

    private ChatTextContext() {}

    public static void push(int yOffset) {
        Y_OFFSETS.push(yOffset);
    }

    public static void pop() {
        if (!Y_OFFSETS.isEmpty()) Y_OFFSETS.pop();
    }

    public static int yOffset() {
        return Y_OFFSETS.isEmpty() ? 0 : Y_OFFSETS.peek();
    }
}
