package com.yh.toy_pj.domain.ticket.comment.dto;

import com.yh.toy_pj.domain.ticket.comment.TicketComment;
import com.yh.toy_pj.domain.user.UserRole;
import java.time.LocalDateTime;

public record CommentResponse(Long id, Long authorId, String authorName, UserRole authorRole,
                              String content, boolean internal, LocalDateTime createdAt) {

    public static CommentResponse from(TicketComment comment) {
        return new CommentResponse(comment.getId(), comment.getAuthor().getId(), comment.getAuthor().getName(),
                comment.getAuthor().getRole(), comment.getContent(), comment.isInternal(), comment.getCreatedAt());
    }
}
