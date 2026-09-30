package com.ync.lms.security;

import com.ync.lms.domain.Role;
import com.ync.lms.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 로그인한 사용자 정보 (세션에 저장됨).
 * Entity 를 통째로 넣지 않고 필요한 값만 복사해 둔다.
 */
public class LoginUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Role role;

    public LoginUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
    }

    // ===== Spring Security 가 사용하는 메서드 =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // hasRole("INSTRUCTOR") 는 "ROLE_INSTRUCTOR" 를 찾는다
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email; // 로그인 아이디 = 이메일
    }

    @Override
    public String getPassword() {
        return password; // BCrypt 값 (입력값과 비교할 때 사용)
    }

    // ===== 우리가 쓰려고 추가한 메서드 =====

    public Long getId() {
        return id; // 본인 강좌/본인 제출 확인 (B5 등)
    }

    public String getName() {
        return name; // 상단바 표시
    }

    public Role getRole() {
        return role;
    }
}
