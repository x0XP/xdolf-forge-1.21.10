package com.x0xp.xdolf.rpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Local desktop IPC only. All blocking I/O stays off Minecraft's thread. */
public final class DiscordPresence implements AutoCloseable {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private volatile String activity;
    private volatile Session session;
    private String appId = "";

    public synchronized void update(String id, String json) {
        activity = json;
        if (!id.matches("[0-9]{17,20}")) { close(); return; }
        if (session != null && id.equals(appId)) return;
        close(); appId = id;
        Session next = new Session(); session = next;
        Thread worker = new Thread(() -> run(next, id), "Xdolf Discord RPC");
        worker.setDaemon(true); worker.start();
    }

    private void run(Session owner, String id) {
        while (!owner.closed) {
            try {
                connect(owner);
                JsonObject hello = new JsonObject(); hello.addProperty("v", 1); hello.addProperty("client_id", id);
                owner.write(0, hello.toString());
                // Read responses continuously, including Discord ping frames.
                Thread reader = new Thread(() -> {
                    try {
                        while (!owner.closed) {
                            int op = Integer.reverseBytes(owner.input.readInt());
                            int size = Integer.reverseBytes(owner.input.readInt());
                            if (size < 0 || size > 1024 * 1024) throw new IOException("Invalid RPC frame");
                            byte[] data = owner.input.readNBytes(size);
                            if (data.length != size || op == 2) throw new EOFException();
                            if (op == 3) owner.write(4, new String(data, StandardCharsets.UTF_8));
                            if (op == 1) {
                                var response = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
                                if (response.has("evt") && !response.get("evt").isJsonNull() && "READY".equals(response.get("evt").getAsString())) {
                                    owner.ready = true;
                                    LOGGER.info("Xdolf Discord RPC connected");
                                } else if (response.has("evt") && !response.get("evt").isJsonNull() && "ERROR".equals(response.get("evt").getAsString())) {
                                    LOGGER.warn("Xdolf Discord RPC rejected request: {}", response.get("data"));
                                }
                            }
                        }
                    } catch (IOException | RuntimeException failure) {
                        if (!owner.closed) LOGGER.debug("Xdolf Discord RPC reader disconnected", failure);
                        owner.disconnect();
                    }
                }, "Xdolf Discord reader");
                reader.setDaemon(true); reader.start();
                String sent = null;
                long lastSent = 0;
                while (!owner.closed && owner.connection != null) {
                    if (owner.ready && !java.util.Objects.equals(sent, activity)
                        && (lastSent == 0 || System.nanoTime() - lastSent >= 15_000_000_000L)) {
                        JsonObject args = new JsonObject(); args.addProperty("pid", ProcessHandle.current().pid());
                        args.add("activity", JsonParser.parseString(activity));
                        JsonObject command = new JsonObject(); command.addProperty("cmd", "SET_ACTIVITY");
                        command.add("args", args); command.addProperty("nonce", UUID.randomUUID().toString());
                        owner.write(1, command.toString()); sent = activity;
                        lastSent = System.nanoTime();
                    }
                    Thread.sleep(100);
                }
                reader.join();
            } catch (IOException | InterruptedException | RuntimeException failure) {
                if (!owner.closed) LOGGER.debug("Xdolf Discord RPC unavailable; retrying in 15 seconds", failure);
                owner.disconnect();
            }
            if (!owner.closed) try { Thread.sleep(15000); } catch (InterruptedException ignored) { return; }
        }
    }

    private static void connect(Session owner) throws IOException {
        for (int index = 0; index < 10 && !owner.closed; index++) {
            try {
                if (System.getProperty("os.name").startsWith("Windows")) {
                    RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw");
                    synchronized (owner) {
                        if (owner.closed) { pipe.close(); return; }
                        owner.connection = pipe;
                        owner.input = new DataInputStream(new InputStream() {
                            public int read() throws IOException { return pipe.read(); }
                            public int read(byte[] bytes, int offset, int length) throws IOException {
                                return pipe.read(bytes, offset, length);
                            }
                        });
                        owner.output = new OutputStream() {
                            public void write(int value) throws IOException { pipe.write(value); }
                            public void write(byte[] bytes, int offset, int length) throws IOException {
                                pipe.write(bytes, offset, length);
                            }
                        };
                    }
                } else {
                    String directory = System.getenv("XDG_RUNTIME_DIR");
                    if (directory == null) directory = System.getenv("TMPDIR");
                    if (directory == null) directory = "/tmp";
                    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    try { channel.connect(UnixDomainSocketAddress.of(directory + "/discord-ipc-" + index)); }
                    catch (IOException error) { channel.close(); throw error; }
                    synchronized (owner) {
                        if (owner.closed) { channel.close(); return; }
                        owner.connection = channel;
                        // Channel stream adapters share a blocking lock; use direct
                        // reads/writes so a waiting reader cannot stall updates.
                        owner.input = new DataInputStream(new InputStream() {
                            public int read() throws IOException {
                                byte[] one = new byte[1];
                                return read(one, 0, 1) < 0 ? -1 : one[0] & 255;
                            }
                            public int read(byte[] bytes, int offset, int length) throws IOException {
                                return channel.read(java.nio.ByteBuffer.wrap(bytes, offset, length));
                            }
                        });
                        owner.output = new OutputStream() {
                            public void write(int value) throws IOException { write(new byte[]{(byte) value}); }
                            public void write(byte[] bytes, int offset, int length) throws IOException {
                                var buffer = java.nio.ByteBuffer.wrap(bytes, offset, length);
                                while (buffer.hasRemaining()) channel.write(buffer);
                            }
                        };
                    }
                }
                return;
            } catch (IOException unavailable) { owner.disconnect(); }
        }
        throw new IOException("Discord desktop IPC unavailable");
    }

    public synchronized void close() {
        Session old = session; session = null;
        if (old != null) {
            old.closed = true;
            Thread cleanup = new Thread(old::disconnect, "Xdolf Discord cleanup");
            cleanup.setDaemon(true);
            cleanup.start();
        }
    }

    private static final class Session {
        volatile boolean closed, ready;
        volatile Closeable connection;
        DataInputStream input;
        OutputStream output;
        synchronized void write(int opcode, String json) throws IOException {
            if (connection == null || closed) throw new EOFException();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream frame = new ByteArrayOutputStream(bytes.length + 8);
            DataOutputStream stream = new DataOutputStream(frame);
            stream.writeInt(Integer.reverseBytes(opcode)); stream.writeInt(Integer.reverseBytes(bytes.length));
            stream.write(bytes);
            output.write(frame.toByteArray()); output.flush();
        }
        void disconnect() {
            Closeable old = connection; connection = null; ready = false;
            if (old != null) try { old.close(); } catch (IOException ignored) {}
        }
    }
}
