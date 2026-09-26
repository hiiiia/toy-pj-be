package com.yh.toy_pj.domain.ticket;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /** 목록 조회: 연관 엔티티(요청자/담당자/자산)를 한 번에 조회해 N+1 방지 */
    @Override
    @EntityGraph(attributePaths = {"requester", "assignee", "asset"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    /** 상세 조회: 처리 이력까지 함께 조회 */
    @EntityGraph(attributePaths = {"requester", "assignee", "asset", "histories"})
    Optional<Ticket> findDetailById(Long id);

    boolean existsByAssetId(Long assetId);

    long countByAssigneeIsNullAndStatus(TicketStatus status);

    @Query("select count(t) from Ticket t where t.status in :statuses and t.dueAt < :now")
    long countOverdue(@Param("statuses") Collection<TicketStatus> statuses, @Param("now") LocalDateTime now);

    @Query("select t.status as code, count(t) as total from Ticket t group by t.status")
    List<KeyCount<TicketStatus>> countGroupByStatus();

    @Query("select t.priority as code, count(t) as total from Ticket t where t.status in :statuses group by t.priority")
    List<KeyCount<TicketPriority>> countGroupByPriority(@Param("statuses") Collection<TicketStatus> statuses);

    // ===== SLA 알림 =====

    /** 처리 기한이 지났는데 아직 "초과" 알림을 보내지 않은 진행중 티켓 */
    @Query("""
            select t from Ticket t left join fetch t.assignee
            where t.status in :statuses and t.dueAt <= :now and t.slaBreachedAt is null
            """)
    List<Ticket> findSlaBreachTargets(@Param("statuses") Collection<TicketStatus> statuses, @Param("now") LocalDateTime now);

    /** 처리 기한이 곧 다가오는데 아직 "임박" 알림을 보내지 않은 진행중 티켓 */
    @Query("""
            select t from Ticket t left join fetch t.assignee
            where t.status in :statuses and t.dueAt > :now and t.dueAt <= :threshold and t.slaWarnedAt is null
            """)
    List<Ticket> findSlaWarningTargets(@Param("statuses") Collection<TicketStatus> statuses,
                                       @Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    /**
     * 알림 발송 기록은 벌크 UPDATE 로 남긴다.
     * 벌크 UPDATE 는 @Version 을 올리지 않으므로, 담당자가 같은 티켓을 수정하는 중이어도 낙관적 락 충돌(409)을 일으키지 않는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Ticket t set t.slaBreachedAt = :now, t.slaWarnedAt = coalesce(t.slaWarnedAt, :now) where t.id in :ids")
    int markSlaBreached(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update Ticket t set t.slaWarnedAt = :now where t.id in :ids")
    int markSlaWarned(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);

    interface KeyCount<K> {
        K getCode();

        long getTotal();
    }
}
