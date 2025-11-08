package com.code.wlu.cp470.wellnest.data.remote.managers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of UserInterface.
 * <p>
 * Storage layout (per user):
 * users/{uid}/user_profile/{uid}
 * users/{uid}/global_score/1           { score: INT }
 * users/{uid}/streak/1                 { count: INT }
 * users/{uid}/friends/{friend_uid}     { friend_uid, friend_name, friend_status }
 * users/{uid}/badges/{badge_uid}       { badge_uid }
 * <p>
 * NOTE: All methods are synchronous via Tasks.await(). Call from a background thread.
 */
public class FirebaseUserManager implements UserInterface {

    private static final String TAG = "FirebaseUserManager";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    /**
     * Optional override for acting user UID (else currentUser.uid is used).
     */
    @Nullable
    private final String fixedUid;

    public FirebaseUserManager() {
        this(null);
    }

    public FirebaseUserManager(@Nullable String fixedUid) {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.fixedUid = fixedUid;
    }

    // ---------------------- helpers ----------------------

    private static boolean awaitOk(Task<?> t) {
        try {
            Tasks.await(t);
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "awaitOk failed", e);
            return false;
        }
    }

    private static @Nullable DocumentSnapshot awaitDoc(Task<DocumentSnapshot> t) {
        try {
            return Tasks.await(t);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "awaitDoc failed", e);
            return null;
        }
    }

    private static @Nullable QuerySnapshot awaitQuery(Task<QuerySnapshot> t) {
        try {
            return Tasks.await(t);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "awaitQuery failed", e);
            return null;
        }
    }

    private static @Nullable String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static @NonNull String asStringOr(Object v, String fallback) {
        return v == null ? fallback : v.toString();
    }

    private String requireUid() {
        if (fixedUid != null && !fixedUid.isEmpty()) return fixedUid;
        FirebaseUser u = auth.getCurrentUser();
        if (u == null || u.getUid() == null) {
            throw new IllegalStateException("No signed-in FirebaseUser; cannot resolve UID.");
        }
        return u.getUid();
    }

    private DocumentReference userRoot() {
        return db.collection("users").document(requireUid());
    }

    // ---------------------- user_profile ----------------------

    private CollectionReference col(String tableName) {
        // Store each table as a subcollection under the user doc
        return userRoot().collection(tableName);
    }

    private DocumentReference singleton(CollectionReference col) {
        // Mirror local "CHECK(id=1)" singletons by using docId "1"
        return col.document("1");
    }

    @Override
    public boolean upsertUserProfile(String uid, String name, String email) {
        if (uid == null || uid.isEmpty()) return false;

        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.UserProfile.Col.UID, uid);
        doc.put(UserContract.UserProfile.Col.NAME, name);
        doc.put(UserContract.UserProfile.Col.EMAIL, email);

        return awaitOk(col(UserContract.UserProfile.TABLE).document(uid).set(doc));
    }

    @Override
    public boolean hasUserProfile(String uid, String email) {
        if (uid != null && !uid.isEmpty()) {
            DocumentSnapshot snap = awaitDoc(col(UserContract.UserProfile.TABLE).document(uid).get());
            return snap != null && snap.exists();
        }
        if (email != null && !email.isEmpty()) {
            QuerySnapshot q = awaitQuery(
                    col(UserContract.UserProfile.TABLE).whereEqualTo(UserContract.UserProfile.Col.EMAIL, email).limit(1).get()
            );
            return q != null && !q.isEmpty();
        }
        return false;
    }

    @Override
    public String getUserName(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        DocumentSnapshot snap = awaitDoc(col(UserContract.UserProfile.TABLE).document(uid).get());
        if (snap == null || !snap.exists()) return null;
        Object v = snap.get(UserContract.UserProfile.Col.NAME);
        return v == null ? null : v.toString();
    }

    @Override
    public String getUserEmail(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        DocumentSnapshot snap = awaitDoc(col(UserContract.UserProfile.TABLE).document(uid).get());
        if (snap == null || !snap.exists()) return null;
        Object v = snap.get(UserContract.UserProfile.Col.EMAIL);
        return v == null ? null : v.toString();
    }

    @Override
    public UserProfile getUserProfile(String uid, String email) {
        if (uid != null && !uid.isEmpty()) {
            DocumentSnapshot snap = awaitDoc(
                    col(UserContract.UserProfile.TABLE).document(uid).get()
            );
            if (snap != null && snap.exists()) {
                return new UserProfile(
                        uid,
                        asString(snap.get(UserContract.UserProfile.Col.NAME)),
                        asString(snap.get(UserContract.UserProfile.Col.EMAIL))
                );
            }
        } else if (email != null && !email.isEmpty()) {
            QuerySnapshot q = awaitQuery(
                    col(UserContract.UserProfile.TABLE)
                            .whereEqualTo(UserContract.UserProfile.Col.EMAIL, email)
                            .limit(1)
                            .get()
            );
            if (q != null && !q.isEmpty()) {
                DocumentSnapshot snap = q.getDocuments().get(0);
                String foundUid = asString(snap.get(UserContract.UserProfile.Col.UID));
                String name = asString(snap.get(UserContract.UserProfile.Col.NAME));
                String mail = asString(snap.get(UserContract.UserProfile.Col.EMAIL));
                return new UserProfile(foundUid, name, mail);
            }
        }
        return null;
    }

    // ---------------------- global_score (multi-row keyed by UID) ----------------------

    private DocumentReference globalScoreDoc(String uid) {
        return col(UserContract.GlobalScore.TABLE).document(uid);
    }

    private @Nullable Integer readScoreSync(String uid) {
        DocumentSnapshot snap = awaitDoc(globalScoreDoc(uid).get());
        if (snap == null || !snap.exists()) return null;
        Number n = (Number) snap.get(UserContract.GlobalScore.Col.SCORE);
        return (n == null) ? null : n.intValue();
    }

    @Override
    public java.util.Map<String, Integer> getGlobalScores(java.util.Collection<String> uids) {
        Map<String, Integer> out = new HashMap<>();
        if (uids == null || uids.isEmpty()) return out;

        // Firestore whereIn supports up to 10 values; chunk if larger.
        final int LIMIT = 10;
        List<String> bucket = new ArrayList<>(LIMIT);

        for (String uid : uids) {
            if (uid == null || uid.isEmpty()) continue;
            bucket.add(uid);
            if (bucket.size() == LIMIT) {
                QuerySnapshot q = awaitQuery(col(UserContract.GlobalScore.TABLE)
                        .whereIn(UserContract.GlobalScore.Col.UID, new ArrayList<>(bucket))
                        .get());
                if (q != null) {
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String id = d.getId();
                        Number n = (Number) d.get(UserContract.GlobalScore.Col.SCORE);
                        if (id != null && n != null) out.put(id, n.intValue());
                    }
                }
                bucket.clear();
            }
        }
        if (!bucket.isEmpty()) {
            QuerySnapshot q = awaitQuery(col(UserContract.GlobalScore.TABLE)
                    .whereIn(UserContract.GlobalScore.Col.UID, new ArrayList<>(bucket))
                    .get());
            if (q != null) {
                for (DocumentSnapshot d : q.getDocuments()) {
                    String id = d.getId();
                    Number n = (Number) d.get(UserContract.GlobalScore.Col.SCORE);
                    if (id != null && n != null) out.put(id, n.intValue());
                }
            }
        }
        return out;
    }

    @Override
    public java.util.List<UserInterface.ScoreEntry> listAllGlobalScores() {
        QuerySnapshot q = awaitQuery(col(UserContract.GlobalScore.TABLE).get());
        List<UserInterface.ScoreEntry> list = new ArrayList<>();
        if (q == null) return list;
        for (DocumentSnapshot d : q.getDocuments()) {
            String uid = d.getId();
            Number n = (Number) d.get(UserContract.GlobalScore.Col.SCORE);
            list.add(new UserInterface.ScoreEntry(uid, n == null ? 0 : n.intValue()));
        }
        return list;
    }

    // ---- CREATE ----
    @Override
    public boolean createGlobalScore(int initialScore) {
        String uid = requireUid();
        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.GlobalScore.Col.UID, uid);
        doc.put(UserContract.GlobalScore.Col.SCORE, Math.max(0, initialScore));
        return awaitOk(globalScoreDoc(uid).set(doc));
    }

    @Override
    public boolean createGlobalScore(String uid, int initialScore) {
        if (uid == null || uid.isEmpty()) return false;
        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.GlobalScore.Col.UID, uid);
        doc.put(UserContract.GlobalScore.Col.SCORE, Math.max(0, initialScore));
        return awaitOk(globalScoreDoc(uid).set(doc));
    }

    @Override
    public boolean ensureGlobalScore(String uid) {
        if (uid == null || uid.isEmpty()) return false;
        // Merge will create if missing, keep existing otherwise.
        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.GlobalScore.Col.UID, uid);
        doc.put(UserContract.GlobalScore.Col.SCORE, 0);
        return awaitOk(globalScoreDoc(uid).set(doc, SetOptions.merge()));
    }

    // ---- UPDATE ----
    @Override
    public int getGlobalScore() {
        String uid = requireUid();
        Integer v = readScoreSync(uid);
        return (v == null) ? 0 : v;
    }

    @Override
    public @Nullable Integer getGlobalScore(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        return readScoreSync(uid);
    }

    @Override
    public boolean setGlobalScore(String uid, int newScore) {
        if (uid == null || uid.isEmpty()) return false;
        Map<String, Object> body = new HashMap<>();
        body.put(UserContract.GlobalScore.Col.UID, uid);
        body.put(UserContract.GlobalScore.Col.SCORE, Math.max(0, newScore));
        // set() acts as upsert (create/replace)
        return awaitOk(globalScoreDoc(uid).set(body));
    }

    @Override
    public int addToGlobalScore(String uid, int delta) {
        if (uid == null || uid.isEmpty()) return 0;
        DocumentReference ref = globalScoreDoc(uid);
        try {
            return Tasks.await(db.runTransaction((Transaction.Function<Integer>) tx -> {
                DocumentSnapshot s = tx.get(ref);
                int cur = 0;
                if (s.exists()) {
                    Number n = (Number) s.get(UserContract.GlobalScore.Col.SCORE);
                    cur = (n == null) ? 0 : n.intValue();
                }
                int next = cur + delta;
                if (next < 0) next = 0;
                Map<String, Object> upd = new HashMap<>();
                upd.put(UserContract.GlobalScore.Col.UID, uid);
                upd.put(UserContract.GlobalScore.Col.SCORE, next);
                tx.set(ref, upd);
                return next;
            }));
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "addToGlobalScore txn failed", e);
            // Best-effort fallback: atomic increment (may create missing doc w/ merge)
            Map<String, Object> merge = new HashMap<>();
            merge.put(UserContract.GlobalScore.Col.UID, uid);
            awaitOk(ref.set(merge, SetOptions.merge()));
            awaitOk(ref.update(UserContract.GlobalScore.Col.SCORE, FieldValue.increment(delta)));
            Integer v = readScoreSync(uid);
            return (v == null) ? 0 : Math.max(0, v);
        }
    }

    // ---- DELETE ----
    @Override
    public boolean deleteGlobalScore() {
        return deleteGlobalScore(requireUid());
    }

    @Override
    public boolean deleteGlobalScore(String uid) {
        if (uid == null || uid.isEmpty()) return false;
        return awaitOk(globalScoreDoc(uid).delete());
    }

    @Override
    public int deleteGlobalScores(java.util.Collection<String> uids) {
        if (uids == null || uids.isEmpty()) return 0;
        int deleted = 0;
        WriteBatch batch = db.batch();
        int ops = 0;

        for (String uid : uids) {
            if (uid == null || uid.isEmpty()) continue;
            batch.delete(globalScoreDoc(uid));
            ops++;
            // Commit periodically to respect batch limit (500)
            if (ops == 450) {
                if (awaitOk(batch.commit())) deleted += ops;
                batch = db.batch();
                ops = 0;
            }
        }
        if (ops > 0 && awaitOk(batch.commit())) deleted += ops;
        return deleted;
    }


    // ---------------------- streak ----------------------

    @Override
    public boolean setGlobalScore(int newScore) {
        Map<String, Object> body = new HashMap<>();
        body.put(UserContract.GlobalScore.Col.SCORE, Math.max(0, newScore));
        return awaitOk(singleton(col(UserContract.GlobalScore.TABLE)).set(body));
    }

    @Override
    public int addToGlobalScore(int delta) {
        DocumentReference ref = singleton(col(UserContract.GlobalScore.TABLE));
        try {
            return Tasks.await(db.runTransaction((Transaction.Function<Integer>) tx -> {
                DocumentSnapshot s = tx.get(ref);
                int cur = 0;
                if (s.exists()) {
                    Number n = (Number) s.get(UserContract.GlobalScore.Col.SCORE);
                    cur = (n == null) ? 0 : n.intValue();
                }
                int next = cur + delta;
                if (next < 0) next = 0;
                Map<String, Object> upd = new HashMap<>();
                upd.put(UserContract.GlobalScore.Col.SCORE, next);
                tx.set(ref, upd);
                return next;
            }));
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "addToGlobalScore txn failed", e);
            // Fallback: non-transactional increment (best-effort)
            awaitOk(ref.update(UserContract.GlobalScore.Col.SCORE, FieldValue.increment(delta)));
            return getGlobalScore();
        }
    }

    @Override
    public int getStreakCount() {
        DocumentSnapshot snap = awaitDoc(singleton(col(UserContract.Streak.TABLE)).get());
        if (snap != null && snap.exists()) {
            Number n = (Number) snap.get(UserContract.Streak.Col.COUNT);
            return n == null ? 0 : n.intValue();
        }
        Map<String, Object> init = new HashMap<>();
        init.put(UserContract.Streak.Col.COUNT, 0);
        awaitOk(singleton(col(UserContract.Streak.TABLE)).set(init));
        return 0;
    }

    @Override
    public boolean setStreakCount(int newCount) {
        Map<String, Object> body = new HashMap<>();
        body.put(UserContract.Streak.Col.COUNT, Math.max(0, newCount));
        return awaitOk(singleton(col(UserContract.Streak.TABLE)).set(body));
    }

    // ---------------------- friends ----------------------

    @Override
    public int incrementStreak() {
        DocumentReference ref = singleton(col(UserContract.Streak.TABLE));
        try {
            return Tasks.await(db.runTransaction((Transaction.Function<Integer>) tx -> {
                DocumentSnapshot s = tx.get(ref);
                int cur = 0;
                if (s.exists()) {
                    Number n = (Number) s.get(UserContract.Streak.Col.COUNT);
                    cur = (n == null) ? 0 : n.intValue();
                }
                int next = cur + 1;
                Map<String, Object> upd = new HashMap<>();
                upd.put(UserContract.Streak.Col.COUNT, next);
                tx.set(ref, upd);
                return next;
            }));
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "incrementStreak txn failed", e);
            awaitOk(ref.update(UserContract.Streak.Col.COUNT, FieldValue.increment(1)));
            return getStreakCount();
        }
    }

    @Override
    public boolean resetStreak() {
        Map<String, Object> body = new HashMap<>();
        body.put(UserContract.Streak.Col.COUNT, 0);
        return awaitOk(singleton(col(UserContract.Streak.TABLE)).set(body));
    }

    @Override
    public boolean upsertFriend(String friendUid, String friendName) {
        if (friendUid == null || friendUid.isEmpty()) return false;

        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.Friends.Col.FRIEND_UID, friendUid);
        doc.put(UserContract.Friends.Col.FRIEND_NAME, friendName == null ? "" : friendName);
        // Keep existing status if present; otherwise default to "pending"
        DocumentReference ref = col(UserContract.Friends.TABLE).document(friendUid);
        DocumentSnapshot existing = awaitDoc(ref.get());
        String status = "pending";
        if (existing != null && existing.exists()) {
            Object s = existing.get(UserContract.Friends.Col.FRIEND_STATUS);
            if (s != null) status = s.toString();
        }
        doc.put(UserContract.Friends.Col.FRIEND_STATUS, status);
        return awaitOk(ref.set(doc));
    }

    @Override
    public boolean removeFriend(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return false;
        return awaitOk(col(UserContract.Friends.TABLE).document(friendUid).delete());
    }

    @Override
    public boolean acceptFriend(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return false;
        return awaitOk(col(UserContract.Friends.TABLE)
                .document(friendUid)
                .update(UserContract.Friends.Col.FRIEND_STATUS, "accepted"));
    }

    @Override
    public boolean denyFriend(String friendUid) {
        // Per spec: we do NOT track denied; just remove the row.
        return removeFriend(friendUid);
    }

    // ---------------------- badges ----------------------

    @Override
    public boolean isFriend(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return false;
        DocumentSnapshot snap = awaitDoc(col(UserContract.Friends.TABLE).document(friendUid).get());
        return snap != null && snap.exists();
    }

    @Override
    public List<Friend> getFriends() {
        QuerySnapshot q = awaitQuery(col(UserContract.Friends.TABLE).get());
        List<Friend> out = new ArrayList<>();
        if (q == null) return out;
        for (DocumentSnapshot d : q.getDocuments()) {
            String uid = asString(d.get(UserContract.Friends.Col.FRIEND_UID));
            String name = asString(d.get(UserContract.Friends.Col.FRIEND_NAME));
            String status = asStringOr(d.get(UserContract.Friends.Col.FRIEND_STATUS), "pending");
            out.add(new Friend(uid, name, status));
        }
        return out;
    }

    @Override
    public boolean addBadge(String badgeId) {
        if (badgeId == null || badgeId.isEmpty()) return false;
        Map<String, Object> doc = new HashMap<>();
        doc.put(UserContract.Badges.Col.BADGE_ID, badgeId);
        return awaitOk(col(UserContract.Badges.TABLE).document(badgeId).set(doc));
    }

    @Override
    public boolean removeBadge(String badgeId) {
        if (badgeId == null || badgeId.isEmpty()) return false;
        return awaitOk(col(UserContract.Badges.TABLE).document(badgeId).delete());
    }

    // ---------------------- utils ----------------------

    @Override
    public boolean hasBadge(String badgeId) {
        if (badgeId == null || badgeId.isEmpty()) return false;
        DocumentSnapshot snap = awaitDoc(col(UserContract.Badges.TABLE).document(badgeId).get());
        return snap != null && snap.exists();
    }

    @Override
    public List<String> listBadges() {
        QuerySnapshot q = awaitQuery(col(UserContract.Badges.TABLE).get());
        List<String> ids = new ArrayList<>();
        if (q == null) return ids;
        for (DocumentSnapshot d : q.getDocuments()) {
            // Mirror local: primary key is badge_uid; we also store doc id == badge_uid
            String id = asString(d.get(UserContract.Badges.Col.BADGE_ID));
            if (id == null || id.isEmpty()) id = d.getId();
            if (id != null) ids.add(id);
        }
        return ids;
    }
}
