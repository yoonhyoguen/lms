package com.ync.lms.exception;

/**
 * 비즈니스 규칙 위반 → 화면 위 "알림" 으로 보여주는 예외.
 * (B1 "제출 기간이 아닙니다", B4 "채점 완료된 과제", B8 "제출물이 있어 삭제 불가")
 *
 * 입력칸 아래 에러로 보여줄 것은 FieldValidationException 을 쓴다.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
