package com.simulator.razorpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.razorpay.dto.CheckoutRequest;
import com.simulator.razorpay.dto.PaymentResponse;
import com.simulator.razorpay.entity.SimOrder;
import com.simulator.razorpay.entity.SimPayment;
import com.simulator.razorpay.entity.TestCard;
import com.simulator.razorpay.entity.WebhookEvent;
import com.simulator.razorpay.repository.PaymentRepository;
import com.simulator.razorpay.repository.TestCardRepository;
import com.simulator.razorpay.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing payments.
 * Implements Razorpay-compatible payment operations with simulation logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TestCardRepository testCardRepository;
    private final OrderService orderService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.simulator.auto-capture:false}")
    private boolean autoCapture;

    @Value("${razorpay.simulator.delays.payment-processing-ms:1000}")
    private int processingDelayMs;

    /**
     * Processes a payment from checkout.
     * Simulates card/UPI/netbanking/wallet payments.
     */
    @Transactional
    public PaymentResponse processPayment(CheckoutRequest request) throws InterruptedException {
        log.info("Processing payment for order: {}, method: {}", request.getOrderId(), request.getMethod());

        SimOrder order = orderService.getOrderEntity(request.getOrderId());

        if (order.getStatus() == SimOrder.OrderStatus.PAID) {
            throw new RuntimeException("Order already paid");
        }

        // Create payment record
        SimPayment payment = createPaymentRecord(order, request);

        // Simulate processing delay
        Thread.sleep(processingDelayMs);

        // Process based on payment method
        SimPayment.PaymentStatus result = processPaymentByMethod(payment, request);
        payment.setStatus(result);

        if (result == SimPayment.PaymentStatus.AUTHORIZED) {
            payment.setAuthorizedAt(Instant.now().getEpochSecond());

            // Calculate fee (2% + GST, like Razorpay)
            long fee = (long) (payment.getAmount() * 0.02);
            long tax = (long) (fee * 0.18);
            payment.setFee(fee);
            payment.setTax(tax);

            // Generate acquirer data
            payment.setAcquirerData(generateAcquirerData(payment));

            // Auto-capture if enabled
            if (autoCapture) {
                payment.setStatus(SimPayment.PaymentStatus.CAPTURED);
                payment.setCapturedAt(Instant.now().getEpochSecond());
                orderService.updateOrderAfterPayment(order.getId(), true, payment.getAmount());
                webhookService.triggerWebhook(WebhookEvent.PAYMENT_CAPTURED, payment);
            } else {
                webhookService.triggerWebhook(WebhookEvent.PAYMENT_AUTHORIZED, payment);
            }
        } else if (result == SimPayment.PaymentStatus.FAILED) {
            orderService.updateOrderAfterPayment(order.getId(), false, 0L);
            webhookService.triggerWebhook(WebhookEvent.PAYMENT_FAILED, payment);
        }

        payment = paymentRepository.save(payment);
        log.info("Payment {} processed: status={}", payment.getId(), payment.getStatus());

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Captures an authorized payment.
     * Equivalent to POST /v1/payments/{id}/capture in Razorpay API.
     */
    @Transactional
    public PaymentResponse capturePayment(String paymentId, Long amount) {
        SimPayment payment = getPaymentEntity(paymentId);

        if (payment.getStatus() != SimPayment.PaymentStatus.AUTHORIZED) {
            throw new RuntimeException("Payment must be authorized to capture. Current status: " + payment.getStatus());
        }

        if (amount != null && amount > payment.getAmount()) {
            throw new RuntimeException("Capture amount cannot exceed authorized amount");
        }

        long captureAmount = amount != null ? amount : payment.getAmount();
        payment.setAmount(captureAmount);
        payment.setStatus(SimPayment.PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now().getEpochSecond());

        payment = paymentRepository.save(payment);

        // Update order
        orderService.updateOrderAfterPayment(payment.getOrder().getId(), true, captureAmount);

        // Trigger webhook
        webhookService.triggerWebhook(WebhookEvent.PAYMENT_CAPTURED, payment);

        // Trigger order.paid webhook
        webhookService.triggerOrderPaidWebhook(payment.getOrder());

        log.info("Payment {} captured: amount={}", paymentId, captureAmount);
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Fetches a payment by ID.
     * Equivalent to GET /v1/payments/{id} in Razorpay API.
     */
    public PaymentResponse getPayment(String paymentId) {
        return PaymentResponse.fromEntity(getPaymentEntity(paymentId));
    }

    /**
     * Fetches all payments with pagination.
     * Equivalent to GET /v1/payments in Razorpay API.
     */
    public Page<PaymentResponse> getAllPayments(int page, int size) {
        Page<SimPayment> payments = paymentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return payments.map(PaymentResponse::fromEntity);
    }

    /**
     * Gets payment entity for internal use.
     */
    public SimPayment getPaymentEntity(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    /**
     * Gets payment statistics for admin dashboard.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "total", paymentRepository.count(),
                "authorized", paymentRepository.countByStatus(SimPayment.PaymentStatus.AUTHORIZED),
                "captured", paymentRepository.countByStatus(SimPayment.PaymentStatus.CAPTURED),
                "failed", paymentRepository.countByStatus(SimPayment.PaymentStatus.FAILED),
                "totalCaptured", paymentRepository.getTotalCapturedAmount() != null ?
                        paymentRepository.getTotalCapturedAmount() : 0L
        );
    }

    // ===================== Private Helper Methods =====================

    private SimPayment createPaymentRecord(SimOrder order, CheckoutRequest request) {
        SimPayment.PaymentMethod method = SimPayment.PaymentMethod.valueOf(
                request.getMethod().toUpperCase()
        );

        SimPayment.SimPaymentBuilder builder = SimPayment.builder()
                .id(IdGenerator.paymentId())
                .order(order)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .method(method)
                .email(request.getEmail())
                .contact(request.getContact());

        // Set method-specific details
        switch (method) {
            case CARD -> {
                String cardNumber = request.getCardNumber().replaceAll("\\s", "");
                builder.cardLast4(cardNumber.substring(cardNumber.length() - 4))
                        .cardNetwork(detectCardNetwork(cardNumber))
                        .cardType("credit")  // Default
                        .cardIssuer("Simulator Bank")
                        .cardId(IdGenerator.cardId());
            }
            case UPI -> builder.vpa(request.getVpa());
            case NETBANKING -> builder.bank(request.getBank());
            case WALLET -> builder.wallet(request.getWallet());
        }

        return builder.build();
    }

    private SimPayment.PaymentStatus processPaymentByMethod(SimPayment payment, CheckoutRequest request) {
        return switch (payment.getMethod()) {
            case CARD -> processCardPayment(payment, request);
            case UPI -> processUpiPayment(payment, request);
            case NETBANKING -> processNetbankingPayment(payment, request);
            case WALLET -> processWalletPayment(payment, request);
            default -> SimPayment.PaymentStatus.FAILED;
        };
    }

    private SimPayment.PaymentStatus processCardPayment(SimPayment payment, CheckoutRequest request) {
        String cardNumber = request.getCardNumber().replaceAll("\\s", "");

        // Check if it's a test card with predefined behavior
        Optional<TestCard> testCard = testCardRepository.findByCardNumber(cardNumber);

        if (testCard.isPresent()) {
            TestCard tc = testCard.get();
            payment.setCardNetwork(tc.getCardNetwork());
            payment.setCardType(tc.getCardType());
            payment.setCardIssuer(tc.getIssuer());

            return switch (tc.getSimulateResult()) {
                case SUCCESS -> SimPayment.PaymentStatus.AUTHORIZED;
                case FAILURE, CARD_DECLINED, INSUFFICIENT_FUNDS, EXPIRED_CARD, INVALID_CVV -> {
                    payment.setErrorCode(tc.getFailureCode());
                    payment.setErrorDescription(tc.getFailureDescription());
                    payment.setErrorSource("bank");
                    payment.setErrorStep("payment_authorization");
                    payment.setErrorReason(tc.getSimulateResult().name().toLowerCase());
                    yield SimPayment.PaymentStatus.FAILED;
                }
                default -> SimPayment.PaymentStatus.AUTHORIZED;
            };
        }

        // Default: authorize the payment (for any card not in test cards)
        return SimPayment.PaymentStatus.AUTHORIZED;
    }

    private SimPayment.PaymentStatus processUpiPayment(SimPayment payment, CheckoutRequest request) {
        String vpa = request.getVpa();

        // Simulate different scenarios based on VPA
        if (vpa.contains("fail")) {
            payment.setErrorCode("PAYMENT_DECLINED");
            payment.setErrorDescription("Payment declined by UPI");
            payment.setErrorSource("customer");
            payment.setErrorReason("payment_declined");
            return SimPayment.PaymentStatus.FAILED;
        }

        return SimPayment.PaymentStatus.AUTHORIZED;
    }

    private SimPayment.PaymentStatus processNetbankingPayment(SimPayment payment, CheckoutRequest request) {
        // All netbanking payments succeed in simulator
        return SimPayment.PaymentStatus.AUTHORIZED;
    }

    private SimPayment.PaymentStatus processWalletPayment(SimPayment payment, CheckoutRequest request) {
        // All wallet payments succeed in simulator
        return SimPayment.PaymentStatus.AUTHORIZED;
    }

    private String detectCardNetwork(String cardNumber) {
        if (cardNumber.startsWith("4")) return "Visa";
        if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return "Mastercard";
        if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) return "Amex";
        if (cardNumber.startsWith("6")) return "RuPay";
        return "Unknown";
    }

    private String generateAcquirerData(SimPayment payment) {
        Map<String, String> data = new HashMap<>();
        data.put("bank_transaction_id", IdGenerator.bankTransactionId());
        data.put("auth_code", IdGenerator.authCode());

        if (payment.getMethod() == SimPayment.PaymentMethod.UPI) {
            data.put("rrn", String.valueOf(System.currentTimeMillis()));
            data.put("upi_transaction_id", "SIM" + System.currentTimeMillis());
        }

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
