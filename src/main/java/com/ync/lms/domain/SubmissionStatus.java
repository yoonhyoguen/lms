package com.ync.lms.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 제출물 상태 (DB 에 저장)
 */
@Getter
@RequiredArgsConstructor
public enum SubmissionStatus {

    SUBMITTED("제출됨", "badge-primary"),
    GRADED("채점 완료", "badge-success"); // B4: 이 상태면 재제출 불가

    private final String label;
    private final String badgeClass;
}
