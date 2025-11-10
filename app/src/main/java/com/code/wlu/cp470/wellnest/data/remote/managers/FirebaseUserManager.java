package com.code.wlu.cp470.wellnest.data.remote.managers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation with NO implicit user.
 * All methods require explicit owner/source/target UIDs.
 * <p>
 * Storage layout (per owner user):
 * users/{ownerUid}/user_profile/{uid}
 * users/{ownerUid}/global_score/{uid} { uid, score }
 * users/{ownerUid}/streak/1           { count }
 * users/{ownerUid}/friends/{friendUid} { friend_uid, friend_name, friend_status }
 * users/{ownerUid}/badges/{badgeId}   { badge_id }
 * <p>
 * All methods are synchronous via Tasks.await(). Call from a background thread.
 */
public class FirebaseUserManager {

    private static final String TAG = "FirebaseUserManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ---------------------- user_profile ----------------------

    public boolean hasUserProfile(@Nullable String uid, @Nullable String email) throws ExecutionException, InterruptedException {
        CollectionReference users = db.collection("users");
        if (uid == null && email == null) {
            throw new IllegalArgumentException("uid and email cannot both be null");
        }
        if (uid != null) {
            if (uid.isEmpty()) {
                throw new IllegalArgumentException("uid cannot be empty");
            }
            DocumentSnapshot userDoc = Tasks.await(users.document(uid).get());
            return userDoc != null && userDoc.exists();
        } else {
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }
            QuerySnapshot querySnapshot = Tasks.await(users.whereEqualTo(UserContract.UserProfile.Col.EMAIL, email).get());
            return !querySnapshot.isEmpty();
        }
    }

    public UserProfile getUser(@Nullable String uid, @Nullable String email) throws ExecutionException, InterruptedException {
        CollectionReference users = db.collection("users");
        DocumentSnapshot userDoc;
        if (uid == null && email == null) {
            throw new IllegalArgumentException("uid and email cannot both be null");
        }
        if (uid != null) {
            if (uid.isEmpty()) {
                throw new IllegalArgumentException("uid cannot be empty");
            }
            userDoc = Tasks.await(users.document(uid).get());

        } else {
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }
            userDoc = Tasks.await(users.whereEqualTo(UserContract.UserProfile.Col.EMAIL, email).get()).getDocuments().get(0);
        }
        String userUid = userDoc.getString(UserContract.UserProfile.Col.UID);
        String userName = userDoc.getString(UserContract.UserProfile.Col.NAME);
        String userEmail = userDoc.getString(UserContract.UserProfile.Col.EMAIL);
        return new UserProfile(userUid, userName, userEmail);
    }

    // ---------------------- Friends ----------------------

    public boolean addFriendRequest(@NonNull String ownerUid, @NonNull String friendUid, @NonNull String friendName) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        try {
            // Reference: /users/{ownerUid}/friends/{friendUid}
            DocumentReference friendDoc = db.collection("users")
                    .document(ownerUid)
                    .collection(UserContract.Friends.TABLE)
                    .document(friendUid);

            // Create the friend entry
            Map<String, Object> data = new HashMap<>();
            data.put(UserContract.Friends.Col.FRIEND_UID, friendUid);
            data.put(UserContract.Friends.Col.FRIEND_NAME, friendName);
            data.put(UserContract.Friends.Col.FRIEND_STATUS, "pending"); // default status

            // Write to Firestore (blocking)
            Tasks.await(friendDoc.set(data, SetOptions.merge()));
            return true;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean acceptFriend(@NonNull String ownerUid, @NonNull String friendUid) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        try {
            // Reference: /users/{ownerUid}/friends/{friendUid}
            DocumentReference friendDoc = db.collection("users")
                    .document(ownerUid)
                    .collection(UserContract.Friends.TABLE)
                    .document(friendUid);

            // Update only the status field
            Map<String, Object> update = new HashMap<>();
            update.put(UserContract.Friends.Col.FRIEND_STATUS, "accepted");

            // Update in Firestore (blocking)
            Tasks.await(friendDoc.update(update));

            return true;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            // Handles case where document might not exist
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFriend(@NonNull String ownerUid, @NonNull String friendUid) {
        if (ownerUid.isEmpty() || friendUid.isEmpty())
            throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");

        try {
            // Reference: /users/{ownerUid}/friends/{friendUid}
            DocumentReference friendDoc = db.collection("users")
                    .document(ownerUid)
                    .collection(UserContract.Friends.TABLE)
                    .document(friendUid);

            // Delete document (blocking)
            Tasks.await(friendDoc.delete());

            return true;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<Friend> getFriends(@NonNull String ownerUid) {
        List<Friend> out = new ArrayList<>();
        if (ownerUid.isEmpty()) return out;

        // Get friends collection: /users/{ownerUid}/friends
        CollectionReference friendsCol = db.collection("users")
                .document(ownerUid)
                .collection(UserContract.Friends.TABLE);
        try {
            // Get all friend documents (blocking)
            QuerySnapshot friendsSnap = Tasks.await(friendsCol.get());
            if (friendsSnap == null) return out;

            Map<String, Friend> friendMap = new HashMap<>();

            // Build friend list from friend documents
            for (DocumentSnapshot d : friendsSnap.getDocuments()) {
                String uid = d.getString(UserContract.Friends.Col.FRIEND_UID);
                String name = d.getString(UserContract.Friends.Col.FRIEND_NAME);
                String status = d.getString(UserContract.Friends.Col.FRIEND_STATUS);
                if (status == null) status = "pending";
                if (uid != null) {
                    friendMap.put(uid, new Friend(uid, name, status, 0));
                }
            }

            // Get scores: /users/{ownerUid}/globalScore
            CollectionReference scoreCol = db.collection("users")
                    .document(ownerUid)
                    .collection(UserContract.GlobalScore.TABLE);

            QuerySnapshot scoreSnap = Tasks.await(scoreCol.get());
            if (scoreSnap != null) {
                for (DocumentSnapshot d : scoreSnap.getDocuments()) {
                    String uid = d.getId();
                    Number n = d.getDouble(UserContract.GlobalScore.Col.SCORE);
                    int score = (n == null) ? 0 : n.intValue();
                    Friend f = friendMap.get(uid);
                    if (f != null) f.setScore(score);
                }
            }

            out.addAll(friendMap.values());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return out;
    }
}

