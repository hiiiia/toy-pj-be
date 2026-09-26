package com.yh.toy_pj.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param localDir 첨부파일을 저장할 디렉터리 (Docker 에서는 볼륨을 연결한다) */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(@DefaultValue("./data/uploads") String localDir) {
}
