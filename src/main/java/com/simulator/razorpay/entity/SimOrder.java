package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a Razorpay Order.
 * Maps to Razorpay's order object structure.
 *
 * @see <a href="https://razorpay.com/docs/api/orders/">Razorpay Orders API</a>
 */
@Entity
@Table(name = "sim_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimOrder {

    @Id
    @Column(length = 32)
    private String id;  // order_xxxxxxxxxx

    @Column(nullable = false)
    private Long amount;  // Amount in paise (₹100 = 10000)

    @Column(name = "amount_paid")
    @Builder.Default
    private Long amountPaid = 0L;

    @Column(name = "amount_due")
    private Long amountDue;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 40)
    private String receipt;  // Your internal order ID

    @Column(length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    @Builder.Default
    private Integer attempts = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;  // JSON string

    @Column(name = "created_at")
    @Builder.Default
    private Long createdAt = Instant.now().getEpochSecond();

    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SimPayment> payments = new ArrayList<>();

    // Webhook callback URL for this order
    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    public enum OrderStatus {
        CREATED,    // Order created, no payment attempted
        ATTEMPTED,  // Payment attempted but not successful
        PAID        // Payment successful
    }

    @PrePersist
    public void prePersist() {
        if (amountDue == null) {
            amountDue = amount;
        }
    }
}
