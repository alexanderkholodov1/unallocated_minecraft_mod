package org.unallocated.interfacefx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Write-ahead, byte-preserving options transaction. No Minecraft dependencies. */
public final class InterfaceOptionsJournal implements AutoCloseable {
    private static final int MAGIC = 0x55494631; // UIF1
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path options;
    private final Path journal;
    private final FileChannel lockChannel;
    private final FileLock lock;

    public InterfaceOptionsJournal(Path directory) throws IOException {
        Files.createDirectories(directory);
        options = directory.resolve("options.txt");
        journal = directory.resolve(".unallocated-interface-options.journal");
        lockChannel = FileChannel.open(directory.resolve(".unallocated-interface-options.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
        } catch (RuntimeException | IOException failure) {
            lockChannel.close();
            throw new IOException("Options transaction is already owned or unavailable", failure);
        }
        if (acquired == null) {
            lockChannel.close();
            throw new IOException("Another client owns this options transaction");
        }
        lock = acquired;
    }

    /** Must complete successfully before the first in-memory or on-disk change. */
    public synchronized void begin() throws IOException {
        if (pending()) {
            readSnapshot(); // Never replace the original, including after a crash.
            return;
        }
        boolean existed = Files.exists(options);
        byte[] original = existed ? Files.readAllBytes(options) : new byte[0];
        if (original.length > MAX_BYTES) throw new IOException("Options file is too large");
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC);
            out.writeBoolean(existed);
            out.writeInt(original.length);
            out.write(original);
        }
        byte[] payload = buffer.toByteArray();
        buffer.writeBytes(digest(payload));
        atomicWrite(journal, buffer.toByteArray());
    }

    public synchronized boolean pending() {
        return Files.exists(journal);
    }

    /** Idempotent: retain the journal until all vanilla shutdown saves have finished. */
    public synchronized void restore() throws IOException {
        if (!pending()) return;
        Snapshot snapshot = readSnapshot();
        if (snapshot.existed) {
            if (!Files.exists(options) || !Arrays.equals(Files.readAllBytes(options), snapshot.bytes)) {
                atomicWrite(options, snapshot.bytes);
            }
        } else {
            Files.deleteIfExists(options);
        }
    }

    /** Only retire after restore succeeded; an interrupted restore can always be replayed. */
    public synchronized void finish() throws IOException {
        restore();
        Files.deleteIfExists(journal);
    }

    private Snapshot readSnapshot() throws IOException {
        long size = Files.size(journal);
        if (size < 41 || size > MAX_BYTES + 41L) throw new IOException("Invalid options journal size");
        byte[] encoded = Files.readAllBytes(journal);
        byte[] payload = Arrays.copyOf(encoded, encoded.length - 32);
        if (!MessageDigest.isEqual(digest(payload), Arrays.copyOfRange(encoded, payload.length, encoded.length))) {
            throw new IOException("Options journal checksum mismatch; original retained");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported options journal");
            boolean existed = in.readBoolean();
            int length = in.readInt();
            if (length < 0 || length > MAX_BYTES || length != in.available() || (!existed && length != 0)) {
                throw new IOException("Invalid options journal payload");
            }
            return new Snapshot(existed, in.readNBytes(length));
        }
    }

    private static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        // Same filesystem, fully flushed before publication. No non-atomic fallback.
        Path staging = target.resolveSibling(target.getFileName() + ".staging");
        try (var channel = FileChannel.open(staging, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        Files.move(staging, target, ATOMIC_MOVE, REPLACE_EXISTING);
    }

    @Override
    public synchronized void close() throws IOException {
        // Closing a crashed owner does not implicitly commit/restore the transaction.
        lock.release();
        lockChannel.close();
    }

    private record Snapshot(boolean existed, byte[] bytes) { }
}
