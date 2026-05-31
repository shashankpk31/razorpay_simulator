package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simulator.razorpay.entity.SimRefund;
import lombok.*;

import java.util.Map;

/**
 * Response DTO for refund operations.
 * Matches Razorpay's refund object structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundResponse {

    private String id;

    private String entity = "refund";

    private Long amount;

    private String currency;

    @JsonProperty("payment_id")
    private String paymentId;

    private String receipt;

    private String status;

    private String speed;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("processed_at")
    private Long processedAt;

    @JsonProperty("batch_id")
    private String batchId;

    private Map<String, String> notes;

    /**
     * Converts entity to response DTO.
     */
    public static RefundResponse fromEntity(SimRefund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .paymentId(refund.getPayment().getId())
                .receipt(refund.getReceipt())
                .status(refund.getStatus().name().toLowerCase())
                .speed(refund.getSpeed().name().toLowerCase())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .batchId(refund.getBatchId())
                .build();
    }
}
