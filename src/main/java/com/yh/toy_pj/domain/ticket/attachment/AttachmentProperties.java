package com.yh.toy_pj.domain.ticket.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * @param maxSize      파일 1개 최대 크기
 * @param maxPerTicket 티켓 1개당 최대 첨부 개수
 */
@ConfigurationProperties(prefix = "app.attachment")
public record AttachmentProperties(
        @DefaultValue("5MB") DataSize maxSize,
        @DefaultValue("10") int maxPerTicket
) {
}
