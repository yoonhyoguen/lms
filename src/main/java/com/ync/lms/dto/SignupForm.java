package com.ync.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 폼 입력값 (화면 ↔ 컨트롤러).
 * Entity(User) 를 폼에 직접 쓰지 않고 입력 전용 객체를 따로 둔다.
 */
@Getter
@Setter // 폼 값을 Spring 이 채워 넣을 때 필요
public class SignupForm {

    @NotBlank(message = "학번을 입력하세요.")
    @Pattern(regexp = "\\d{0,20}", message = "학번은 숫자만 입력하세요. (최대 20자리)") // 빈 값은 @NotBlank 가 담당
    private String studentNo;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력하세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력하세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;
}
