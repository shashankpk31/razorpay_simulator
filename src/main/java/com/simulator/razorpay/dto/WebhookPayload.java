package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Webhook payload structure matching Razorpay's webhook format.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookPayload {

    private String entity = "event";

    @JsonProperty("account_id")
    private String accountId;

    private String event;  // payment.authorized, payment.captured, etc.

    private String[] contains;  // ["payment"]

    private WebhookPayloadData payload;

    @JsonProperty("created_at")
    private Long createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebhookPayloadData {
        private EntityWrapper payment;
        private EntityWrapper order;
        private EntityWrapper refund;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntityWrapper {
        private Object entity;
    }

    /**
     * Creates a payment webhook payload.
     */
    public static WebhookPayload forPayment(String event, PaymentResponse payment) {
        return WebhookPayload.builder()
                .accountId("acc_simulator")
                .event(event)
                .contains(new String[]{"payment"})
                .payload(WebhookPayloadData.builder()
                        .payment(EntityWrapper.builder().entity(payment).build())
                        .build())
                .createdAt(System.currentTimeMillis() / 1000)
                .build();
    }

    /**
     * Creates an order webhook payload.
     */
    public static WebhookPayload forOrder(String event, OrderResponse order) {
        return WebhookPayload.builder()
                .accountId("acc_simulator")
                .event(event)
                .contains(new String[]{"order"})
                .payload(WebhookPayloadData.builder()
                        .order(EntityWrapper.builder().entity(order).build())
                        .build())
                .createdAt(System.currentTimeMillis() / 1000)
                .build();
    }

    /**
     * Creates a refund webhook payload.
     */
    public static WebhookPayload forRefund(String event, RefundResponse refund, PaymentResponse payment) {
        return WebhookPayload.builder()
                .accountId("acc_simulator")
                .event(event)
                .contains(new String[]{"refund", "payment"})
                .payload(WebhookPayloadData.builder()
                        .refund(EntityWrapper.builder().entity(refund).build())
                        .payment(EntityWrapper.builder().entity(payment).build())
                        .build())
                .createdAt(System.currentTimeMillis() / 1000)
                .build();
    }
}
