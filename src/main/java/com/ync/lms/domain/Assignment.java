package com.ync.lms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 과제 N : 1 강좌
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 4000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime startAt; // 제출 시작일시

    @Column(nullable = false)
    private LocalDateTime endAt;   // 제출 종료일시

    @Column(nullable = false)
    private int maxScore;          // 배점

    // 추가 기능: 점수 공개 여부 (공개 전에는 학생에게 "평가 중"). 기존 과제는 비공개(false)로 채워짐
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean scoresPublished;

    // 첨부파일 (선택) - 15-4 단계에서 사용
    private String fileOriginalName; // 사용자가 올린 이름 (다운로드 시 표시)
    private String fileStoredName;   // 서버에 저장한 이름 (UUID)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Assignment create(Course course, String title, String content,
                                    LocalDateTime startAt, LocalDateTime endAt, int maxScore) {
        Assignment assignment = new Assignment();
        assignment.course = course;
        assignment.update(title, content, startAt, endAt, maxScore);
        return assignment;
    }

    // 과제 수정 (I3)
    public void update(String title, String content,
                       LocalDateTime startAt, LocalDateTime endAt, int maxScore) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxScore = maxScore;
    }

    // ===== 점수 공개 (추가 기능) =====

    public void publishScores() {
        this.scoresPublished = true;
    }

    public void unpublishScores() {
        this.scoresPublished = false;
    }

    // ===== 첨부파일 =====

    public boolean hasFile() {
        return fileStoredName != null;
    }

    /**
     * 첨부파일 교체
     * @return 이전에 저장돼 있던 파일 이름 (지워야 할 파일, 없으면 null)
     */
    public String changeFile(String originalName, String storedName) {
        String oldStoredName = this.fileStoredName;
        this.fileOriginalName = originalName;
        this.fileStoredName = storedName;
        return oldStoredName;
    }

    // ===== 상태 계산 =====

    // S2 배지: 예정 / 진행중 / 마감
    public AssignmentStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startAt)) {
            return AssignmentStatus.UPCOMING;
        }
        if (now.isAfter(endAt)) {
            return AssignmentStatus.CLOSED;
        }
        return AssignmentStatus.OPEN;
    }

    // B1: 시작일시 ≤ 현재 ≤ 종료일시 일 때만 제출 가능
    public boolean isSubmittable() {
        return getStatus() == AssignmentStatus.OPEN;
    }
}
