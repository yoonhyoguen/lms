package com.ync.lms.service;

import com.ync.lms.domain.User;
import com.ync.lms.dto.SignupForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * U1 학생 회원가입.
     * 형식 검사(@NotBlank 등)는 컨트롤러에서 끝난 상태로 들어온다.
     * 여기서는 DB 를 봐야 알 수 있는 규칙을 검사한다.
     */
    @Transactional
    public Long signup(SignupForm form) {
        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            throw new FieldValidationException("passwordConfirm", "비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new FieldValidationException("email", "이미 가입된 이메일입니다.");
        }
        if (userRepository.existsByStudentNo(form.getStudentNo())) {
            throw new FieldValidationException("studentNo", "이미 가입된 학번입니다.");
        }

        User student = User.createStudent(
                form.getStudentNo(),
                form.getName(),
                form.getEmail(),
                passwordEncoder.encode(form.getPassword()) // 암호화해서 저장
        );
        return userRepository.save(student).getId();
    }
}
