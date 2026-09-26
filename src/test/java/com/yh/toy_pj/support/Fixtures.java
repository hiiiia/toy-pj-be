package com.yh.toy_pj.support;

import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.asset.AssetType;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class Fixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 9, 0);

    private Fixtures() {
    }

    public static User admin() {
        return User.create("김관리", "admin@test.com", "IT지원팀", UserRole.ADMIN);
    }

    public static User employee() {
        return User.create("홍길동", "hong@test.com", "영업팀", UserRole.USER);
    }

    public static User withId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Asset laptop(String serial) {
        return Asset.register("MacBook Pro", AssetType.LAPTOP, serial, LocalDate.of(2025, 1, 1), null);
    }

    public static Ticket ticket(User requester, TicketPriority priority) {
        return Ticket.open("VPN 접속 불가", "재택 중 VPN 이 연결되지 않습니다.", TicketCategory.NETWORK, priority,
                ClassificationSource.MANUAL, requester, null, NOW);
    }
}
