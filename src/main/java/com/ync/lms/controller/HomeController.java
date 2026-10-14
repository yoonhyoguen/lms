package com.ync.lms.controller;

import com.ync.lms.domain.Role;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StatsService statsService;

    // 역할별 대시보드: 강사 = 요약 카드, 학생 = S5 내 통계
    @GetMapping("/")
    public String home(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        if (loginUser.getRole() == Role.INSTRUCTOR) {
            model.addAttribute("instructorHome", statsService.getInstructorHome(loginUser.getId()));
        } else {
            StatsService.StudentHomeView studentHome = statsService.getStudentHome(loginUser.getId());
            model.addAttribute("studentHome", studentHome);

            // 차트용 (채점된 과제의 내 점수, 100점 환산)
            model.addAttribute("chartLabels", studentHome.scored().stream().map(StatsService.ScoredAssignment::title).toList());
            model.addAttribute("chartScores", studentHome.scored().stream()
                    .map(s -> Math.round(s.percent() * 10) / 10.0)
                    .toList());
        }
        return "index"; // → templates/index.html
    }
}
