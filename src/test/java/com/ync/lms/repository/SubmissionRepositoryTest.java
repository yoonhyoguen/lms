package com.ync.lms.repository;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Course;
import com.ync.lms.domain.Submission;
import com.ync.lms.domain.User;
import com.ync.lms.dto.AssignmentSubmissionStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I6 통계 group by 쿼리 테스트 (실제 H2 DB 에 데이터를 넣고 확인)
 */
@DataJpaTest
class SubmissionRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    SubmissionRepository submissionRepository;

    Course course;
    Assignment hw1;
    Assignment hw2;

    @BeforeEach
    void setUp() {
        User instructor = em.persist(User.createInstructor("강사", "t@lms.com", "pw"));
        User s1 = em.persist(User.createStudent("1", "학생1", "s1@lms.com", "pw"));
        User s2 = em.persist(User.createStudent("2", "학생2", "s2@lms.com", "pw"));
        User s3 = em.persist(User.createStudent("3", "학생3", "s3@lms.com", "pw"));

        course = em.persist(Course.create("자바", null, "AAAA2222", instructor));
        Course otherCourse = em.persist(Course.create("다른 강좌", null, "BBBB3333", instructor));

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        hw1 = em.persist(Assignment.create(course, "HW1", null, start, end, 50));
        hw2 = em.persist(Assignment.create(course, "HW2", null, start, end, 100));
        Assignment otherHw = em.persist(Assignment.create(otherCourse, "OTHER", null, start, end, 100));

        // HW1: 3명 제출, 2명 채점 (40점, 50점)
        Submission a = Submission.create(hw1, s1, "a");
        a.grade(40, "ok");
        Submission b = Submission.create(hw1, s2, "b");
        b.grade(50, "good");
        em.persist(a);
        em.persist(b);
        em.persist(Submission.create(hw1, s3, "c"));

        // HW2: 1명 제출, 채점 전
        em.persist(Submission.create(hw2, s1, "d"));

        // 다른 강좌 제출물 (집계에 섞이면 안 됨)
        Submission other = Submission.create(otherHw, s1, "x");
        other.grade(100, "");
        em.persist(other);

        em.flush();
        em.clear();
    }

    @Test
    void 과제별_제출수_채점수_평균_최고_최저점수를_group_by로_계산한다() {
        Map<Long, AssignmentSubmissionStat> stats = submissionRepository.countByAssignmentInCourse(course.getId())
                .stream()
                .collect(Collectors.toMap(AssignmentSubmissionStat::assignmentId, Function.identity()));

        assertThat(stats).hasSize(2); // 다른 강좌 과제는 포함되지 않음

        AssignmentSubmissionStat stat1 = stats.get(hw1.getId());
        assertThat(stat1.submittedCount()).isEqualTo(3);
        assertThat(stat1.gradedCount()).isEqualTo(2);       // 채점 전(null) 은 count(score) 에서 빠짐
        assertThat(stat1.averageScore()).isEqualTo(45.0);   // (40 + 50) / 2, null 제외
        assertThat(stat1.highestScore()).isEqualTo(50);
        assertThat(stat1.lowestScore()).isEqualTo(40);      // 채점 전(null) 은 최저에서도 빠짐
        assertThat(stat1.highestScore()).isEqualTo(50);
        assertThat(stat1.lowestScore()).isEqualTo(40);      // 채점 전(null) 은 최저에서도 빠짐

        AssignmentSubmissionStat stat2 = stats.get(hw2.getId());
        assertThat(stat2.submittedCount()).isEqualTo(1);
        assertThat(stat2.gradedCount()).isZero();
        assertThat(stat2.averageScore()).isNull();          // 채점 0건 → 평균 없음
        assertThat(stat2.highestScore()).isNull();
        assertThat(stat2.lowestScore()).isNull();
        assertThat(stat2.highestScore()).isNull();
        assertThat(stat2.lowestScore()).isNull();
    }

    @Test
    void 제출물이_없는_강좌는_빈_결과() {
        User instructor = em.persist(User.createInstructor("강사2", "t2@lms.com", "pw"));
        Course empty = em.persist(Course.create("빈 강좌", null, "CCCC4444", instructor));

        List<AssignmentSubmissionStat> stats = submissionRepository.countByAssignmentInCourse(empty.getId());

        assertThat(stats).isEmpty();
    }
}
