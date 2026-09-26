package com.yh.toy_pj.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.yh.toy_pj.ai.dto.TriageResult;
import com.yh.toy_pj.domain.ticket.ClassificationSource;
import com.yh.toy_pj.domain.ticket.TicketCategory;
import com.yh.toy_pj.domain.ticket.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RuleBasedTriageTest {

    private final RuleBasedTriage triage = new RuleBasedTriage();

    @ParameterizedTest(name = "[{0}] → {2} / {3}")
    @CsvSource(delimiter = '|', value = {
            "로그인이 안돼요         | 비밀번호를 바꾼 뒤로 로그인 불가        | ACCOUNT  | HIGH",
            "와이파이 문의          | 회의실 와이파이 비밀번호가 궁금합니다     | ACCOUNT  | LOW",
            "긴급) 전사 인터넷 장애  | 전체 인원 인터넷 연결 안 됨             | NETWORK  | URGENT",
            "모니터 추가 신청        | 듀얼 모니터 사용을 위해 추가 요청합니다   | HARDWARE | LOW",
            "엑셀 업데이트 후 느림   | 업데이트 이후 파일 열 때 오래 걸립니다    | SOFTWARE | MEDIUM",
            "기타                  | 자리 이동 관련                        | ETC      | MEDIUM",
            "노트북 전원 안 켜짐     | 부팅이 안 되고 업무 불가 상태입니다      | HARDWARE | HIGH",
            "전 직원 메일 접속 장애  | 오전부터 계속됩니다                     | ETC      | URGENT",
            "모니터 전체 화면 깨짐   | 화면 절반이 줄무늬로 보입니다           | HARDWARE | MEDIUM"
    })
    @DisplayName("키워드로 분류와 우선순위를 결정한다")
    void classifiesByKeyword(String title, String description, TicketCategory category, TicketPriority priority) {
        TriageResult result = triage.triage(title, description);

        assertThat(result.category()).isEqualTo(category);
        assertThat(result.priority()).isEqualTo(priority);
        assertThat(result.source()).isEqualTo(ClassificationSource.RULE);
    }

    @Test
    @DisplayName("한 사람의 업무 불가는 '높음' (여러 사람의 업무 중단·보안 사고만 '긴급') - AI 분류 기준과 동일")
    void personalOutageIsHighNotUrgent() {
        assertThat(triage.triage("노트북 고장", "제 노트북이 먹통이라 업무 불가입니다").priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(triage.triage("긴급) 전사 서버 다운", "모든 직원이 접속할 수 없습니다").priority()).isEqualTo(TicketPriority.URGENT);
    }

    @Test
    @DisplayName("대소문자와 무관하게 영문 키워드를 인식한다")
    void caseInsensitive() {
        assertThat(triage.triage("VPN disconnected", "").category()).isEqualTo(TicketCategory.NETWORK);
    }
}
