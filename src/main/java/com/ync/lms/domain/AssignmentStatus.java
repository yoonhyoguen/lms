package com.ync.lms.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 과제 상태 (DB에 저장하지 않고 시작/종료일시와 현재 시각으로 계산)
 */
@Getter
@RequiredArgsConstructor
public enum AssignmentStatus {

    UPCOMING("예정", "badge-secondary"),
    OPEN("진행중", "badge-success"),
    CLOSED("마감", "badge-danger");

    private final String label;      // 화면 표시 글자
    private final String badgeClass; // Bootstrap 배지 색
}
