package com.ync.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 비밀번호 변경 (현재 비밀번호 확인, 새 비밀번호 일치 여부는 Service 에서)
 */
@Getter
@Setter
public class PasswordChangeForm {

    @NotBlank(message = "현재 비밀번호를 입력하세요.")
    private String currentPassword;

    // @NotBlank + @Size 를 함께 쓰면 빈 값일 때 메시지가 2줄 → @NotNull + @Size 로 하나만 (SignupForm 과 동일)
    @NotNull(message = "새 비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "새 비밀번호를 8~64자로 입력하세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력하세요.")
    private String newPasswordConfirm;
}
