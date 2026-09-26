package com.yh.toy_pj.domain.ticket.attachment;

import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 업로드 허용 파일 형식.
 *
 * 확장자와 클라이언트가 보낸 Content-Type 은 얼마든지 속일 수 있으므로,
 * 1) 확장자 허용 목록 → 2) 파일 앞부분의 실제 바이트(매직 넘버)로 형식을 한 번 더 확인하고,
 * 3) 응답 Content-Type 은 클라이언트 값이 아닌 서버가 정한 값을 사용한다.
 * HTML·SVG 처럼 브라우저에서 스크립트가 실행될 수 있는 형식은 허용하지 않는다.
 */
final class FileTypePolicy {

    record FileType(String contentType, Predicate<byte[]> signature) {
    }

    private static final Map<String, FileType> ALLOWED = Map.of(
            "png", new FileType("image/png", h -> startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)),
            "jpg", new FileType("image/jpeg", h -> startsWith(h, 0xFF, 0xD8, 0xFF)),
            "jpeg", new FileType("image/jpeg", h -> startsWith(h, 0xFF, 0xD8, 0xFF)),
            "gif", new FileType("image/gif", h -> startsWith(h, 'G', 'I', 'F', '8')),
            "webp", new FileType("image/webp", h -> startsWith(h, 'R', 'I', 'F', 'F') && matchesAt(h, 8, 'W', 'E', 'B', 'P')),
            "pdf", new FileType("application/pdf", h -> startsWith(h, '%', 'P', 'D', 'F', '-')),
            "txt", new FileType("text/plain; charset=UTF-8", FileTypePolicy::looksLikeText),
            "log", new FileType("text/plain; charset=UTF-8", FileTypePolicy::looksLikeText)
    );

    static final String ALLOWED_EXTENSIONS = String.join(", ", ALLOWED.keySet().stream().sorted().toList());

    private FileTypePolicy() {
    }

    /** 허용된 형식이면 서버가 정한 Content-Type 을 돌려주고, 아니면 415 예외 */
    static String verify(String filename, byte[] header) {
        String ext = extensionOf(filename);
        FileType type = ALLOWED.get(ext);
        if (type == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "허용되지 않는 파일 형식입니다. (허용: " + ALLOWED_EXTENSIONS + ")");
        }
        if (!type.signature().test(header)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "파일 내용이 확장자(." + ext + ")와 일치하지 않습니다.");
        }
        return type.contentType();
    }

    static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] data, int... expected) {
        return matchesAt(data, 0, expected);
    }

    private static boolean matchesAt(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /** 텍스트 파일: 실행 파일 등 바이너리가 섞이지 않았는지 NUL 바이트로 간단히 확인 */
    private static boolean looksLikeText(byte[] data) {
        for (byte b : data) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }
}
