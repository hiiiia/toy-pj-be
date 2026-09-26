package com.yh.toy_pj.domain.ticket.attachment;

import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 첨부파일 메타데이터. 파일 내용은 DB 가 아닌 {@link com.yh.toy_pj.global.storage.FileStorage} 에 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, unique = true, length = 100)
    private String storedName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long size;

    public TicketAttachment(Ticket ticket, User uploader, String originalFilename, String storedName,
                            String contentType, long size) {
        this.ticket = ticket;
        this.uploader = uploader;
        this.originalFilename = originalFilename;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
    }

    public boolean isUploadedBy(Long userId) {
        return uploader.getId().equals(userId);
    }
}
