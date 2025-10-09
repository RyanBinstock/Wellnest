package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.code.wlu.cp470.wellnest.data.local.room.entities.BadgeLocal;
import com.code.wlu.cp470.wellnest.data.local.room.entities.FriendLocal;

import java.util.List;

@Dao
public interface UserLocalDao {

    // ===== Friends =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertFriend(FriendLocal f);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertFriends(List<FriendLocal> friends);

    @Query("DELETE FROM friend_local WHERE friendUid = :uid")
    void deleteFriendByUid(String uid);

    @Query("SELECT * FROM friend_local ORDER BY sinceEpochMs DESC")
    List<FriendLocal> listFriends();

    @Query("DELETE FROM friend_local")
    void clearFriends();


    // ===== Badges =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertBadge(BadgeLocal badge);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertBadges(List<BadgeLocal> badges);

    @Query("DELETE FROM badge_local WHERE code = :code")
    void deleteBadge(String code);

    @Query("SELECT EXISTS(SELECT 1 FROM badge_local WHERE code = :code)")
    boolean hasBadge(String code);

    @Query("SELECT * FROM badge_local ORDER BY earnedAt DESC")
    List<BadgeLocal> listBadges();

    @Query("DELETE FROM badge_local")
    void clearBadges();
}
