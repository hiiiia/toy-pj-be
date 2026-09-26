package com.yh.toy_pj.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Asset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 예: "맥북 M3"
    private String type; // 예: "LAPTOP", "SOFTWARE"
    private String owner;   // 담당자 / 실사용자 (예: 홍길동)
    private String status;  // 자산 상태 (사용중, 점검중, 반납 등)

    @ManyToOne // 여러 자산이 한 명의 유저에게 할당됨
    @JoinColumn(name = "user_id")
    private User assignedUser;
}