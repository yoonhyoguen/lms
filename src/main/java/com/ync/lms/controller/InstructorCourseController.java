package com.ync.lms.controller;

import com.ync.lms.dto.CourseForm;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import com.ync.lms.service.CourseService;
import com.ync.lms.service.EnrollmentService;
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

@Controller
@RequestMapping("/instructor/courses") // 이 컨트롤러의 모든 주소 앞에 붙음
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;

    // 내 강좌 목록
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("courses", courseService.findMyCourses(loginUser.getId()));
        return "instructor/courses";
    }

    // I1 강좌 개설 화면
    @GetMapping("/new")
    public String createForm(@ModelAttribute CourseForm courseForm) {
        return "instructor/course-form";
    }

    // I1 강좌 개설 처리
    @PostMapping("/new")
    public String create(@AuthenticationPrincipal LoginUser loginUser,
                         @Valid @ModelAttribute CourseForm courseForm,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "instructor/course-form";
        }
        courseService.create(courseForm, loginUser.getId());
        return "redirect:/instructor/courses";
    }

    // I2 강좌 상세 + 수강생 목록
    @GetMapping("/{courseId}")
    public String detail(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long courseId,
                         Model model) {
        // B5: 본인 강좌가 아니면 여기서 403 (없으면 404)
        model.addAttribute("course", courseService.getMyCourse(courseId, loginUser.getId()));
        model.addAttribute("enrollments", enrollmentService.findStudentsOf(courseId));
        model.addAttribute("assignments", assignmentService.findByCourse(courseId));
        return "instructor/course-detail";
    }
}
