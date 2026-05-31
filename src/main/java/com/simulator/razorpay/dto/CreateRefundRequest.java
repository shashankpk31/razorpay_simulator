package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating a refund.
 * Matches Razorpay's POST /v1/payments/{id}/refund request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRefundRequest {

    private Long amount;  // Amount to refund in paise (optional, defaults to full payment)

    @Builder.Default
    private String speed = "normal";  // normal or optimum

    private String receipt;

    private Map<String, String> notes;

    @JsonProperty("reverse_all")
    @Builder.Default
    private Boolean reverseAll = false;  // Reverse the entire payment
}
