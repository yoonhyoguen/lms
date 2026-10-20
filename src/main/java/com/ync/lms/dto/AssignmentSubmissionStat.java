package com.ync.lms.dto;

/**
 * 과제별 제출 통계 (group by 쿼리 결과 한 줄)
 *
 * @param assignmentId   과제 id
 * @param submittedCount 제출 수
 * @param gradedCount    채점 완료 수 (score 가 있는 것)
 * @param averageScore   채점된 제출물의 평균 점수 (채점 0건이면 null)
 * @param highestScore   최고 점수 (채점 0건이면 null)
 * @param lowestScore    최저 점수 (채점 0건이면 null)
 */
public record AssignmentSubmissionStat(Long assignmentId,
                                       long submittedCount,
                                       long gradedCount,
                                       Double averageScore,
                                       Integer highestScore,
                                       Integer lowestScore) {
}
