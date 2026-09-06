package com.x0xp.xdolf.mixin;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-render vertical offset for Xdolf TTF text inside Minecraft chat containers. */
final class ChatTextContext {
    private static final Deque<Integer> Y_OFFSETS = new ArrayDeque<>();

    private ChatTextContext() {}

    static void push(int yOffset) {
        Y_OFFSETS.push(yOffset);
    }

    static void pop() {
        if (!Y_OFFSETS.isEmpty()) Y_OFFSETS.pop();
    }

    static int yOffset() {
        return Y_OFFSETS.isEmpty() ? 0 : Y_OFFSETS.peek();
    }
}
