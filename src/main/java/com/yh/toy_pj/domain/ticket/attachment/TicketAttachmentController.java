package com.yh.toy_pj.domain.ticket.attachment;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.attachment.dto.AttachmentDownload;
import com.yh.toy_pj.domain.ticket.attachment.dto.AttachmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Ticket Attachment", description = "티켓 첨부파일 (스크린샷, 로그 등)")
@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    @Operation(summary = "첨부파일 목록")
    @GetMapping
    public List<AttachmentResponse> findAll(@PathVariable Long ticketId, @AuthenticationPrincipal AuthUser me) {
        return attachmentService.findAll(ticketId, me);
    }

    @Operation(summary = "첨부파일 업로드", description = "png, jpg, gif, webp, pdf, txt, log / 최대 5MB / 티켓당 10개")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long ticketId, @RequestPart("file") MultipartFile file,
                                                     @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(ticketId, file, me));
    }

    /**
     * 항상 "다운로드"(attachment)로 내려주고 nosniff 를 붙여, 브라우저가 파일 내용을 추측해 실행하지 않도록 한다.
     */
    @Operation(summary = "첨부파일 다운로드")
    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long ticketId, @PathVariable Long attachmentId,
                                             @AuthenticationPrincipal AuthUser me) {
        AttachmentDownload file = attachmentService.download(ticketId, attachmentId, me);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.size())
                .body(file.resource());
    }

    @Operation(summary = "첨부파일 삭제", description = "올린 사람 본인 또는 IT 관리자")
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long ticketId, @PathVariable Long attachmentId,
                                       @AuthenticationPrincipal AuthUser me) {
        attachmentService.delete(ticketId, attachmentId, me);
        return ResponseEntity.noContent().build();
    }
}
