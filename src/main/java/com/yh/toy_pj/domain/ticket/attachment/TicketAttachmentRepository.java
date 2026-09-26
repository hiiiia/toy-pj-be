package com.yh.toy_pj.domain.ticket.attachment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    @Query("select a from TicketAttachment a join fetch a.uploader where a.ticket.id = :ticketId order by a.id asc")
    List<TicketAttachment> findByTicketId(@Param("ticketId") Long ticketId);

    long countByTicketId(Long ticketId);
}
