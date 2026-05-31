package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Represents a webhook event to be delivered.
 * Tracks delivery status and retry attempts.
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @Column(length = 32)
    private String id;  // evt_xxxxxxxxxx

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;  // payment.authorized, payment.captured, etc.

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;  // Full JSON payload

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "account_id", length = 32)
    private String accountId;

    @Builder.Default
    private Boolean delivered = false;

    @Column(name = "delivery_attempts")
    @Builder.Default
    private Integer deliveryAttempts = 0;

    @Column(name = "last_attempt_at")
    private Long lastAttemptAt;

    @Column(name = "delivered_at")
    private Long deliveredAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at")
    @Builder.Default
    private Long createdAt = Instant.now().getEpochSecond();

    // Common webhook event types (matching Razorpay)
    public static final String PAYMENT_AUTHORIZED = "payment.authorized";
    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_PAID = "order.paid";
    public static final String REFUND_CREATED = "refund.created";
    public static final String REFUND_PROCESSED = "refund.processed";
    public static final String REFUND_FAILED = "refund.failed";
}
