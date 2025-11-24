package com.code.wlu.cp470.wellnest.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Result wrapper for friend request operations so callers can differentiate
 * between success, local persistence issues, cloud failures, and Play Services
 * outages.
 */
public final class FriendRequestResult {

    public enum Status {
        SUCCESS,
        LOCAL_FAILURE,
        REMOTE_FAILURE,
        PLAY_SERVICES_UNAVAILABLE
    }

    private final Status status;
    @Nullable
    private final String message;
    @Nullable
    private final Exception exception;

    private FriendRequestResult(@NonNull Status status,
                                @Nullable String message,
                                @Nullable Exception exception) {
        this.status = status;
        this.message = message;
        this.exception = exception;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    public static FriendRequestResult success() {
        return new FriendRequestResult(Status.SUCCESS, null, null);
    }

    public static FriendRequestResult success(@Nullable String message) {
        return new FriendRequestResult(Status.SUCCESS, message, null);
    }

    public static FriendRequestResult localFailure(@Nullable String message,
                                                   @Nullable Exception exception) {
        return new FriendRequestResult(Status.LOCAL_FAILURE, message, exception);
    }

    public static FriendRequestResult remoteFailure(@Nullable String message,
                                                    @Nullable Exception exception) {
        return new FriendRequestResult(Status.REMOTE_FAILURE, message, exception);
    }

    public static FriendRequestResult playServicesUnavailable(@Nullable String message,
                                                              @Nullable Exception exception) {
        return new FriendRequestResult(Status.PLAY_SERVICES_UNAVAILABLE, message, exception);
    }
}
