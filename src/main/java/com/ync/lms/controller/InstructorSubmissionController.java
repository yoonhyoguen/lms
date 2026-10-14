package com.ync.lms.controller;

import com.ync.lms.domain.Submission;
import com.ync.lms.dto.GradeForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 강사: 제출 현황(I4), 채점(I5)
 */
@Controller
@RequiredArgsConstructor
public class InstructorSubmissionController {

    private final SubmissionService submissionService;

    // I4 제출 현황 + 미제출자
    @GetMapping("/instructor/assignments/{assignmentId}/submissions")
    public String submissions(@AuthenticationPrincipal LoginUser loginUser,
                              @PathVariable Long assignmentId,
                              Model model) {
        model.addAttribute("view", submissionService.getSubmissionStatus(assignmentId, loginUser.getId())); // B5
        return "instructor/submissions";
    }

    // I5 채점 화면
    @GetMapping("/instructor/submissions/{submissionId}/grade")
    public String gradeForm(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long submissionId,
                            Model model) {
        Submission submission = submissionService.getForGrading(submissionId, loginUser.getId()); // B5

        // 이미 채점했으면 기존 점수/피드백을 채워 둠 (재채점)
        GradeForm form = new GradeForm();
        form.setScore(submission.getScore());
        form.setFeedback(submission.getFeedback());

        model.addAttribute("submission", submission);
        model.addAttribute("gradeForm", form);
        return "instructor/grade";
    }

    // I5 채점 처리
    @PostMapping("/instructor/submissions/{submissionId}/grade")
    public String grade(@AuthenticationPrincipal LoginUser loginUser,
                        @PathVariable Long submissionId,
                        @Valid @ModelAttribute GradeForm gradeForm,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long assignmentId = submissionService.grade(submissionId, gradeForm, loginUser.getId());
                redirectAttributes.addFlashAttribute("message", "채점을 저장했습니다.");
                return "redirect:/instructor/assignments/" + assignmentId + "/submissions";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage()); // B6
            }
        }
        // 실패 시 화면을 다시 그릴 때는 새로 조회한다.
        // (grade() 가 예외로 롤백되면 그 전에 불러온 엔티티는 연결이 끊겨 LAZY 필드를 읽을 수 없음)
        model.addAttribute("submission", submissionService.getForGrading(submissionId, loginUser.getId()));
        return "instructor/grade";
    }
}
