package com.ync.lms.controller;

import com.ync.lms.domain.Submission;
import com.ync.lms.dto.GradeForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.SubmissionService;
import com.ync.lms.util.CsvWriter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 강사: 제출 현황(I4), 채점(I5)
 */
@Controller
@RequiredArgsConstructor
public class InstructorSubmissionController {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SubmissionService submissionService;

    // I4 제출 현황 + 미제출자
    @GetMapping("/instructor/assignments/{assignmentId}/submissions")
    public String submissions(@AuthenticationPrincipal LoginUser loginUser,
                              @PathVariable Long assignmentId,
                              Model model) {
        model.addAttribute("view", submissionService.getSubmissionStatus(assignmentId, loginUser.getId())); // B5
        return "instructor/submissions";
    }

    // 추가 기능: 점수 공개 / 공개 취소 (학교 LMS "교수가 평가 중입니다" 참고)
    @PostMapping("/instructor/assignments/{assignmentId}/publish")
    public String publish(@AuthenticationPrincipal LoginUser loginUser,
                          @PathVariable Long assignmentId,
                          RedirectAttributes redirectAttributes) {
        submissionService.setScoresPublished(assignmentId, true, loginUser.getId()); // B5
        redirectAttributes.addFlashAttribute("message", "점수를 공개했습니다. 학생이 채점 결과를 볼 수 있습니다.");
        return "redirect:/instructor/assignments/" + assignmentId + "/submissions";
    }

    @PostMapping("/instructor/assignments/{assignmentId}/unpublish")
    public String unpublish(@AuthenticationPrincipal LoginUser loginUser,
                            @PathVariable Long assignmentId,
                            RedirectAttributes redirectAttributes) {
        submissionService.setScoresPublished(assignmentId, false, loginUser.getId()); // B5
        redirectAttributes.addFlashAttribute("message", "점수 공개를 취소했습니다. 학생에게는 \"평가 중\"으로 보입니다.");
        return "redirect:/instructor/assignments/" + assignmentId + "/submissions";
    }

    // 추가 기능: 제출 현황 CSV 다운로드 (엑셀에서 바로 열림)
    @GetMapping("/instructor/assignments/{assignmentId}/submissions.csv")
    public ResponseEntity<byte[]> submissionsCsv(@AuthenticationPrincipal LoginUser loginUser,
                                                 @PathVariable Long assignmentId) {
        SubmissionService.SubmissionStatusView view =
                submissionService.getSubmissionStatus(assignmentId, loginUser.getId()); // B5
        int maxScore = view.assignment().getMaxScore();

        List<List<String>> rows = view.rows().stream()
                .map(row -> {
                    Submission s = row.submission();
                    return List.of(
                            row.student().getStudentNo(),
                            row.student().getName(),
                            row.student().getEmail(),
                            s == null ? "미제출" : s.getStatus().getLabel(),
                            s == null ? "" : s.getSubmittedAt().format(CSV_TIME),
                            (s != null && s.isGraded()) ? String.valueOf(s.getScore()) : "",
                            String.valueOf(maxScore),
                            (s != null && s.getFeedback() != null) ? s.getFeedback() : "");
                })
                .toList();

        byte[] csv = CsvWriter.toBytes(
                List.of("학번", "이름", "이메일", "상태", "제출일시", "점수", "배점", "피드백"), rows);

        String filename = view.assignment().getCourse().getTitle() + "_" + view.assignment().getTitle() + "_제출현황.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
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
