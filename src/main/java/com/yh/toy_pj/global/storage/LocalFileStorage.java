package com.yh.toy_pj.global.storage;

import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(StorageProperties properties) throws IOException {
        this.baseDir = Path.of(properties.localDir()).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        log.info("첨부파일 저장 위치: {}", baseDir);
    }

    @Override
    public void store(String storedName, InputStream content) {
        try (content) {
            Files.copy(content, resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "파일 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (!Files.isReadable(path)) {
            throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND, "저장된 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException e) {
            log.warn("파일 삭제 실패 (나중에 정리 필요): {}", storedName, e);
        }
    }

    /** "../" 같은 경로 조작으로 저장 디렉터리 밖에 접근하지 못하도록 한 번 더 검사한다. */
    private Path resolve(String storedName) {
        Path path = baseDir.resolve(storedName).normalize();
        if (!path.getParent().equals(baseDir)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "잘못된 파일 경로입니다.");
        }
        return path;
    }
}
