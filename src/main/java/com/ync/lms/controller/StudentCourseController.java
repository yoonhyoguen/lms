package com.ync.lms.controller;

import com.ync.lms.domain.Course;
import com.ync.lms.dto.JoinCodeForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.EnrollmentService;
import com.ync.lms.service.StudentCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final EnrollmentService enrollmentService;
    private final StudentCourseService studentCourseService;

    // 추가 기능: 학생 강좌 홈 (과제 + 내 성적표)  /student/courses/3?gradedOnly=false
    @GetMapping("/{courseId}")
    public String home(@AuthenticationPrincipal LoginUser loginUser,
                       @PathVariable Long courseId,
                       @RequestParam(defaultValue = "true") boolean gradedOnly, // 평가된 과제에만 근거한 계산 (기본 체크)
                       Model model) {
        model.addAttribute("view", studentCourseService.getCourseHome(courseId, loginUser.getId(), gradedOnly)); // B2
        return "student/course-home";
    }

    // S1 수강 등록 화면 (+ 수강 중인 강좌 목록)
    @GetMapping("/join")
    public String joinForm(@AuthenticationPrincipal LoginUser loginUser,
                           @ModelAttribute JoinCodeForm joinCodeForm,
                           Model model) {
        model.addAttribute("enrollments", enrollmentService.findMyEnrollments(loginUser.getId()));
        return "student/course-join";
    }

    // S1 수강 등록 처리
    @PostMapping("/join")
    public String join(@AuthenticationPrincipal LoginUser loginUser,
                       @Valid @ModelAttribute JoinCodeForm joinCodeForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Course course = enrollmentService.enroll(joinCodeForm.getJoinCode(), loginUser.getId());
                // 성공 메시지: redirect 후 한 번만 보이는 값
                redirectAttributes.addFlashAttribute("message", "'" + course.getTitle() + "' 강좌에 등록되었습니다.");
                return "redirect:/student/courses/join";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            }
        }
        // 실패: 같은 화면 다시 (목록도 다시 담아야 함)
        model.addAttribute("enrollments", enrollmentService.findMyEnrollments(loginUser.getId()));
        return "student/course-join";
    }
}
