package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

@Entity
@Table(name = "user_social_account")
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(nullable = false, length = 255)
    private String providerUserId;

    private SocialAccount(User user, SocialProvider provider, String providerUserId) {

        this.user = Objects.requireNonNull(user, "user");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerUserId = Objects.requireNonNull(providerUserId, "providerUserId");
    }

    public static SocialAccount create(User user, SocialProvider provider, String providerUserId) {
        return new SocialAccount(user, provider, providerUserId);
    }
}
