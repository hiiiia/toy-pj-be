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

    interface KeyCount<K> {
        K getCode();

        long getTotal();
    }
}
