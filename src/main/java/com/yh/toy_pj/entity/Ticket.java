package com.yh.toy_pj.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Data;

@Entity
@Data
public class Ticket {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String status; // "WAITING", "IN_PROGRESS", "DONE"
    private String description; // 상세 내용

    @ManyToOne // 여러 티켓을 한 명의 유저(고객)가 작성할 수 있음
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne // 한 자산에 대해 여러 번의 티켓(장애)이 발생할 수 있음
    @JoinColumn(name = "asset_id")
    private Asset asset;
}