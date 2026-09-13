package com.x0xp.xdolf.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Minimal Discord desktop IPC client for Xdolf Rich Presence.
 *
 * <p>All blocking pipe I/O lives off the Minecraft render thread. Live activity
 * changes are written only when the requested presence actually changes.
 */
public final class DiscordPresence {
    public static final String APPLICATION_ID = "1548709727276376184";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object STATE_LOCK = new Object();
    private static final Object WRITE_LOCK = new Object();

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;
    private static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
    private static final long RETRY_MILLIS = 2500L;
    private static final long READ_POLL_MILLIS = 20L;

    private static volatile boolean running;
    private static volatile boolean ready;
    private static volatile long generation;
    private static volatile RandomAccessFile pipe;
    private static volatile Thread worker;
    private static volatile String activityNonce;
    private static volatile ActivitySpec desiredActivity = new ActivitySpec("Playing with Xdolf", "In the menus", 0L);
    private static volatile ActivitySpec lastSentActivity;

    private DiscordPresence() {}

    /** Update the presence requested by the module. Unchanged values are not resent. */
    public static void update(String details, String state, long startedAt) {
        ActivitySpec requested = new ActivitySpec(limit(details, 128), limit(state, 128), Math.max(0L, startedAt));
        desiredActivity = requested;

        RandomAccessFile current = pipe;
        if (!running || !ready || current == null || requested.equals(lastSentActivity)) return;
        try {
            sendActivity(current, requested);
        } catch (IOException error) {
            LOGGER.debug("Could not update Xdolf Discord RPC activity; reconnecting", error);
            closeQuietly(current);
        }
    }

    public static void start() {
        final long token;
        synchronized (STATE_LOCK) {
            if (running) return;
            running = true;
            ready = false;
            lastSentActivity = null;
            token = ++generation;
            Thread thread = new Thread(() -> runLoop(token), "Xdolf Discord RPC");
            thread.setDaemon(true);
            worker = thread;
            thread.start();
        }
    }

    public static void stop() {
        RandomAccessFile current;
        Thread currentWorker;
        synchronized (STATE_LOCK) {
            if (!running && pipe == null) return;
            running = false;
            ready = false;
            generation++;
            current = pipe;
            pipe = null;
            currentWorker = worker;
            worker = null;
            activityNonce = null;
            lastSentActivity = null;
        }

        if (current != null) {
            try {
                writeFrame(current, OP_FRAME, clearActivityPayload());
            } catch (Throwable ignored) {
                // Discord may already have closed its end. Disable must remain safe.
            }
            closeQuietly(current);
        }
        if (currentWorker != null) currentWorker.interrupt();
        LOGGER.info("Xdolf Discord RPC stopped");
    }

    public static boolean connected() {
        return running && ready && pipe != null;
    }

    private static void runLoop(long token) {
        try {
            if (!isWindows()) {
                LOGGER.debug("Xdolf Discord RPC is idle: desktop IPC transport is only enabled on Windows");
                return;
            }

            while (active(token)) {
                RandomAccessFile connection = null;
                try {
                    connection = openDiscordPipe();
                    if (!publishConnection(token, connection)) {
                        closeQuietly(connection);
                        return;
                    }

                    writeFrame(connection, OP_HANDSHAKE, handshakePayload());
                    awaitReady(connection, token);
                    if (!markReady(token, connection)) return;

                    lastSentActivity = null;
                    sendActivity(connection, desiredActivity);
                    LOGGER.info("Xdolf Discord RPC connected; activity sent");

                    readLoop(connection, token);
                } catch (IOException | RuntimeException error) {
                    if (active(token))
                        LOGGER.debug("Xdolf Discord RPC connection unavailable; retrying", error);
                } finally {
                    clearConnection(connection);
                    closeQuietly(connection);
                }

                if (!sleepBeforeRetry(token)) return;
            }
        } finally {
            synchronized (STATE_LOCK) {
                if (worker == Thread.currentThread()) worker = null;
            }
        }
    }

    private static void awaitReady(RandomAccessFile connection, long token) throws IOException {
        while (active(token)) {
            Frame frame = readFrame(connection, token);
            if (frame.opcode == OP_PING) {
                writeFrame(connection, OP_PONG, frame.payload);
                continue;
            }
            if (frame.opcode == OP_CLOSE)
                throw new IOException("Discord closed RPC during handshake");
            if (frame.opcode != OP_FRAME) continue;

            JsonObject message = parseObject(frame.payload);
            if ("READY".equals(string(message, "evt"))) {
                LOGGER.info("Xdolf Discord RPC READY received");
                return;
            }
            logRpcError(message);
        }
        throw new InterruptedIOException("Discord RPC stopped during handshake");
    }

    private static void readLoop(RandomAccessFile connection, long token) throws IOException {
        while (active(token)) {
            Frame frame = readFrame(connection, token);
            switch (frame.opcode) {
                case OP_PING -> writeFrame(connection, OP_PONG, frame.payload);
                case OP_CLOSE -> throw new IOException("Discord closed RPC connection");
                case OP_FRAME -> handleFrame(frame.payload);
                default -> { }
            }
        }
    }

    private static void sendActivity(RandomAccessFile connection, ActivitySpec activity) throws IOException {
        String nonce = UUID.randomUUID().toString();
        activityNonce = nonce;
        writeFrame(connection, OP_FRAME, setActivityPayload(nonce, activity));
        lastSentActivity = activity;
    }

    private static void handleFrame(String payload) {
        JsonObject message;
        try {
            message = parseObject(payload);
        } catch (RuntimeException error) {
            LOGGER.debug("Ignoring malformed Discord RPC frame", error);
            return;
        }

        logRpcError(message);
        String nonce = string(message, "nonce");
        String expected = activityNonce;
        if (expected != null && expected.equals(nonce)) {
            if (!"ERROR".equals(string(message, "evt")))
                LOGGER.info("Xdolf Discord RPC activity accepted");
            activityNonce = null;
        }
    }

    private static void logRpcError(JsonObject message) {
        if (!"ERROR".equals(string(message, "evt"))) return;
        String error = "unknown Discord RPC error";
        JsonElement data = message.get("data");
        if (data != null && data.isJsonObject()) {
            String detail = string(data.getAsJsonObject(), "message");
            if (!detail.isBlank()) error = detail;
        }
        LOGGER.warn("Xdolf Discord RPC rejected a command: {}", error);
    }

    private static RandomAccessFile openDiscordPipe() throws IOException {
        IOException last = null;
        for (int index = 0; index < 10; index++) {
            for (String prefix : new String[]{"\\\\.\\pipe\\discord-ipc-", "\\\\?\\pipe\\discord-ipc-"}) {
                try {
                    return new RandomAccessFile(prefix + index, "rw");
                } catch (IOException error) {
                    last = error;
                }
            }
        }
        throw last == null ? new IOException("No Discord IPC pipe found") : last;
    }

    private static String handshakePayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("client_id", APPLICATION_ID);
        return payload.toString();
    }

    private static String setActivityPayload(String nonce, ActivitySpec spec) {
        JsonObject activity = new JsonObject();
        activity.addProperty("type", 0);
        activity.addProperty("details", spec.details);
        activity.addProperty("state", spec.state);
        if (spec.startedAt > 0L) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", spec.startedAt);
            activity.add("timestamps", timestamps);
        }

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", nonce);
        return payload.toString();
    }

    private static String clearActivityPayload() {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", JsonNull.INSTANCE);

        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", UUID.randomUUID().toString());
        return payload.toString();
    }

    private static void writeFrame(RandomAccessFile connection, int opcode, String payload) throws IOException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(opcode);
        frame.putInt(body.length);
        frame.put(body);
        synchronized (WRITE_LOCK) {
            // Discord's IPC transport expects one complete frame per pipe write.
            connection.write(frame.array());
        }
    }

    private static Frame readFrame(RandomAccessFile connection, long token) throws IOException {
        byte[] header = new byte[8];
        readFullyPolling(connection, header, token);
        ByteBuffer values = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = values.getInt();
        int length = values.getInt();
        if (length < 0 || length > MAX_PAYLOAD_BYTES)
            throw new IOException("Invalid Discord RPC payload length: " + length);
        byte[] body = new byte[length];
        readFullyPolling(connection, body, token);
        return new Frame(opcode, new String(body, StandardCharsets.UTF_8));
    }

    /**
     * RandomAccessFile opens a synchronous Windows pipe. Poll buffered bytes before
     * reading so a blocked ReadFile cannot prevent the render thread's disable write.
     */
    private static void readFullyPolling(RandomAccessFile connection, byte[] destination, long token) throws IOException {
        int offset = 0;
        while (offset < destination.length) {
            if (!active(token) || Thread.currentThread().isInterrupted())
                throw new InterruptedIOException("Discord RPC read interrupted");

            long available = connection.length();
            if (available <= 0) {
                sleepReadPoll();
                continue;
            }

            int request = (int) Math.min(destination.length - offset, Math.min(available, Integer.MAX_VALUE));
            int count = connection.read(destination, offset, request);
            if (count < 0) throw new EOFException("Discord IPC pipe closed");
            if (count == 0) {
                sleepReadPoll();
                continue;
            }
            offset += count;
        }
    }

    private static void sleepReadPoll() throws InterruptedIOException {
        try {
            Thread.sleep(READ_POLL_MILLIS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Discord RPC read interrupted");
        }
    }

    private static JsonObject parseObject(String payload) {
        JsonElement parsed = JsonParser.parseString(payload);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Discord RPC payload was not an object");
        return parsed.getAsJsonObject();
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean publishConnection(long token, RandomAccessFile connection) {
        synchronized (STATE_LOCK) {
            if (!running || generation != token) return false;
            pipe = connection;
            ready = false;
            return true;
        }
    }

    private static boolean markReady(long token, RandomAccessFile connection) {
        synchronized (STATE_LOCK) {
            if (!running || generation != token || pipe != connection) return false;
            ready = true;
            return true;
        }
    }

    private static void clearConnection(RandomAccessFile connection) {
        if (connection == null) return;
        synchronized (STATE_LOCK) {
            if (pipe == connection) {
                pipe = null;
                ready = false;
                lastSentActivity = null;
                activityNonce = null;
            }
        }
    }

    private static boolean active(long token) {
        return running && generation == token;
    }

    private static boolean sleepBeforeRetry(long token) {
        if (!active(token)) return false;
        try {
            Thread.sleep(RETRY_MILLIS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return active(token);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String limit(String value, int maxLength) {
        String safe = value == null || value.isBlank() ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static void closeQuietly(RandomAccessFile connection) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (IOException ignored) { }
    }

    private record ActivitySpec(String details, String state, long startedAt) { }
    private record Frame(int opcode, String payload) { }
}
