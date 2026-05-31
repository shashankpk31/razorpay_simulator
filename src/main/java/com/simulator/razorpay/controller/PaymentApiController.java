package com.simulator.razorpay.controller;

import com.simulator.razorpay.dto.*;
import com.simulator.razorpay.service.PaymentService;
import com.simulator.razorpay.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Payments.
 * Matches Razorpay's /v1/payments endpoints.
 *
 * @see <a href="https://razorpay.com/docs/api/payments/">Razorpay Payments API</a>
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentApiController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    /**
     * Fetch a payment by ID.
     * GET /v1/payments/{id}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        log.info("GET /v1/payments/{}", paymentId);
        PaymentResponse payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    /**
     * Fetch all payments.
     * GET /v1/payments
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPayments(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "10") int count) {

        log.info("GET /v1/payments - skip={}, count={}", skip, count);
        int page = skip / count;
        Page<PaymentResponse> payments = paymentService.getAllPayments(page, count);

        Map<String, Object> response = new HashMap<>();
        response.put("entity", "collection");
        response.put("count", payments.getTotalElements());
        response.put("items", payments.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * Capture a payment.
     * POST /v1/payments/{id}/capture
     */
    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable String paymentId,
            @Valid @RequestBody CapturePaymentRequest request) {

        log.info("POST /v1/payments/{}/capture - amount={}", paymentId, request.getAmount());
        PaymentResponse payment = paymentService.capturePayment(paymentId, request.getAmount());
        return ResponseEntity.ok(payment);
    }

    /**
     * Create a refund for a payment.
     * POST /v1/payments/{id}/refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable String paymentId,
            @RequestBody(required = false) CreateRefundRequest request) {

        log.info("POST /v1/payments/{}/refund", paymentId);
        if (request == null) {
            request = new CreateRefundRequest();
        }
        RefundResponse refund = refundService.createRefund(paymentId, request);
        return ResponseEntity.ok(refund);
    }

    /**
     * Fetch refunds for a payment.
     * GET /v1/payments/{id}/refunds
     */
    @GetMapping("/{paymentId}/refunds")
    public ResponseEntity<Map<String, Object>> getPaymentRefunds(@PathVariable String paymentId) {
        log.info("GET /v1/payments/{}/refunds", paymentId);

        List<RefundResponse> refunds = refundService.getRefundsForPayment(paymentId);

        Map<String, Object> response = new HashMap<>();
        response.put("entity", "collection");
        response.put("count", refunds.size());
        response.put("items", refunds);

        return ResponseEntity.ok(response);
    }
}
