package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simulator.razorpay.entity.SimOrder;
import lombok.*;

import java.util.Map;

/**
 * Response DTO for order operations.
 * Matches Razorpay's order object structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String id;

    private String entity = "order";

    private Long amount;

    @JsonProperty("amount_paid")
    private Long amountPaid;

    @JsonProperty("amount_due")
    private Long amountDue;

    private String currency;

    private String receipt;

    private String status;

    private Integer attempts;

    private Map<String, String> notes;

    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * Converts entity to response DTO.
     */
    public static OrderResponse fromEntity(SimOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .amount(order.getAmount())
                .amountPaid(order.getAmountPaid())
                .amountDue(order.getAmountDue())
                .currency(order.getCurrency())
                .receipt(order.getReceipt())
                .status(order.getStatus().name().toLowerCase())
                .attempts(order.getAttempts())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
