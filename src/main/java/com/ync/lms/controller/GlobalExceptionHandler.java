package com.ync.lms.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 모든 컨트롤러 공통 예외 처리
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // 업로드 파일이 너무 큼 (application.yaml 의 max-file-size) → 이전 화면으로 돌아가서 알림
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "파일은 10MB 이하만 올릴 수 있습니다.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
