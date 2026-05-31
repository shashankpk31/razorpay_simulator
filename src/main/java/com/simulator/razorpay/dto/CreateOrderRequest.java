package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating an order.
 * Matches Razorpay's POST /v1/orders request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 100 paise (₹1)")
    private Long amount;  // Amount in paise

    @Builder.Default
    private String currency = "INR";

    private String receipt;  // Your internal order ID

    private Map<String, String> notes;

    @JsonProperty("partial_payment")
    @Builder.Default
    private Boolean partialPayment = false;

    // Callback URL for webhooks specific to this order
    @JsonProperty("callback_url")
    private String callbackUrl;
}
