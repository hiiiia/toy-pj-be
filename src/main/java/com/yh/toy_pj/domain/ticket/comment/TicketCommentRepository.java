package com.yh.toy_pj.domain.ticket.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    /** 일반 사용자에게는 내부 메모를 제외하고 보여준다. (includeInternal = false) */
    @Query("""
            select c from TicketComment c join fetch c.author
            where c.ticket.id = :ticketId and (:includeInternal = true or c.internal = false)
            order by c.id asc
            """)
    List<TicketComment> findByTicket(@Param("ticketId") Long ticketId, @Param("includeInternal") boolean includeInternal);
}
