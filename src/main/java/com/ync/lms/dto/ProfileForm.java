package com.ync.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 내 정보 수정 (이름만. 이메일·학번은 변경 불가)
 */
@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력하세요.")
    private String name;
}
