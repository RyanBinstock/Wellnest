package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.code.wlu.cp470.wellnest.data.local.datastore.GlobalScoreSerializer;
import com.code.wlu.cp470.wellnest.proto.GlobalScore;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Simple, minimal tests for GlobalScoreSerializer.
 * <p>
 * What this proves:
 * 1) We can write a GlobalScore to bytes and read it back exactly (round trip).
 * 2) getDefaultValue() is sane (proto3 defaults → ints/longs are 0).
 * 3) EMPTY bytes are treated by protobuf as a valid default message (no exception).
 * 4) Truly corrupt bytes cause a parse error (wrapped as RuntimeException by the serializer).
 * <p>
 * Notes for teammates:
 * - In the real app, DataStore calls readFrom()/writeTo() for us.
 * - Here we call them directly using in-memory streams to verify behavior.
 */
public class GlobalScoreSerializerTest {

    private static final Continuation<GlobalScore> CONT_SCORE = new NoopContinuation<>();
    private static final Continuation<Unit> CONT_UNIT = new NoopContinuation<>();

    // Helper to build a sample GlobalScore matching YOUR proto fields
    private static GlobalScore sampleScore() {
        return GlobalScore.newBuilder()
                .setTotalPoints(1234)
                .setUpdatedAtMs(1_690_000_000_000L) // any stable timestamp
                .build();
    }

    /**
     * Happy path: write → read should preserve the message exactly.
     */
    @Test
    public void writeThenRead_roundTripsSuccessfully() {
        GlobalScoreSerializer serializer = new GlobalScoreSerializer();

        // Write (serialize) to bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GlobalScore original = sampleScore();
        Object writeResult = serializer.writeTo(original, out, CONT_UNIT);
        assertSame("writeTo should return Unit.INSTANCE", Unit.INSTANCE, writeResult);

        // Read (parse) from those same bytes
        byte[] bytes = out.toByteArray();
        InputStream in = new ByteArrayInputStream(bytes);
        Object readObj = serializer.readFrom(in, CONT_SCORE);
        assertTrue("readFrom should return a GlobalScore", readObj instanceof GlobalScore);

        GlobalScore restored = (GlobalScore) readObj;

        // Protobuf messages use value-based equals(); this should match exactly.
        assertEquals(original, restored);
        assertEquals(1234, restored.getTotalPoints());
        assertEquals(1_690_000_000_000L, restored.getUpdatedAtMs());
    }

    /**
     * Proto detail: an EMPTY stream is treated as a valid "default" message.
     * So we expect no exception; fields should be their proto3 defaults (0).
     */
    @Test
    public void readFrom_withEmptyBytes_returnsDefault() {
        GlobalScoreSerializer serializer = new GlobalScoreSerializer();

        byte[] empty = new byte[0];
        InputStream in = new ByteArrayInputStream(empty);

        Object readObj = serializer.readFrom(in, CONT_SCORE);
        assertTrue(readObj instanceof GlobalScore);

        GlobalScore def = (GlobalScore) readObj;
        assertEquals(0, def.getTotalPoints());
        assertEquals(0L, def.getUpdatedAtMs());
    }

    /**
     * Truly corrupt bytes (invalid varint) should throw:
     * This byte pattern forces a protobuf parse error.
     * Your serializer wraps it as RuntimeException.
     */
    @Test(expected = RuntimeException.class)
    public void readFrom_withCorruptBytes_throwsRuntimeException() {
        GlobalScoreSerializer serializer = new GlobalScoreSerializer();

        // Invalid varint pattern → guarantees parse failure
        byte[] corrupt = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F};
        InputStream in = new ByteArrayInputStream(corrupt);

        serializer.readFrom(in, CONT_SCORE); // should throw
    }

    /**
     * Sanity check for defaults returned by getDefaultValue().
     */
    @Test
    public void getDefaultValue_returnsZeros() {
        GlobalScoreSerializer serializer = new GlobalScoreSerializer();
        GlobalScore def = serializer.getDefaultValue();

        assertEquals(0, def.getTotalPoints());
        assertEquals(0L, def.getUpdatedAtMs());
    }

    // --- Tiny no-op Continuation (required by the interface; we don't use it in assertions) ---
    private static final class NoopContinuation<T> implements Continuation<T> {
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(Object result) { /* no-op */ }
    }
}
