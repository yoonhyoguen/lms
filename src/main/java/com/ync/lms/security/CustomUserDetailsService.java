package com.ync.lms.security;

import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 시 Spring Security 가 호출: "이 이메일의 사용자 정보를 줘"
 * 비밀번호 비교는 Security 가 PasswordEncoder 로 알아서 한다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일: " + email));
    }
}
