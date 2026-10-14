package com.ync.lms.controller;

import com.ync.lms.domain.Submission;
import com.ync.lms.dto.SubmissionForm;
import com.ync.lms.exception.BusinessRuleException;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import com.ync.lms.service.SubmissionService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/assignments")
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    // S2 내 과제 목록 (수강 중인 모든 강좌)
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("assignments", assignmentService.findForStudent(loginUser.getId()));
        return "student/assignments";
    }

    // S2 과제 상세 (B2: 수강생만) + S3 제출 영역
    @GetMapping("/{assignmentId}")
    public String detail(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long assignmentId,
                         Model model) {
        Submission submission = submissionService.findMySubmission(assignmentId, loginUser.getId()).orElse(null);

        // 이미 냈으면 낸 내용을 폼에 채워 둠 (재제출 시 수정하기 편하게)
        SubmissionForm form = new SubmissionForm();
        if (submission != null) {
            form.setContent(submission.getContent());
        }
        return renderDetail(assignmentId, loginUser, form, model);
    }

    // S3 과제 제출 / 재제출
    @PostMapping("/{assignmentId}/submit")
    public String submit(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long assignmentId,
                         @Valid @ModelAttribute SubmissionForm submissionForm,
                         BindingResult bindingResult,
                         @RequestParam(required = false) MultipartFile file,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                boolean resubmitted = submissionService.submit(assignmentId, submissionForm, file, loginUser.getId());
                redirectAttributes.addFlashAttribute("message", resubmitted ? "재제출되었습니다." : "제출되었습니다.");
                return "redirect:/student/assignments/" + assignmentId;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            } catch (BusinessRuleException e) {
                // B1 제출 기간 아님, B4 채점 완료 → 상세 화면 위에 알림
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/student/assignments/" + assignmentId;
            }
        }
        return renderDetail(assignmentId, loginUser, submissionForm, model);
    }

    private String renderDetail(Long assignmentId, LoginUser loginUser, SubmissionForm form, Model model) {
        model.addAttribute("assignment", assignmentService.getForStudent(assignmentId, loginUser.getId())); // B2
        model.addAttribute("submission", submissionService.findMySubmission(assignmentId, loginUser.getId()).orElse(null));
        model.addAttribute("submissionForm", form);
        return "student/assignment-detail";
    }
}
