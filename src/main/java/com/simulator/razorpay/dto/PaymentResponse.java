package com.simulator.razorpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simulator.razorpay.entity.SimPayment;
import lombok.*;

import java.util.Map;

/**
 * Response DTO for payment operations.
 * Matches Razorpay's payment object structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private String id;

    private String entity = "payment";

    private Long amount;

    private String currency;

    private String status;

    @JsonProperty("order_id")
    private String orderId;

    private String method;

    private String description;

    // Card details
    private CardDetails card;

    // UPI details
    private String vpa;

    // Netbanking details
    private String bank;

    // Wallet details
    private String wallet;

    // Contact details
    private String email;

    private String contact;

    // Timestamps
    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("authorized_at")
    private Long authorizedAt;

    @JsonProperty("captured_at")
    private Long capturedAt;

    // Fee
    private Long fee;

    private Long tax;

    // Error details
    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_source")
    private String errorSource;

    @JsonProperty("error_step")
    private String errorStep;

    @JsonProperty("error_reason")
    private String errorReason;

    // Refund tracking
    @JsonProperty("amount_refunded")
    private Long amountRefunded;

    @JsonProperty("refund_status")
    private String refundStatus;

    private Map<String, String> notes;

    @JsonProperty("acquirer_data")
    private Map<String, String> acquirerData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDetails {
        private String id;

        @JsonProperty("last4")
        private String last4;

        private String network;

        private String type;

        private String issuer;

        private Boolean international;

        private Boolean emi;
    }

    /**
     * Converts entity to response DTO.
     */
    public static PaymentResponse fromEntity(SimPayment payment) {
        PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name().toLowerCase())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .method(payment.getMethod() != null ? payment.getMethod().name().toLowerCase() : null)
                .description(payment.getDescription())
                .email(payment.getEmail())
                .contact(payment.getContact())
                .createdAt(payment.getCreatedAt())
                .authorizedAt(payment.getAuthorizedAt())
                .capturedAt(payment.getCapturedAt())
                .fee(payment.getFee())
                .tax(payment.getTax())
                .errorCode(payment.getErrorCode())
                .errorDescription(payment.getErrorDescription())
                .errorSource(payment.getErrorSource())
                .errorStep(payment.getErrorStep())
                .errorReason(payment.getErrorReason())
                .amountRefunded(payment.getAmountRefunded())
                .refundStatus(payment.getRefundStatus());

        // Add method-specific details
        if (payment.getMethod() != null) {
            switch (payment.getMethod()) {
                case CARD -> builder.card(CardDetails.builder()
                        .id(payment.getCardId())
                        .last4(payment.getCardLast4())
                        .network(payment.getCardNetwork())
                        .type(payment.getCardType())
                        .issuer(payment.getCardIssuer())
                        .international(payment.getCardInternational())
                        .emi(payment.getCardEmi())
                        .build());
                case UPI -> builder.vpa(payment.getVpa());
                case NETBANKING -> builder.bank(payment.getBank());
                case WALLET -> builder.wallet(payment.getWallet());
            }
        }

        return builder.build();
    }
}
