package com.yh.toy_pj.domain.ticket.comment;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketService;
import com.yh.toy_pj.domain.ticket.comment.dto.CommentCreateRequest;
import com.yh.toy_pj.domain.ticket.comment.dto.CommentResponse;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserService;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import com.yh.toy_pj.notification.TicketEvents;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 권한
 * <ul>
 *   <li>조회/작성: 티켓을 볼 수 있는 사람 (요청자 본인, IT 관리자)</li>
 *   <li>내부 메모: IT 관리자만 작성·조회</li>
 *   <li>종료·취소된 티켓에는 작성 불가</li>
 *   <li>삭제: 작성자 본인만</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketService ticketService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public List<CommentResponse> findAll(Long ticketId, AuthUser me) {
        ticketService.getViewableTicket(ticketId, me);
        return commentRepository.findByTicket(ticketId, me.isAdmin()).stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse write(Long ticketId, CommentCreateRequest request, AuthUser me) {
        if (request.isInternal() && !me.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내부 메모는 IT 관리자만 작성할 수 있습니다.");
        }
        Ticket ticket = ticketService.getWritableTicket(ticketId, me);
        User author = userService.getUser(me.id());
        TicketComment comment = commentRepository.save(
                TicketComment.write(ticket, author, request.content().trim(), request.isInternal()));

        // 알림은 이벤트로 분리 → 댓글 서비스는 누가 알림을 받는지 몰라도 된다
        eventPublisher.publishEvent(new TicketEvents.CommentAdded(ticket, author, comment.isInternal()));
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long ticketId, Long commentId, AuthUser me) {
        ticketService.getViewableTicket(ticketId, me);
        TicketComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getTicket().getId().equals(ticketId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.isWrittenBy(me.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }
}
