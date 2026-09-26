package com.yh.toy_pj.domain.ticket.attachment.dto;

import org.springframework.core.io.Resource;

public record AttachmentDownload(Resource resource, String filename, String contentType, long size) {
}
