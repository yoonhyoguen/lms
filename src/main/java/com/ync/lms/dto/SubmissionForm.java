package com.ync.lms.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * S3 과제 제출 폼 (내용 또는 파일 중 하나는 필요 → Service 에서 검사)
 */
@Getter
@Setter
public class SubmissionForm {

    @Size(max = 4000, message = "내용은 4000자 이하로 입력하세요.")
    private String content;
}
