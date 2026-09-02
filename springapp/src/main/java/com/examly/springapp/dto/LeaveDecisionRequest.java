package com.examly.springapp.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LeaveDecisionRequest {
    private Long approverUserId;
    private Long substituteTeacherId;
}
