package com.code.wlu.cp470.wellnest.data.remote.managers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Firestore manager (synchronous). Call every method from a background thread.
 * <p>
 * Schema used here:
 * users/{uid}  (root document)
 * - NAME (UserContract.UserProfile.Col.NAME)
 * - EMAIL (UserContract.UserProfile.Col.EMAIL)
 * - GLOBAL_SCORE (UserContract.GlobalScore.Col.SCORE)   // optional
 * - STREAK (UserContract.Streak.Col.COUNT)              // optional
 * friends/{friendUid} (subcollection)
 * - FRIEND_UID
 * - FRIEND_NAME
 * - FRIEND_STATUS ("pending" | "accepted")
 */
public class FirebaseUserManager {

    private static final String TAG = "FirebaseUserManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static boolean awaitOk(Task<?> t) {
        try {
            Tasks.await(t);
            return true;
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException f = (FirebaseFirestoreException) c;
                Log.e(TAG, "awaitOk failed: " + f.getCode() + " / " + f.getMessage(), f);
            } else {
                Log.e(TAG, "awaitOk failed (exec)", e);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "awaitOk interrupted", e);
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // User profile (root: users/{uid})
    // ---------------------------------------------------------------------

    /**
     * Upsert root user doc with name/email (does not overwrite score/streak).
     */
    public boolean upsertUserProfile(@NonNull String uid,
                                     @NonNull String name,
                                     @NonNull String email) {
        if (uid.isEmpty() || name.isEmpty() || email.isEmpty()) {
            throw new IllegalArgumentException("uid, name, and email cannot be empty");
        }
        DocumentReference userDoc = db.collection("users").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put(UserContract.UserProfile.Col.NAME, name);
        data.put(UserContract.UserProfile.Col.EMAIL, email);

        Log.d(TAG, "upsertUserProfile: " + data);
        boolean ok = awaitOk(userDoc.set(data, SetOptions.merge()));
        Log.d(TAG, "upsertUserProfile success: " + ok);
        return ok;
    }

    /**
     * True if the root users/{uid} exists or a users query by email finds a match.
     */
    public boolean hasUserProfile(@Nullable String uid, @Nullable String email)
            throws ExecutionException, InterruptedException {
        CollectionReference users = db.collection("users");

        if (uid == null && email == null) {
            throw new IllegalArgumentException("uid and email cannot both be null");
        }
        if (uid != null) {
            if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
            DocumentSnapshot userDoc = Tasks.await(users.document(uid).get());
            return userDoc != null && userDoc.exists();
        } else {
            if (email == null || email.isEmpty())
                throw new IllegalArgumentException("email cannot be empty");
            QuerySnapshot snap = Tasks.await(
                    users.whereEqualTo(UserContract.UserProfile.Col.EMAIL, email).get());
            return snap != null && !snap.isEmpty();
        }
    }

    /**
     * Fetch a user either by uid (preferred) or by email. Returns null if not found.
     */
    @Nullable
    public UserProfile getUser(@Nullable String uid, @Nullable String email)
            throws ExecutionException, InterruptedException {
        CollectionReference users = db.collection("users");
        DocumentSnapshot doc;

        if (uid == null && email == null) {
            throw new IllegalArgumentException("uid and email cannot both be null");
        }

        if (uid != null) {
            if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
            doc = Tasks.await(users.document(uid).get());
        } else {
            if (email == null || email.isEmpty())
                throw new IllegalArgumentException("email cannot be empty");
            Query q = users.whereEqualTo(UserContract.UserProfile.Col.EMAIL, email);
            QuerySnapshot qs = Tasks.await(q.get());
            if (qs == null || qs.isEmpty()) return null;
            doc = qs.getDocuments().get(0);
        }

        if (doc == null || !doc.exists()) return null;

        String outUid = doc.getId(); // use document id as uid
        String outName = doc.getString(UserContract.UserProfile.Col.NAME);
        String outEmail = doc.getString(UserContract.UserProfile.Col.EMAIL);
        return new UserProfile(outUid, outName, outEmail);
    }


    // Optional convenience methods for score/streak on root user doc
    public Integer getGlobalScore(@NonNull String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot d = Tasks.await(db.collection("users").document(uid).get());
        if (d == null || !d.exists()) return null;
        Number n = (Number) d.get(UserContract.GlobalScore.Col.SCORE);
        return (n == null) ? null : n.intValue();
    }

    public boolean setGlobalScore(@NonNull String uid, int score) {
        if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
        DocumentReference userDoc = db.collection("users").document(uid);
        Map<String, Object> data = new HashMap<>();
        data.put(UserContract.GlobalScore.Col.SCORE, score);
        return awaitOk(userDoc.set(data, SetOptions.merge()));
    }

    public Integer getStreak(@NonNull String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot d = Tasks.await(db.collection("users").document(uid).get());
        if (d == null || !d.exists()) return null;
        Number n = (Number) d.get(UserContract.Streak.Col.COUNT);
        return (n == null) ? null : n.intValue();
    }

    public boolean setStreak(@NonNull String uid, int count) {
        if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
        DocumentReference userDoc = db.collection("users").document(uid);
        Map<String, Object> data = new HashMap<>();
        data.put(UserContract.Streak.Col.COUNT, count);
        return awaitOk(userDoc.set(data, SetOptions.merge()));
    }

    // ---------------------------------------------------------------------
    // Friends: users/{ownerUid}/friends/{friendUid}
    // ---------------------------------------------------------------------

    /**
     * Inserts/merges a friend entry with default status=pending.
     */
    public boolean addFriendRequest(@NonNull String ownerUid,
                                    @NonNull String friendUid,
                                    @NonNull String friendName) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        DocumentReference friendDoc = db.collection("users")
                .document(ownerUid)
                .collection(UserContract.Friends.TABLE)
                .document(friendUid);

        Map<String, Object> data = new HashMap<>();
        data.put(UserContract.Friends.Col.FRIEND_UID, friendUid);
        data.put(UserContract.Friends.Col.FRIEND_NAME, friendName);
        data.put(UserContract.Friends.Col.FRIEND_STATUS, "pending");

        return awaitOk(friendDoc.set(data, SetOptions.merge()));
    }

    /**
     * Sets status=accepted for the friend entry.
     */
    public boolean acceptFriend(@NonNull String ownerUid, @NonNull String friendUid) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        DocumentReference friendDoc = db.collection("users")
                .document(ownerUid)
                .collection(UserContract.Friends.TABLE)
                .document(friendUid);

        Map<String, Object> update = new HashMap<>();
        update.put(UserContract.Friends.Col.FRIEND_STATUS, "accepted");

        return awaitOk(friendDoc.update(update));
    }

    /**
     * Deletes the friend entry.
     */
    public boolean removeFriend(@NonNull String ownerUid, @NonNull String friendUid) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        DocumentReference friendDoc = db.collection("users")
                .document(ownerUid)
                .collection(UserContract.Friends.TABLE)
                .document(friendUid);

        return awaitOk(friendDoc.delete());
    }

    /**
     * Returns the owner's friends list, enriching with each friend's GLOBAL_SCORE
     * from their root user doc (if present). Missing scores default to 0.
     */
    public List<Friend> getFriends(@NonNull String ownerUid) {
        List<Friend> out = new ArrayList<>();
        if (ownerUid.isEmpty()) return out;

        CollectionReference friendsCol = db.collection("users")
                .document(ownerUid)
                .collection(UserContract.Friends.TABLE);

        try {
            QuerySnapshot friendsSnap = Tasks.await(friendsCol.get());
            if (friendsSnap == null) return out;

            // Map of friendUid → Friend object
            Map<String, Friend> map = new HashMap<>();
            List<Task<DocumentSnapshot>> pendingUserGets = new ArrayList<>();

            for (DocumentSnapshot d : friendsSnap.getDocuments()) {
                String uid = d.getString(UserContract.Friends.Col.FRIEND_UID);
                String name = d.getString(UserContract.Friends.Col.FRIEND_NAME);
                String status = d.getString(UserContract.Friends.Col.FRIEND_STATUS);
                if (uid == null) continue;
                if (status == null) status = "pending";
                map.put(uid, new Friend(uid, name, status, 0));

                // Fetch root user doc later to read their score
                pendingUserGets.add(db.collection("users").document(uid).get());
            }

            if (!pendingUserGets.isEmpty()) {
                List<Object> results = Tasks.await(Tasks.whenAllSuccess(pendingUserGets));
                for (Object obj : results) {
                    DocumentSnapshot ud = (DocumentSnapshot) obj;
                    if (ud != null && ud.exists()) {
                        String fid = ud.getId();
                        Number n = (Number) ud.get(UserContract.GlobalScore.Col.SCORE);
                        int score = (n == null) ? 0 : n.intValue();
                        Friend f = map.get(fid);
                        if (f != null) f.setScore(score);
                    }
                }
            }

            out.addAll(map.values());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "getFriends failed", e);
            Thread.currentThread().interrupt();
        }
        return out;
    }


    // ---------------------------------------------------------------------
    // Optional helper: find a user's UID by email (first match).
    // ---------------------------------------------------------------------
    @Nullable
    public String findUidByEmail(@NonNull String email)
            throws ExecutionException, InterruptedException {
        if (email.isEmpty()) return null;
        QuerySnapshot qs = Tasks.await(db.collection("users")
                .whereEqualTo(UserContract.UserProfile.Col.EMAIL, email)
                .limit(1)
                .get());
        if (qs == null || qs.isEmpty()) return null;
        return qs.getDocuments().get(0).getId();
    }
}
