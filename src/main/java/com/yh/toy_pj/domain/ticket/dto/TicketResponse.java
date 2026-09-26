package com.yh.toy_pj.domain.ticket.dto;

import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import com.yh.toy_pj.domain.user.User;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        ClassificationSource classificationSource,
        TicketStatus status,
        Long requesterId,
        String requesterName,
        Long assigneeId,
        String assigneeName,
        Long assetId,
        String assetName,
        LocalDateTime dueAt,
        boolean overdue,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TicketResponse of(Ticket ticket, LocalDateTime now) {
        User requester = ticket.getRequester();
        User assignee = ticket.getAssignee();
        Asset asset = ticket.getAsset();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getClassificationSource(),
                ticket.getStatus(),
                requester.getId(),
                requester.getName(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,
                asset != null ? asset.getId() : null,
                asset != null ? asset.getName() : null,
                ticket.getDueAt(),
                ticket.isOverdue(now),
                ticket.getResolvedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
