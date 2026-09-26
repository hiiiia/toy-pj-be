package com.yh.toy_pj.global.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * 파일 저장소 추상화. 지금은 로컬 디스크({@link LocalFileStorage})를 쓰지만,
 * 서버를 여러 대로 늘리면 S3 같은 오브젝트 스토리지 구현체로 바꿔 끼우면 된다. (호출하는 쪽 코드는 그대로)
 */
public interface FileStorage {

    /** storedName 은 서버가 만든 이름(UUID)만 사용한다. 사용자가 보낸 파일명은 절대 경로에 쓰지 않는다. */
    void store(String storedName, InputStream content);

    Resource load(String storedName);

    void delete(String storedName);
}
