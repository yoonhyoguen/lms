package com.ync.lms.controller;

import com.ync.lms.domain.Role;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.StatsService;
import com.ync.lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StatsService statsService;
    private final SubmissionService submissionService;

    // 역할별 대시보드: 강사 = 요약 카드 + 과제별 제출률 차트, 학생 = S5 내 통계
    @GetMapping("/")
    public String home(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        if (loginUser.getRole() == Role.INSTRUCTOR) {
            StatsService.InstructorHomeView instructorHome = statsService.getInstructorHome(loginUser.getId());
            model.addAttribute("instructorHome", instructorHome);

            // 차트용 (과제별 제출률 %)
            model.addAttribute("rateLabels", instructorHome.rateBars().stream().map(StatsService.RateBar::label).toList());
            model.addAttribute("rateValues", instructorHome.rateBars().stream().map(StatsService.RateBar::rate).toList());
        } else {
            StatsService.StudentHomeView studentHome = statsService.getStudentHome(loginUser.getId());
            model.addAttribute("studentHome", studentHome);
            model.addAttribute("recentFeedback", submissionService.findRecentFeedback(loginUser.getId()));

            // 차트용 (채점된 과제의 내 점수, 100점 환산)
            model.addAttribute("chartLabels", studentHome.scored().stream().map(StatsService.ScoredAssignment::title).toList());
            model.addAttribute("chartScores", studentHome.scored().stream()
                    .map(s -> Math.round(s.percent() * 10) / 10.0)
                    .toList());
        }
        return "index"; // → templates/index.html
    }
}
