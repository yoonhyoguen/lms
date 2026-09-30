package com.ync.lms.exception;

import lombok.Getter;

/**
 * Service 의 규칙 검사에서 실패했을 때, "어느 입력칸" 의 문제인지 함께 알려주는 예외.
 * 컨트롤러가 받아서 해당 입력칸 아래에 에러 메시지로 보여준다.
 * (회원가입 중복, B6 점수 범위, B7 종료일시 등에서 재사용)
 */
@Getter
public class FieldValidationException extends RuntimeException {

    private final String field; // 폼 필드 이름 (예: "email")

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
