package com.ync.lms.controller;

import com.ync.lms.dto.SignupForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 로그인 화면만 보여준다. (POST /login 처리는 Spring Security 가 함)
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    // U1 회원가입 화면 (빈 폼 객체를 화면에 넘김)
    @GetMapping("/signup")
    public String signupForm(@ModelAttribute SignupForm signupForm) {
        return "auth/signup";
    }

    // U1 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupForm signupForm,
                         BindingResult bindingResult) {
        // 1층: 형식 검사 (@NotBlank, @Email ...) 실패 → 입력값 그대로 다시 보여줌
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        // 2층: 규칙 검사 (중복, 비밀번호 확인) 는 Service 에서
        try {
            userService.signup(signupForm);
        } catch (FieldValidationException e) {
            bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            return "auth/signup";
        }

        return "redirect:/login?signup"; // 성공 → 로그인 화면에 "가입 완료" 표시
    }
}
