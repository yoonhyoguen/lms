package com.ync.lms.repository;

import com.ync.lms.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // B2: 이 학생이 이 강좌 수강생인가? / S1: 이미 등록했나?
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    // 학생: 내가 수강 중인 강좌 목록 (최근 등록순)
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);

    // I2 강사: 강좌별 수강생 목록 (등록순)
    List<Enrollment> findByCourseIdOrderByEnrolledAtAsc(Long courseId);

    // 강좌 삭제 규칙: 수강생이 있나?
    boolean existsByCourseId(Long courseId);

    // 강사 대시보드: 내 모든 강좌의 수강 등록 수
    long countByCourseInstructorId(Long instructorId);
}
