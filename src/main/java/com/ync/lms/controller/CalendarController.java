package com.ync.lms.controller;

import com.ync.lms.security.LoginUser;
import com.ync.lms.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // 추가 기능: 마감 캘린더 (강사·학생 공통)  /calendar?month=2026-10  (없으면 이번 달)
    @GetMapping("/calendar")
    public String calendar(@AuthenticationPrincipal LoginUser loginUser,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Model model) {
        YearMonth target = (month != null) ? month : YearMonth.now();
        model.addAttribute("cal", calendarService.getMonth(loginUser, target));
        return "calendar";
    }
}
