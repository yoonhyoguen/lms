package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Role;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.SubmissionRepository;
import com.ync.lms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추가 기능: 과제 마감 캘린더 (학교 LMS 캘린더 참고). 라이브러리 없이 월 달력을 직접 만든다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;

    /** 달력 한 칸 */
    public record CalendarDay(LocalDate date, boolean inMonth, boolean today, List<Assignment> dueAssignments) {
    }

    /** 한 달 달력 */
    public record CalendarMonth(YearMonth month,
                                List<List<CalendarDay>> weeks,
                                Set<Long> submittedAssignmentIds, // 학생: 이미 낸 과제 (✓ 표시)
                                boolean instructor) {

        public YearMonth previous() {
            return month.minusMonths(1);
        }

        public YearMonth next() {
            return month.plusMonths(1);
        }

        public long dueCount() {
            return weeks.stream().flatMap(List::stream)
                    .filter(CalendarDay::inMonth)
                    .mapToLong(d -> d.dueAssignments().size())
                    .sum();
        }
    }

    public CalendarMonth getMonth(LoginUser loginUser, YearMonth month) {
        boolean instructor = loginUser.getRole() == Role.INSTRUCTOR;

        // 내가 볼 수 있는 과제: 강사 = 내 강좌 과제, 학생 = 수강 중인 강좌 과제 (B2·B5 와 같은 범위)
        List<Assignment> assignments = instructor
                ? assignmentRepository.findByCourseInstructorIdOrderByEndAtAsc(loginUser.getId())
                : assignmentService.findForStudent(loginUser.getId());

        // 마감 날짜 → 과제들
        Map<LocalDate, List<Assignment>> byDueDate = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getEndAt().toLocalDate()));

        Set<Long> submittedIds = instructor ? Set.of()
                : submissionRepository.findByStudentIdOrderBySubmittedAtDesc(loginUser.getId()).stream()
                        .map(s -> s.getAssignment().getId())
                        .collect(Collectors.toSet());

        return new CalendarMonth(month, buildWeeks(month, byDueDate), submittedIds, instructor);
    }

    // 일요일 시작 달력: 앞뒤 빈칸은 지난달/다음 달 날짜로 채워 항상 7칸씩
    private List<List<CalendarDay>> buildWeeks(YearMonth month, Map<LocalDate, List<Assignment>> byDueDate) {
        LocalDate today = LocalDate.now();
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() % 7;                 // 일요일=0, 월요일=1 ... 토요일=6
        LocalDate start = first.minusDays(offset);                         // 달력 첫 칸 (일요일)
        LocalDate last = month.atEndOfMonth();
        LocalDate end = last.plusDays((DayOfWeek.SATURDAY.getValue() - last.getDayOfWeek().getValue() + 7) % 7); // 마지막 칸 (토요일)

        List<List<CalendarDay>> weeks = new ArrayList<>();
        List<CalendarDay> week = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            week.add(new CalendarDay(d, YearMonth.from(d).equals(month), d.equals(today),
                    byDueDate.getOrDefault(d, List.of())));
            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }
        return weeks;
    }
}
