package com.yh.toy_pj.domain.ticket.attachment.dto;

import com.yh.toy_pj.domain.ticket.attachment.TicketAttachment;
import java.time.LocalDateTime;

public record AttachmentResponse(Long id, String filename, String contentType, long size,
                                 Long uploaderId, String uploaderName, LocalDateTime createdAt) {

    public static AttachmentResponse from(TicketAttachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getSize(),
                a.getUploader().getId(), a.getUploader().getName(), a.getCreatedAt());
    }
}
