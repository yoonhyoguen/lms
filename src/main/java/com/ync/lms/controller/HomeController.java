package com.ync.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 역할별 대시보드 (지금은 틀만)
    @GetMapping("/")
    public String home() {
        return "index"; // → templates/index.html
    }
}
