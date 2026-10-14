package com.ync.lms.controller;

import com.ync.lms.domain.Course;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.CourseService;
import com.ync.lms.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class InstructorStatsController {

    private final CourseService courseService;
    private final StatsService statsService;

    // I6 통계 대시보드  /instructor/stats?courseId=3  (courseId 없으면 첫 번째 강좌)
    @GetMapping("/instructor/stats")
    public String stats(@AuthenticationPrincipal LoginUser loginUser,
                        @RequestParam(required = false) Long courseId,
                        Model model) {
        List<Course> myCourses = courseService.findMyCourses(loginUser.getId());
        model.addAttribute("courses", myCourses);

        // courseId 를 직접 넣었으면 내 강좌가 하나도 없어도 반드시 B5 검사 (남의 강좌면 403)
        if (courseId == null && myCourses.isEmpty()) {
            return "instructor/stats"; // 강좌가 없으면 안내만
        }

        Long selectedId = (courseId != null) ? courseId : myCourses.get(0).getId();
        StatsService.CourseStatsView view = statsService.getCourseStats(selectedId, loginUser.getId()); // B5
        model.addAttribute("view", view);

        // 차트용 데이터 (JS 로 넘김)
        model.addAttribute("chartLabels", view.rows().stream().map(r -> r.assignment().getTitle()).toList());
        model.addAttribute("chartRates", view.rows().stream().map(StatsService.AssignmentStatRow::submissionRate).toList());
        model.addAttribute("chartAverages", view.rows().stream()
                .map(r -> r.averagePercent() == null ? null : Math.round(r.averagePercent() * 10) / 10.0) // 소수 1자리
                .toList());
        return "instructor/stats";
    }
}
