package com.ync.lms.controller;

import com.ync.lms.domain.Course;
import com.ync.lms.dto.CourseForm;
import com.ync.lms.exception.BusinessRuleException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // ===== 추가 기능: 강좌 수정 · 참여코드 재발급 · 삭제 =====

    // 강좌 수정 화면 (개설 화면 재사용)
    @GetMapping("/{courseId}/edit")
    public String editForm(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long courseId,
                           Model model) {
        Course course = courseService.getMyCourse(courseId, loginUser.getId()); // B5
        CourseForm form = new CourseForm();
        form.setTitle(course.getTitle());
        form.setDescription(course.getDescription());
        model.addAttribute("courseForm", form);
        model.addAttribute("course", course);
        return "instructor/course-form";
    }

    @PostMapping("/{courseId}/edit")
    public String edit(@AuthenticationPrincipal LoginUser loginUser,
                       @PathVariable Long courseId,
                       @Valid @ModelAttribute CourseForm courseForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("course", courseService.getMyCourse(courseId, loginUser.getId())); // B5
            return "instructor/course-form";
        }
        courseService.update(courseId, courseForm, loginUser.getId());
        redirectAttributes.addFlashAttribute("message", "강좌 정보를 수정했습니다.");
        return "redirect:/instructor/courses/" + courseId;
    }

    // 참여코드 재발급
    @PostMapping("/{courseId}/join-code")
    public String regenerateJoinCode(@AuthenticationPrincipal LoginUser loginUser,
                                     @PathVariable Long courseId,
                                     RedirectAttributes redirectAttributes) {
        String newCode = courseService.regenerateJoinCode(courseId, loginUser.getId());
        redirectAttributes.addFlashAttribute("message", "참여코드를 새로 발급했습니다: " + newCode);
        return "redirect:/instructor/courses/" + courseId;
    }

    // 강좌 삭제 (수강생·과제가 있으면 불가)
    @PostMapping("/{courseId}/delete")
    public String delete(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long courseId,
                         RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(courseId, loginUser.getId());
            redirectAttributes.addFlashAttribute("message", "강좌를 삭제했습니다.");
            return "redirect:/instructor/courses";
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/instructor/courses/" + courseId + "/edit";
        }
    }
}
