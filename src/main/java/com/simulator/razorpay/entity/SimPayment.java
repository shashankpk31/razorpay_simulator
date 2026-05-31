package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Represents a Razorpay Payment.
 * Maps to Razorpay's payment object structure.
 *
 * @see <a href="https://razorpay.com/docs/api/payments/">Razorpay Payments API</a>
 */
@Entity
@Table(name = "sim_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimPayment {

    @Id
    @Column(length = 32)
    private String id;  // pay_xxxxxxxxxx

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private SimOrder order;

    @Column(nullable = false)
    private Long amount;  // Amount in paise

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(length = 255)
    private String description;

    // ========== Card Details ==========
    @Column(name = "card_id", length = 32)
    private String cardId;  // card_xxxxxxxxxx

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_network", length = 20)
    private String cardNetwork;  // Visa, Mastercard, Amex, etc.

    @Column(name = "card_type", length = 10)
    private String cardType;  // credit, debit

    @Column(name = "card_issuer", length = 50)
    private String cardIssuer;  // HDFC, ICICI, etc.

    @Column(name = "card_international")
    @Builder.Default
    private Boolean cardInternational = false;

    @Column(name = "card_emi")
    @Builder.Default
    private Boolean cardEmi = false;

    // ========== UPI Details ==========
    @Column(length = 100)
    private String vpa;  // user@upi

    // ========== Netbanking Details ==========
    @Column(length = 10)
    private String bank;  // HDFC, ICIC, SBIN, etc.

    // ========== Wallet Details ==========
    @Column(length = 20)
    private String wallet;  // paytm, phonepe, amazonpay, etc.

    // ========== Contact Details ==========
    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String contact;

    // ========== Timestamps ==========
    @Column(name = "created_at")
    @Builder.Default
    private Long createdAt = Instant.now().getEpochSecond();

    @Column(name = "authorized_at")
    private Long authorizedAt;

    @Column(name = "captured_at")
    private Long capturedAt;

    // ========== Fee & Tax ==========
    private Long fee;  // Razorpay fee in paise

    private Long tax;  // GST on fee in paise

    // ========== Error Details (for failed payments) ==========
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_description", length = 255)
    private String errorDescription;

    @Column(name = "error_source", length = 50)
    private String errorSource;  // customer, bank, business, gateway

    @Column(name = "error_step", length = 50)
    private String errorStep;  // payment_initiation, payment_authentication, etc.

    @Column(name = "error_reason", length = 50)
    private String errorReason;

    // ========== Refund tracking ==========
    @Column(name = "amount_refunded")
    @Builder.Default
    private Long amountRefunded = 0L;

    @Column(name = "refund_status", length = 20)
    private String refundStatus;  // null, partial, full

    // ========== Notes ==========
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========== Acquirer Data ==========
    @Column(name = "acquirer_data", columnDefinition = "TEXT")
    private String acquirerData;  // JSON: bank_transaction_id, auth_code, etc.

    public enum PaymentStatus {
        CREATED,     // Payment initiated
        AUTHORIZED,  // Payment authorized (card blocked)
        CAPTURED,    // Payment captured (money received)
        REFUNDED,    // Fully refunded
        FAILED       // Payment failed
    }

    public enum PaymentMethod {
        CARD,
        UPI,
        NETBANKING,
        WALLET,
        EMI,
        BANK_TRANSFER
    }
}
