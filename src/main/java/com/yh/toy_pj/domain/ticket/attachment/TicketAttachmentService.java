package com.yh.toy_pj.domain.ticket.attachment;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketService;
import com.yh.toy_pj.domain.ticket.attachment.dto.AttachmentDownload;
import com.yh.toy_pj.domain.ticket.attachment.dto.AttachmentResponse;
import com.yh.toy_pj.domain.user.UserService;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.global.storage.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일 업로드/다운로드.
 *
 * DB(메타데이터)와 디스크(파일 내용)는 하나의 트랜잭션으로 묶이지 않으므로 순서를 맞춘다.
 * <ul>
 *   <li>업로드: DB 저장 → 파일 저장. 이후 DB 가 롤백되면 방금 저장한 파일도 지운다.</li>
 *   <li>삭제: DB 삭제 → 커밋이 성공한 뒤에만 파일을 지운다. (롤백되면 파일은 남아 있어야 하므로)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketAttachmentService {

    private static final int HEADER_SIZE = 8 * 1024;

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final FileStorage fileStorage;
    private final AttachmentProperties properties;

    public List<AttachmentResponse> findAll(Long ticketId, AuthUser me) {
        ticketService.getViewableTicket(ticketId, me);
        return attachmentRepository.findByTicketId(ticketId).stream().map(AttachmentResponse::from).toList();
    }

    @Transactional
    public AttachmentResponse upload(Long ticketId, MultipartFile file, AuthUser me) {
        Ticket ticket = ticketService.getWritableTicket(ticketId, me);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > properties.maxSize().toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "파일 크기는 %dMB 이하여야 합니다.".formatted(properties.maxSize().toMegabytes()));
        }
        if (attachmentRepository.countByTicketId(ticketId) >= properties.maxPerTicket()) {
            throw new BusinessException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED,
                    "티켓당 첨부파일은 %d개까지 등록할 수 있습니다.".formatted(properties.maxPerTicket()));
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        String contentType = FileTypePolicy.verify(filename, readHeader(file));
        String storedName = UUID.randomUUID() + "." + FileTypePolicy.extensionOf(filename);

        TicketAttachment attachment = attachmentRepository.save(new TicketAttachment(
                ticket, userService.getUser(me.id()), filename, storedName, contentType, file.getSize()));

        try {
            fileStorage.store(storedName, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "업로드한 파일을 읽을 수 없습니다.", e);
        }
        afterRollback(() -> fileStorage.delete(storedName));
        return AttachmentResponse.from(attachment);
    }

    public AttachmentDownload download(Long ticketId, Long attachmentId, AuthUser me) {
        ticketService.getViewableTicket(ticketId, me);
        TicketAttachment attachment = getAttachment(ticketId, attachmentId);
        return new AttachmentDownload(fileStorage.load(attachment.getStoredName()),
                attachment.getOriginalFilename(), attachment.getContentType(), attachment.getSize());
    }

    /** 업로드한 본인 또는 IT 관리자만 삭제할 수 있다. */
    @Transactional
    public void delete(Long ticketId, Long attachmentId, AuthUser me) {
        ticketService.getViewableTicket(ticketId, me);
        TicketAttachment attachment = getAttachment(ticketId, attachmentId);
        if (!me.isAdmin() && !attachment.isUploadedBy(me.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 올린 첨부파일만 삭제할 수 있습니다.");
        }
        attachmentRepository.delete(attachment);
        String storedName = attachment.getStoredName();
        afterCommit(() -> fileStorage.delete(storedName));
    }

    private TicketAttachment getAttachment(Long ticketId, Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTicket().getId().equals(ticketId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(HEADER_SIZE);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "업로드한 파일을 읽을 수 없습니다.", e);
        }
    }

    /**
     * 표시용 파일명 정리: 일부 브라우저가 보내는 전체 경로("C:\\Users\\...\\a.png")에서 이름만 남기고,
     * 제어 문자를 제거하고 길이를 제한한다. (저장 경로에는 이 이름을 쓰지 않는다)
     */
    static String sanitizeFilename(String original) {
        String name = StringUtils.hasText(original) ? original : "file";
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            name = "file";
        }
        if (name.length() > 200) {
            String ext = FileTypePolicy.extensionOf(name);
            name = name.substring(0, 190) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name;
    }

    private static void afterRollback(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
