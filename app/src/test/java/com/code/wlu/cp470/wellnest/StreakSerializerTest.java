package com.code.wlu.cp470.wellnest;

import com.code.wlu.cp470.wellnest.data.local.datastore.StreakSerializer;
import com.code.wlu.cp470.wellnest.proto.Streak;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

import static org.junit.Assert.*;

/**
 * Simple, easy-to-understand tests for StreakSerializer.
 *
 * What this proves:
 *  1) We can write a Streak to bytes and read it back exactly (round trip).
 *  2) The default value is sane (all zeros for int fields).
 *  3) Corrupt/empty bytes currently throw a RuntimeException (as per the serializer).
 *
 * Notes for teammates (no DataStore knowledge needed):
 *  - In the *real app*, DataStore calls readFrom()/writeTo() for us.
 *  - Here we call them directly so we can verify their behavior.
 */
public class StreakSerializerTest {

    // --- Tiny no-op Continuation stubs (required by the interface; unused in these tests) ---
    private static final class NoopContinuation<T> implements Continuation<T> {
        @Override public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
        @Override public void resumeWith(Object result) { /* no-op */ }
    }
    private static final Continuation<Streak> CONT_STREAK = new NoopContinuation<>();
    private static final Continuation<Unit>   CONT_UNIT   = new NoopContinuation<>();

    // Helper factory for a sample Streak that matches YOUR proto fields
    private static Streak sampleStreak() {
        return Streak.newBuilder()
                .setCurrent(5)
                .setBest(12)
                .setLastYyyymmdd(20251008) // YYYYMMDD as an int
                .build();
    }

    /**
     * Happy path: write a Streak to an OutputStream, then read it back from an InputStream.
     * Expectation: all field values survive the round trip unchanged.
     */
    @Test
    public void writeThenRead_roundTripsSuccessfully() {
        StreakSerializer serializer = new StreakSerializer();

        // 1) Serialize to bytes (like saving to disk)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streak original = sampleStreak();
        Object writeResult = serializer.writeTo(original, out, CONT_UNIT);
        assertSame("writeTo should return Unit.INSTANCE", Unit.INSTANCE, writeResult);

        // 2) Deserialize from those bytes (like loading from disk)
        byte[] bytes = out.toByteArray();
        InputStream in = new ByteArrayInputStream(bytes);
        Object readObj = serializer.readFrom(in, CONT_STREAK);
        assertTrue("readFrom should return a Streak", readObj instanceof Streak);

        Streak restored = (Streak) readObj;

        // Proto messages use value-based equals(); this should be exactly equal.
        assertEquals("Round-tripped Streak should equal the original", original, restored);

        // Also assert individual fields for clarity
        assertEquals(5, restored.getCurrent());
        assertEquals(12, restored.getBest());
        assertEquals(20251008, restored.getLastYyyymmdd());
    }

    /**
     * Default value test:
     * If no file exists yet (e.g., first launch), DataStore asks the serializer for a default.
     * Proto3 defaults: all int fields are 0.
     */
    @Test
    public void getDefaultValue_returnsZeros() {
        StreakSerializer serializer = new StreakSerializer();
        Streak def = serializer.getDefaultValue();

        assertEquals(0, def.getCurrent());
        assertEquals(0, def.getBest());
        assertEquals(0, def.getLastYyyymmdd());
    }

    /**
     * Current behavior in your serializer:
     * - readFrom() wraps parse errors as RuntimeException(CorruptionException).
     * This test simulates a corrupt/empty file.
     *
     * If you later decide to "return default on parse error", change this test to
     * assert a default value instead of expecting an exception.
     */
    @Test(expected = RuntimeException.class)
    public void readFrom_withCorruptBytes_throwsRuntimeException() {
        StreakSerializer serializer = new StreakSerializer();

        // Empty byte array stands in for a corrupt/invalid file
        byte[] corrupt = new byte[0];
        InputStream in = new ByteArrayInputStream(corrupt);

        serializer.readFrom(in, CONT_STREAK); // should throw
    }
}
