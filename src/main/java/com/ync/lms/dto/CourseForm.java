package com.ync.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 강좌 개설 폼 입력값.
 * 참여코드와 강사는 입력받지 않는다. (서버가 정함)
 */
@Getter
@Setter
public class CourseForm {

    @NotBlank(message = "강좌명을 입력하세요.")
    @Size(max = 100, message = "강좌명은 100자 이하로 입력하세요.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이하로 입력하세요.")
    private String description;
}
