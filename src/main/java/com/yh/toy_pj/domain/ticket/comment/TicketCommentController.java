package com.yh.toy_pj.domain.ticket.comment;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.comment.dto.CommentCreateRequest;
import com.yh.toy_pj.domain.ticket.comment.dto.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ticket Comment", description = "티켓 댓글 / 내부 메모")
@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {

    private final TicketCommentService commentService;

    @Operation(summary = "댓글 목록", description = "일반 사용자에게는 내부 메모가 보이지 않는다.")
    @GetMapping
    public List<CommentResponse> findAll(@PathVariable Long ticketId, @AuthenticationPrincipal AuthUser me) {
        return commentService.findAll(ticketId, me);
    }

    @Operation(summary = "댓글 작성", description = "internal=true 는 IT 관리자 전용 내부 메모. 종료된 티켓에는 작성할 수 없다.")
    @PostMapping
    public ResponseEntity<CommentResponse> write(@PathVariable Long ticketId, @Valid @RequestBody CommentCreateRequest request,
                                                 @AuthenticationPrincipal AuthUser me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.write(ticketId, request, me));
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인만 삭제할 수 있다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long ticketId, @PathVariable Long commentId,
                                       @AuthenticationPrincipal AuthUser me) {
        commentService.delete(ticketId, commentId, me);
        return ResponseEntity.noContent().build();
    }
}
