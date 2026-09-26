package com.yh.toy_pj.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // PostgreSQL에서 user는 예약어이므로 users로 명명
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role; // "CLIENT" 또는 "ADMIN"
}