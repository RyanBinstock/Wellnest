package com.code.wlu.cp470.wellnest.data.local.datastore;

import androidx.annotation.NonNull;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import com.code.wlu.cp470.wellnest.proto.Streak;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class StreakSerializer implements Serializer<Streak> {

    @NonNull
    @Override
    public Streak getDefaultValue() {
        return Streak.getDefaultInstance();
    }

    @Override
    public Object readFrom(@NonNull InputStream inputStream,
                           @NonNull Continuation<? super Streak> continuation) {
        try {
            return Streak.parseFrom(inputStream);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(new CorruptionException("Cannot read Streak proto.", e));
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Streak proto.", e);
        }
    }

    @Override
    public Object writeTo(Streak value,
                          @NonNull OutputStream outputStream,
                          @NonNull Continuation<? super Unit> continuation) {
        try {
            value.writeTo(outputStream);
            return Unit.INSTANCE;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while writing Streak proto.", e);
        }
    }
}
