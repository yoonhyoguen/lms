package com.ync.lms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제출물 (제출 + 채점 결과를 한 줄에)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_submission_assignment_student",
        columnNames = {"assignment_id", "student_id"} // B3: 과제당 학생 1건
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(length = 4000)
    private String content;

    private String fileOriginalName;
    private String fileStoredName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    private Integer score;      // 채점 전 null

    @Column(length = 2000)
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime submittedAt; // 마지막 제출(재제출) 시각

    private LocalDateTime gradedAt;

    public static Submission create(Assignment assignment, User student, String content) {
        Submission submission = new Submission();
        submission.assignment = assignment;
        submission.student = student;
        submission.content = content;
        submission.status = SubmissionStatus.SUBMITTED;
        submission.submittedAt = LocalDateTime.now();
        return submission;
    }

    // S3 재제출: 기존 제출을 덮어쓴다 (B3)
    public void resubmit(String content) {
        this.content = content;
        this.submittedAt = LocalDateTime.now();
    }

    // I5 채점
    public void grade(int score, String feedback) {
        this.score = score;
        this.feedback = feedback;
        this.status = SubmissionStatus.GRADED;
        this.gradedAt = LocalDateTime.now();
    }

    public boolean isGraded() {
        return status == SubmissionStatus.GRADED;
    }

    // ===== 학생에게 보여 줄 때 (점수 공개 시점, 추가 기능) =====

    /** 학생이 점수·피드백을 볼 수 있나? = 채점 완료 + 강사가 점수 공개 */
    public boolean isResultVisible() {
        return isGraded() && assignment.isScoresPublished();
    }

    /** 학생 화면 상태: 제출됨 / 평가 중(채점했지만 비공개) / 채점 완료 */
    public String getStudentStatusLabel() {
        if (isGraded() && !isResultVisible()) {
            return "평가 중";
        }
        return status.getLabel();
    }

    public String getStudentStatusBadge() {
        if (isGraded() && !isResultVisible()) {
            return "badge-warning";
        }
        return status.getBadgeClass();
    }

    // ===== 첨부파일 (Assignment 와 같은 방식) =====

    public boolean hasFile() {
        return fileStoredName != null;
    }

    /** @return 이전 파일의 저장 이름 (지워야 할 파일, 없으면 null) */
    public String changeFile(String originalName, String storedName) {
        String oldStoredName = this.fileStoredName;
        this.fileOriginalName = originalName;
        this.fileStoredName = storedName;
        return oldStoredName;
    }
}
