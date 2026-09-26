package com.yh.toy_pj.support;

import com.yh.toy_pj.domain.asset.Asset;
import com.yh.toy_pj.domain.asset.AssetRepository;
import com.yh.toy_pj.domain.asset.AssetType;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.Ticket;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import com.yh.toy_pj.domain.ticket.TicketRepository;
import com.yh.toy_pj.domain.ticket.TicketStatus;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.domain.user.UserRole;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발/데모용 샘플 데이터. DB 가 비어 있을 때만 1회 생성한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);

        User admin = userRepository.save(User.create("김관리", "admin@daon.example", "IT지원팀", UserRole.ADMIN));
        userRepository.save(User.create("이지원", "support@daon.example", "IT지원팀", UserRole.ADMIN));
        User hong = userRepository.save(User.create("홍길동", "hong@daon.example", "영업팀", UserRole.USER));
        User kim = userRepository.save(User.create("김철수", "kim@daon.example", "개발팀", UserRole.USER));
        userRepository.save(User.create("박영희", "park@daon.example", "인사팀", UserRole.USER));

        Asset macbook = Asset.register("MacBook Pro 14 M3", AssetType.LAPTOP, "C02XK1AAMD6T", LocalDate.of(2025, 3, 2), null);
        macbook.assignTo(hong);
        Asset gram = Asset.register("LG gram 16", AssetType.LAPTOP, "LG16-2024-0012", LocalDate.of(2024, 8, 19), null);
        gram.assignTo(kim);
        Asset monitor = Asset.register("Dell U2723QE", AssetType.MONITOR, "DELL-U27-5531", LocalDate.of(2024, 1, 10), "4K 모니터");
        Asset office = Asset.register("Microsoft 365 Business", AssetType.SOFTWARE, "M365-LIC-0042", LocalDate.of(2025, 1, 1), "연간 라이선스");
        Asset printer = Asset.register("HP LaserJet Pro", AssetType.ETC, "HP-LJ-3F-01", LocalDate.of(2022, 5, 30), "3층 공용 프린터");
        printer.startRepair();
        assetRepository.save(macbook);
        assetRepository.save(gram);
        assetRepository.save(monitor);
        assetRepository.save(office);
        assetRepository.save(printer);

        ticketRepository.save(Ticket.open("VPN 접속이 안 됩니다", "재택 중 VPN 연결 시 인증 오류가 발생합니다.",
                TicketCategory.NETWORK, TicketPriority.HIGH, ClassificationSource.MANUAL, hong, null, now));

        Ticket battery = Ticket.open("노트북 배터리가 빨리 닳아요", "완충 후 1시간이면 방전됩니다.",
                TicketCategory.HARDWARE, TicketPriority.MEDIUM, ClassificationSource.RULE, kim, gram, now);
        battery.assign(admin);
        battery.changeStatus(TicketStatus.IN_PROGRESS, "배터리 진단 진행", now);
        ticketRepository.save(battery);

        Ticket printerTicket = Ticket.open("3층 프린터 용지 걸림", "용지가 계속 걸려서 출력이 안 됩니다.",
                TicketCategory.HARDWARE, TicketPriority.LOW, ClassificationSource.MANUAL, hong, printer, now);
        printerTicket.assign(admin);
        printerTicket.changeStatus(TicketStatus.IN_PROGRESS, null, now);
        printerTicket.changeStatus(TicketStatus.RESOLVED, "롤러 교체 완료", now);
        ticketRepository.save(printerTicket);

        log.info("[local] 샘플 데이터를 생성했습니다. (사용자 5, 자산 5, 티켓 3)");
    }
}
