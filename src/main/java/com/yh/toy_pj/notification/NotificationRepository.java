package com.yh.toy_pj.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.recipient.id = :recipientId order by n.id desc")
    List<Notification> findRecent(@Param("recipientId") Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Notification n where n.readAt is not null and n.readAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
