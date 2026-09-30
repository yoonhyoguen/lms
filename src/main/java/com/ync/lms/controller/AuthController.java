package com.ync.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // 로그인 화면만 보여준다. (POST /login 처리는 Spring Security 가 함)
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }
}
