package com.ync.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * S1 수강 등록: 참여코드 입력값
 */
@Getter
@Setter
public class JoinCodeForm {

    @NotBlank(message = "참여코드를 입력하세요.")
    private String joinCode;
}
