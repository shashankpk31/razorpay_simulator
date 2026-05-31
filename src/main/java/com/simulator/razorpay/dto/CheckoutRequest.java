package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Request DTO for processing payment in checkout.
 * Used by the Thymeleaf checkout form.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @JsonProperty("order_id")
    private String orderId;

    private String method;  // card, upi, netbanking, wallet

    // Card details
    @JsonProperty("card_number")
    private String cardNumber;

    @JsonProperty("card_expiry")
    private String cardExpiry;  // MM/YY

    @JsonProperty("card_cvv")
    private String cardCvv;

    @JsonProperty("card_name")
    private String cardName;

    // UPI details
    private String vpa;

    // Netbanking details
    private String bank;

    // Wallet details
    private String wallet;

    // Contact details
    private String email;

    private String contact;

    // For OTP verification
    private String otp;

    @JsonProperty("payment_id")
    private String paymentId;  // Used during OTP step
}
