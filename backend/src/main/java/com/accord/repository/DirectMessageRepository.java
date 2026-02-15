package com.accord.repository;

import com.accord.model.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("SELECT dm FROM DirectMessage dm WHERE " +
           "(dm.senderId = :userId1 AND dm.recipientId = :userId2) OR " +
           "(dm.senderId = :userId2 AND dm.recipientId = :userId1) " +
           "ORDER BY dm.timestamp DESC")
    List<DirectMessage> findConversation(@Param("userId1") Long userId1,
                                         @Param("userId2") Long userId2,
                                         Pageable pageable);

    @Modifying
    @Query("UPDATE DirectMessage dm SET dm.read = true WHERE dm.recipientId = :recipientId AND dm.senderId = :senderId AND dm.read = false")
    int markConversationAsRead(@Param("recipientId") Long recipientId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(dm) FROM DirectMessage dm WHERE dm.recipientId = :recipientId AND dm.senderId = :senderId AND dm.read = false")
    long countUnreadFromSender(@Param("recipientId") Long recipientId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(dm) FROM DirectMessage dm WHERE dm.recipientId = :recipientId AND dm.read = false")
    long countUnreadForUser(@Param("recipientId") Long recipientId);

    @Query("SELECT DISTINCT CASE WHEN dm.senderId = :userId THEN dm.recipientId ELSE dm.senderId END " +
           "FROM DirectMessage dm WHERE dm.senderId = :userId OR dm.recipientId = :userId")
    List<Long> findConversationPartnerIds(@Param("userId") Long userId);
}
