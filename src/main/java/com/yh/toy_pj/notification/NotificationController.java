package com.yh.toy_pj.notification;

import com.yh.toy_pj.auth.AuthUser;
import com.yh.toy_pj.notification.dto.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "내 알림")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "최근 알림 30개와 안 읽은 개수")
    @GetMapping
    public NotificationListResponse findRecent(@AuthenticationPrincipal AuthUser me) {
        return notificationService.findRecent(me);
    }

    @Operation(summary = "읽음 처리")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        notificationService.markRead(id, me);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "모두 읽음 처리")
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthUser me) {
        notificationService.markAllRead(me);
        return ResponseEntity.noContent().build();
    }
}
