package com.ync.lms.service;

import com.ync.lms.domain.User;
import com.ync.lms.dto.PasswordChangeForm;
import com.ync.lms.dto.ProfileForm;
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

    // ===== 내 정보 (추가 기능) =====

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * 이름 변경
     * @return 바뀐 사용자 (세션의 로그인 정보를 새로 만들 때 사용)
     */
    @Transactional
    public User changeName(Long userId, ProfileForm form) {
        User user = getUser(userId);
        user.changeName(form.getName().trim());
        return user;
    }

    /**
     * 비밀번호 변경: 현재 비밀번호가 맞아야 하고, 새 비밀번호는 확인과 일치 + 현재와 달라야 한다.
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeForm form) {
        User user = getUser(userId);

        // 저장된 값은 BCrypt 라서 equals 로 비교할 수 없음 → matches(입력 원문, 저장된 암호문)
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new FieldValidationException("currentPassword", "현재 비밀번호가 올바르지 않습니다.");
        }
        if (!form.getNewPassword().equals(form.getNewPasswordConfirm())) {
            throw new FieldValidationException("newPasswordConfirm", "새 비밀번호가 일치하지 않습니다.");
        }
        if (form.getNewPassword().equals(form.getCurrentPassword())) {
            throw new FieldValidationException("newPassword", "현재 비밀번호와 다른 비밀번호를 입력하세요.");
        }

        user.changePassword(passwordEncoder.encode(form.getNewPassword()));
    }
}
