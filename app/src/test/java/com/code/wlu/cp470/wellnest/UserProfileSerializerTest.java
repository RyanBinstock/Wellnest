package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.code.wlu.cp470.wellnest.data.local.datastore.UserProfileSerializer;
import com.code.wlu.cp470.wellnest.proto.UserProfile;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Simple, minimal tests for UserProfileSerializer.
 * <p>
 * What we test:
 * 1) Can we write a UserProfile to bytes and read it back? (round-trip)
 * 2) Does getDefaultValue() return sane defaults (empty strings, false, 0)?
 * 3) EMPTY bytes -> valid default message (no exception).
 * 4) Corrupt bytes -> RuntimeException (wrapped CorruptionException).
 * <p>
 * Notes for teammates:
 * - In the real app, DataStore handles all of this automatically.
 * - These tests just prove that our read/write logic works as expected.
 */
public class UserProfileSerializerTest {

    private static final Continuation<UserProfile> CONT_PROFILE = new NoopContinuation<>();
    private static final Continuation<Unit> CONT_UNIT = new NoopContinuation<>();

    // --- Helper: Build a sample UserProfile with test data ---
    private static UserProfile sampleProfile() {
        return UserProfile.newBuilder()
                .setUid("abc123")
                .setDisplayName("Test User")
                .setEmail("test@example.com")
                .setPhotoUrl("https://example.com/pic.jpg")
                .setOnboardingComplete(true)
                .setUpdatedAtMs(1_690_000_000_000L)
                .build();
    }

    /**
     * Test that writing and then reading returns the exact same UserProfile.
     */
    @Test
    public void writeThenRead_roundTripsSuccessfully() {
        UserProfileSerializer serializer = new UserProfileSerializer();

        // Write profile to a byte array (like saving to disk)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UserProfile original = sampleProfile();
        Object writeResult = serializer.writeTo(original, out, CONT_UNIT);
        assertSame("writeTo should return Unit.INSTANCE", Unit.INSTANCE, writeResult);

        // Read back from those same bytes (like loading from disk)
        byte[] bytes = out.toByteArray();
        InputStream in = new ByteArrayInputStream(bytes);
        Object readObj = serializer.readFrom(in, CONT_PROFILE);
        assertTrue("readFrom should return a UserProfile", readObj instanceof UserProfile);

        UserProfile restored = (UserProfile) readObj;

        // Proto messages implement value-based equals()
        assertEquals(original, restored);

        // Spot-check a few fields for clarity
        assertEquals("abc123", restored.getUid());
        assertEquals("Test User", restored.getDisplayName());
        assertTrue(restored.getOnboardingComplete());
        assertEquals(1_690_000_000_000L, restored.getUpdatedAtMs());
    }

    /**
     * Empty bytes -> protobuf returns default message (no exception).
     */
    @Test
    public void readFrom_withEmptyBytes_returnsDefault() {
        UserProfileSerializer serializer = new UserProfileSerializer();

        byte[] empty = new byte[0];
        InputStream in = new ByteArrayInputStream(empty);

        Object readObj = serializer.readFrom(in, CONT_PROFILE);
        assertTrue(readObj instanceof UserProfile);

        UserProfile def = (UserProfile) readObj;

        // Proto3 defaults: empty strings, false, and 0 for numbers
        assertEquals("", def.getUid());
        assertEquals("", def.getDisplayName());
        assertEquals("", def.getEmail());
        assertEquals("", def.getPhotoUrl());
        assertFalse(def.getOnboardingComplete());
        assertEquals(0L, def.getUpdatedAtMs());
    }

    /**
     * Corrupt bytes (invalid varint pattern) should throw RuntimeException.
     */
    @Test(expected = RuntimeException.class)
    public void readFrom_withCorruptBytes_throwsRuntimeException() {
        UserProfileSerializer serializer = new UserProfileSerializer();

        // Invalid byte pattern guaranteed to break protobuf parsing
        byte[] corrupt = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F};
        InputStream in = new ByteArrayInputStream(corrupt);

        serializer.readFrom(in, CONT_PROFILE); // expect RuntimeException(CorruptionException)
    }

    /**
     * Check that getDefaultValue() returns a "blank" UserProfile.
     */
    @Test
    public void getDefaultValue_returnsBlankProfile() {
        UserProfileSerializer serializer = new UserProfileSerializer();
        UserProfile def = serializer.getDefaultValue();

        assertEquals("", def.getUid());
        assertEquals("", def.getDisplayName());
        assertEquals("", def.getEmail());
        assertEquals("", def.getPhotoUrl());
        assertFalse(def.getOnboardingComplete());
        assertEquals(0L, def.getUpdatedAtMs());
    }

    // --- Simple no-op Continuations (required by the interface; unused in tests) ---
    private static final class NoopContinuation<T> implements Continuation<T> {
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(Object result) { /* no-op */ }
    }
}
