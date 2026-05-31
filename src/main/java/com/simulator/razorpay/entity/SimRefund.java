package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Represents a Razorpay Refund.
 * Maps to Razorpay's refund object structure.
 *
 * @see <a href="https://razorpay.com/docs/api/refunds/">Razorpay Refunds API</a>
 */
@Entity
@Table(name = "sim_refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimRefund {

    @Id
    @Column(length = 32)
    private String id;  // rfnd_xxxxxxxxxx

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private SimPayment payment;

    @Column(nullable = false)
    private Long amount;  // Amount in paise

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RefundSpeed speed = RefundSpeed.NORMAL;

    @Column(length = 255)
    private String receipt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private Long createdAt = Instant.now().getEpochSecond();

    @Column(name = "processed_at")
    private Long processedAt;

    @Column(name = "batch_id", length = 32)
    private String batchId;

    public enum RefundStatus {
        PENDING,    // Refund is being processed
        PROCESSED,  // Refund successful
        FAILED      // Refund failed
    }

    public enum RefundSpeed {
        NORMAL,   // 5-7 business days
        OPTIMUM   // Instant refund (if eligible)
    }
}
