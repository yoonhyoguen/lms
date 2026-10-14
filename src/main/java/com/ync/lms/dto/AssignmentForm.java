package com.ync.lms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 과제 등록/수정 폼 입력값
 */
@Getter
@Setter
public class AssignmentForm {

    // 화면 <input type="datetime-local"> 이 보내는 형식: 2026-10-14T09:00
    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm";

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 100, message = "제목은 100자 이하로 입력하세요.")
    private String title;

    @Size(max = 4000, message = "내용은 4000자 이하로 입력하세요.")
    private String content;

    @NotNull(message = "시작일시를 입력하세요.")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime startAt;

    @NotNull(message = "종료일시를 입력하세요.")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime endAt;

    @NotNull(message = "배점을 입력하세요.")
    @Min(value = 1, message = "배점은 1 이상이어야 합니다.")
    @Max(value = 1000, message = "배점은 1000 이하로 입력하세요.")
    private Integer maxScore;
}
