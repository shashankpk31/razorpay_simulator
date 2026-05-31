package com.simulator.razorpay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for capturing a payment.
 * Matches Razorpay's POST /v1/payments/{id}/capture request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapturePaymentRequest {

    @NotNull(message = "Amount is required")
    private Long amount;  // Amount to capture in paise

    private String currency;
}
