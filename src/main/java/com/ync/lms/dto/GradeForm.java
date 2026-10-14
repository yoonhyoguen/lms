package com.ync.lms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * I5 채점 폼. 점수 범위(B6: 0 ~ 배점)는 과제마다 배점이 달라서 Service 에서 검사한다.
 */
@Getter
@Setter
public class GradeForm {

    @NotNull(message = "점수를 입력하세요.")
    private Integer score;

    @Size(max = 2000, message = "피드백은 2000자 이하로 입력하세요.")
    private String feedback;
}
