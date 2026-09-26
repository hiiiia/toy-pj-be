package com.yh.toy_pj.domain.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    @Query("select new com.yh.toy_pj.domain.user.AccountStatus(u.passwordChangedAt, u.mustChangePassword) from User u where u.id = :id")
    Optional<AccountStatus> findAccountStatusById(@Param("id") Long id);
}
