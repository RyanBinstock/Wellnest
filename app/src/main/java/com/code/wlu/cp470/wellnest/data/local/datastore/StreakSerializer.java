package com.code.wlu.cp470.wellnest.data.local.datastore;

// import proto's generated Streak class
import com.code.wlu.cp470.wellnest.proto.Streak;

import androidx.annotation.NonNull;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * Handles saving and loading the user's streak data (current streak, longest streak, last active date)
 * to and from a small file on the device.
 *
 * Think of this as a translator that converts our Streak object into raw bytes (to save),
 * and back from raw bytes into a Streak object (to load).
 *
 * Used automatically by DataStore. You will rarely need to call these methods directly.
 */
public final class StreakSerializer implements Serializer<Streak> {

    /**
     * Called by DataStore if the streak file doesn't exist or can't be read.
     * Returns a default Streak object with all fields set to zero or empty.
     *
     * Example: new users or first app launch start with this default value.
     */
    @NonNull
    @Override
    public Streak getDefaultValue() {
        return Streak.getDefaultInstance();
    }

    /**
     * Called automatically by DataStore whenever it needs to read the streak data from disk.
     *
     * Converts the saved binary file (streak.pb) back into a usable Streak object.
     * If the file is missing or corrupt, throws an error so DataStore can reset it.
     *
     * You don’t call this yourself — DataStore handles it when you access streakStore.data().
     */
    @Override
    public Object readFrom(@NonNull InputStream inputStream,
                           @NonNull Continuation<? super Streak> continuation) {
        try {
            // Ensure we can "peek" and then reset
            InputStream in = inputStream;
            if (!in.markSupported()) {
                in = new BufferedInputStream(in);
            }

            // Peek one byte to detect an empty file
            in.mark(1);
            int first = in.read();
            if (first == -1) {
                // Empty stream → treat as corruption so DataStore resets to default
                throw new RuntimeException(new CorruptionException("Empty streak file (0 bytes).", null));
            }
            in.reset();

            // Non-empty → parse normally
            return Streak.parseFrom(in);

        } catch (InvalidProtocolBufferException e) {
            // Bytes exist but can't be parsed as Streak → corruption
            throw new RuntimeException(new CorruptionException("Cannot read Streak proto.", e));
        } catch (IOException e) {
            // Plain I/O problem (disk, stream) → bubble up as runtime for test simplicity
            throw new RuntimeException("I/O error while reading Streak proto.", e);
        }
    }

    /**
     * Called automatically by DataStore whenever the streak data changes and needs to be saved.
     *
     * Converts the Streak object (with updated streak counts or dates)
     * into binary format and writes it to the file (streak.pb) in app storage.
     *
     * You don’t call this directly — DataStore calls it when you use updateDataAsync() or edit().
     */
    @Override
    public Object writeTo(Streak value,
                          @NonNull OutputStream outputStream,
                          @NonNull Continuation<? super Unit> continuation) {
        try {
            // Write the current streak data into the file
            value.writeTo(outputStream);
            return Unit.INSTANCE;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while writing Streak proto.", e);
        }
    }
}