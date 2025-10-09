package com.code.wlu.cp470.wellnest.data.local.datastore;

import androidx.annotation.NonNull;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import com.code.wlu.cp470.wellnest.proto.UserProfile;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class UserProfileSerializer implements Serializer<UserProfile> {

    @NonNull
    @Override
    public UserProfile getDefaultValue() {
        return UserProfile.getDefaultInstance();
    }

    @Override
    public Object readFrom(@NonNull InputStream inputStream,
                           @NonNull Continuation<? super UserProfile> continuation) {
        try {
            return UserProfile.parseFrom(inputStream);
        } catch (InvalidProtocolBufferException e) {
            // DataStore expects this exact type to be thrown on corruption
            throw new RuntimeException(new CorruptionException("Cannot read UserProfile proto.", e));
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading UserProfile proto.", e);
        }
    }

    @Override
    public Object writeTo(UserProfile value,
                          @NonNull OutputStream outputStream,
                          @NonNull Continuation<? super Unit> continuation) {
        try {
            value.writeTo(outputStream);
            return Unit.INSTANCE;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while writing UserProfile proto.", e);
        }
    }
}
