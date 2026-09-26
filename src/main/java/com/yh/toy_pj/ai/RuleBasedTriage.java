package com.yh.toy_pj.ai;

import com.yh.toy_pj.ai.dto.TriageResult;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 키워드 기반 분류기. AI 를 사용할 수 없거나(키 미설정/장애/타임아웃) AI 응답이 잘못된 경우의 대체 수단.
 * 외부 의존성이 없어 항상 결과를 보장한다.
 */
@Component
public class RuleBasedTriage {

    private static final Map<TicketCategory, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final Map<TicketPriority, List<String>> PRIORITY_KEYWORDS = new LinkedHashMap<>();

    static {
        // 먼저 선언된 항목이 우선한다.
        CATEGORY_KEYWORDS.put(TicketCategory.ACCOUNT, List.of("계정", "비밀번호", "패스워드", "로그인", "권한", "password", "account"));
        CATEGORY_KEYWORDS.put(TicketCategory.NETWORK, List.of("네트워크", "인터넷", "와이파이", "wifi", "vpn", "랜선", "공유기"));
        CATEGORY_KEYWORDS.put(TicketCategory.HARDWARE, List.of("노트북", "모니터", "키보드", "마우스", "프린터", "전원", "부팅", "배터리", "하드웨어"));
        CATEGORY_KEYWORDS.put(TicketCategory.SOFTWARE, List.of("설치", "프로그램", "소프트웨어", "업데이트", "라이선스", "오피스", "에러", "오류"));

        PRIORITY_KEYWORDS.put(TicketPriority.URGENT, List.of("긴급", "전사", "전체", "서버 다운", "업무 불가", "보안 사고"));
        PRIORITY_KEYWORDS.put(TicketPriority.HIGH, List.of("안됨", "안 됨", "안돼", "불가", "먹통", "고장", "접속 불가"));
        PRIORITY_KEYWORDS.put(TicketPriority.LOW, List.of("문의", "요청", "변경", "추가", "신청"));
    }

    public TriageResult triage(String title, String description) {
        String text = (title + " " + description).toLowerCase();
        TicketCategory category = match(CATEGORY_KEYWORDS, text, TicketCategory.ETC);
        TicketPriority priority = match(PRIORITY_KEYWORDS, text, TicketPriority.MEDIUM);
        return new TriageResult(category, priority, ClassificationSource.RULE, "키워드 규칙 기반 분류");
    }

    private static <E> E match(Map<E, List<String>> rules, String text, E fallback) {
        return rules.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(text::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(fallback);
    }
}
