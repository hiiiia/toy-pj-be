package com.yh.toy_pj.domain.ticket.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileTypePolicyTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);

    @Test
    @DisplayName("확장자와 실제 내용이 일치하면 서버가 정한 Content-Type 을 돌려준다")
    void allowed() {
        assertThat(FileTypePolicy.verify("screen.PNG", PNG)).isEqualTo("image/png");
        assertThat(FileTypePolicy.verify("report.pdf", PDF)).isEqualTo("application/pdf");
        assertThat(FileTypePolicy.verify("error.log", "ERROR at line 1".getBytes())).startsWith("text/plain");
    }

    @ParameterizedTest
    @CsvSource({"evil.html", "icon.svg", "run.exe", "noext", "script.js"})
    @DisplayName("허용 목록에 없는 확장자는 415")
    void disallowedExtension(String filename) {
        assertThatThrownBy(() -> FileTypePolicy.verify(filename, PNG))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("확장자만 .png 로 바꾼 파일(내용은 PDF)은 거절한다")
    void spoofedExtension() {
        assertThatThrownBy(() -> FileTypePolicy.verify("fake.png", PDF))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일치하지 않습니다");
    }

    @Test
    @DisplayName("바이너리가 섞인 .txt 는 거절한다")
    void binaryText() {
        assertThatThrownBy(() -> FileTypePolicy.verify("a.txt", new byte[]{'M', 'Z', 0, 0}))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("파일명 정리: 경로 제거, 제어문자 제거, 길이 제한 (확장자 유지)")
    void sanitizeFilename() {
        assertThat(TicketAttachmentService.sanitizeFilename("C:\\Users\\hong\\화면 캡처.png")).isEqualTo("화면 캡처.png");
        assertThat(TicketAttachmentService.sanitizeFilename("../../etc/passwd.txt")).isEqualTo("passwd.txt");
        assertThat(TicketAttachmentService.sanitizeFilename("a\nb.png")).isEqualTo("ab.png");
        assertThat(TicketAttachmentService.sanitizeFilename(null)).isEqualTo("file");
        assertThat(TicketAttachmentService.sanitizeFilename("x".repeat(300) + ".pdf")).hasSize(194).endsWith(".pdf");
    }
}
