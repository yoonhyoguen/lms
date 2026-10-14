package com.ync.lms.controller;

import com.ync.lms.domain.Assignment;
import com.ync.lms.dto.AssignmentForm;
import com.ync.lms.exception.BusinessRuleException;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import com.ync.lms.service.CourseService;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/instructor/assignments")
@RequiredArgsConstructor
public class InstructorAssignmentController {

    private final AssignmentService assignmentService;
    private final CourseService courseService;

    // I3 과제 등록 화면  /instructor/assignments/new?courseId=3
    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal LoginUser loginUser,
                             @RequestParam Long courseId,
                             @ModelAttribute AssignmentForm assignmentForm,
                             Model model) {
        model.addAttribute("course", courseService.getMyCourse(courseId, loginUser.getId())); // B5

        // 편의상 기본값: 지금부터 7일 뒤 23:59 까지, 배점 100
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        assignmentForm.setStartAt(now);
        assignmentForm.setEndAt(now.plusDays(7).withHour(23).withMinute(59));
        assignmentForm.setMaxScore(100);
        return "instructor/assignment-form";
    }

    // I3 과제 등록 처리
    @PostMapping("/new")
    public String create(@AuthenticationPrincipal LoginUser loginUser,
                         @RequestParam Long courseId,
                         @Valid @ModelAttribute AssignmentForm assignmentForm,
                         BindingResult bindingResult,
                         Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                assignmentService.create(courseId, assignmentForm, loginUser.getId());
                return "redirect:/instructor/courses/" + courseId;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage()); // B7
            }
        }
        model.addAttribute("course", courseService.getMyCourse(courseId, loginUser.getId()));
        return "instructor/assignment-form";
    }

    // I3 과제 수정 화면  /instructor/assignments/5/edit
    @GetMapping("/{assignmentId}/edit")
    public String editForm(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long assignmentId,
                           Model model) {
        Assignment assignment = assignmentService.getMyAssignment(assignmentId, loginUser.getId()); // B5

        // 저장된 값으로 폼 채우기
        AssignmentForm form = new AssignmentForm();
        form.setTitle(assignment.getTitle());
        form.setContent(assignment.getContent());
        form.setStartAt(assignment.getStartAt());
        form.setEndAt(assignment.getEndAt());
        form.setMaxScore(assignment.getMaxScore());

        model.addAttribute("assignmentForm", form);
        model.addAttribute("assignment", assignment);
        model.addAttribute("course", assignment.getCourse());
        return "instructor/assignment-form";
    }

    // I3 과제 수정 처리
    @PostMapping("/{assignmentId}/edit")
    public String edit(@AuthenticationPrincipal LoginUser loginUser,
                       @PathVariable Long assignmentId,
                       @Valid @ModelAttribute AssignmentForm assignmentForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Assignment assignment = assignmentService.getMyAssignment(assignmentId, loginUser.getId()); // B5
        if (!bindingResult.hasErrors()) {
            try {
                assignmentService.update(assignmentId, assignmentForm, loginUser.getId());
                redirectAttributes.addFlashAttribute("message", "과제가 수정되었습니다.");
                return "redirect:/instructor/courses/" + assignment.getCourse().getId();
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage()); // B7
            }
        }
        model.addAttribute("assignment", assignment);
        model.addAttribute("course", assignment.getCourse());
        return "instructor/assignment-form";
    }

    // I3 과제 삭제
    @PostMapping("/{assignmentId}/delete")
    public String delete(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long assignmentId,
                         RedirectAttributes redirectAttributes) {
        try {
            Long courseId = assignmentService.delete(assignmentId, loginUser.getId());
            redirectAttributes.addFlashAttribute("message", "과제가 삭제되었습니다.");
            return "redirect:/instructor/courses/" + courseId;
        } catch (BusinessRuleException e) {
            // B8: 제출물이 있으면 삭제 불가 → 수정 화면에 알림
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/instructor/assignments/" + assignmentId + "/edit";
        }
    }
}
