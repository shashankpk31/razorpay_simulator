package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Pre-configured test cards for simulating different payment scenarios.
 * Similar to Razorpay's test card numbers.
 */
@Entity
@Table(name = "test_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCard {

    @Id
    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "card_network", length = 20)
    private String cardNetwork;  // Visa, Mastercard, Amex, RuPay, etc.

    @Column(name = "card_type", length = 10)
    private String cardType;  // credit, debit

    @Column(length = 50)
    private String issuer;  // Bank name

    @Column(name = "simulate_result", length = 20)
    @Enumerated(EnumType.STRING)
    private SimulateResult simulateResult;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_description", length = 255)
    private String failureDescription;

    @Column(name = "processing_delay_ms")
    @Builder.Default
    private Integer processingDelayMs = 1000;

    @Column(length = 255)
    private String description;  // What this card simulates

    public enum SimulateResult {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        OTP_REQUIRED,  // 3DS simulation
        INSUFFICIENT_FUNDS,
        CARD_DECLINED,
        EXPIRED_CARD,
        INVALID_CVV
    }
}
