package com.yh.toy_pj.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 이 클래스가 API 응답을 처리한다는 것을 알려줍니다. (FastAPI의 라우터 역할)
public class HelloController {

    @GetMapping("/hello") // URL 경로를 설정해 줍니다.
    public String helloWorld() {
        return "Hello World! 다온 플레이스 프로젝트 서버가 켜졌습니다!";
    }
}