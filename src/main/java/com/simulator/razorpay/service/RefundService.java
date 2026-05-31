package com.simulator.razorpay.service;

import com.simulator.razorpay.dto.CreateRefundRequest;
import com.simulator.razorpay.dto.RefundResponse;
import com.simulator.razorpay.entity.SimPayment;
import com.simulator.razorpay.entity.SimRefund;
import com.simulator.razorpay.entity.WebhookEvent;
import com.simulator.razorpay.repository.RefundRepository;
import com.simulator.razorpay.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for managing refunds.
 * Implements Razorpay-compatible refund operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentService paymentService;
    private final WebhookService webhookService;

    @Value("${razorpay.simulator.delays.refund-processing-ms:2000}")
    private int refundProcessingDelayMs;

    /**
     * Creates a refund for a payment.
     * Equivalent to POST /v1/payments/{id}/refund in Razorpay API.
     */
    @Transactional
    public RefundResponse createRefund(String paymentId, CreateRefundRequest request) {
        SimPayment payment = paymentService.getPaymentEntity(paymentId);

        // Validate payment can be refunded
        if (payment.getStatus() != SimPayment.PaymentStatus.CAPTURED) {
            throw new RuntimeException("Payment must be captured to refund. Current status: " + payment.getStatus());
        }

        // Calculate refund amount
        Long refundableAmount = payment.getAmount() - payment.getAmountRefunded();
        Long refundAmount = request.getAmount() != null ? request.getAmount() : refundableAmount;

        if (refundAmount > refundableAmount) {
            throw new RuntimeException("Refund amount exceeds refundable amount. Max: " + refundableAmount);
        }

        // Create refund
        SimRefund refund = SimRefund.builder()
                .id(IdGenerator.refundId())
                .payment(payment)
                .amount(refundAmount)
                .currency(payment.getCurrency())
                .speed(request.getSpeed() != null ?
                        SimRefund.RefundSpeed.valueOf(request.getSpeed().toUpperCase()) :
                        SimRefund.RefundSpeed.NORMAL)
                .receipt(request.getReceipt())
                .build();

        refund = refundRepository.save(refund);

        // Trigger refund.created webhook
        webhookService.triggerRefundWebhook(WebhookEvent.REFUND_CREATED, refund);

        // Process refund asynchronously
        processRefundAsync(refund.getId());

        log.info("Refund {} created for payment {}: amount={}", refund.getId(), paymentId, refundAmount);
        return RefundResponse.fromEntity(refund);
    }

    /**
     * Fetches a refund by ID.
     * Equivalent to GET /v1/refunds/{id} in Razorpay API.
     */
    public RefundResponse getRefund(String refundId) {
        SimRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + refundId));
        return RefundResponse.fromEntity(refund);
    }

    /**
     * Fetches all refunds for a payment.
     */
    public List<RefundResponse> getRefundsForPayment(String paymentId) {
        return refundRepository.findByPaymentId(paymentId)
                .stream()
                .map(RefundResponse::fromEntity)
                .toList();
    }

    /**
     * Fetches all refunds with pagination.
     * Equivalent to GET /v1/refunds in Razorpay API.
     */
    public Page<RefundResponse> getAllRefunds(int page, int size) {
        Page<SimRefund> refunds = refundRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return refunds.map(RefundResponse::fromEntity);
    }

    /**
     * Gets refund statistics for admin dashboard.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "total", refundRepository.count(),
                "pending", refundRepository.findByStatus(SimRefund.RefundStatus.PENDING,
                        PageRequest.of(0, 1)).getTotalElements(),
                "processed", refundRepository.findByStatus(SimRefund.RefundStatus.PROCESSED,
                        PageRequest.of(0, 1)).getTotalElements(),
                "totalRefunded", refundRepository.getTotalRefundedAmount() != null ?
                        refundRepository.getTotalRefundedAmount() : 0L
        );
    }

    /**
     * Processes a refund asynchronously (simulates bank processing time).
     */
    @Async
    @Transactional
    public void processRefundAsync(String refundId) {
        try {
            // Simulate processing delay
            Thread.sleep(refundProcessingDelayMs);

            SimRefund refund = refundRepository.findById(refundId).orElse(null);
            if (refund == null || refund.getStatus() != SimRefund.RefundStatus.PENDING) {
                return;
            }

            // Process the refund
            refund.setStatus(SimRefund.RefundStatus.PROCESSED);
            refund.setProcessedAt(Instant.now().getEpochSecond());
            refundRepository.save(refund);

            // Update payment's refunded amount
            SimPayment payment = refund.getPayment();
            payment.setAmountRefunded(payment.getAmountRefunded() + refund.getAmount());

            if (payment.getAmountRefunded().equals(payment.getAmount())) {
                payment.setRefundStatus("full");
                payment.setStatus(SimPayment.PaymentStatus.REFUNDED);
            } else {
                payment.setRefundStatus("partial");
            }

            // Trigger refund.processed webhook
            webhookService.triggerRefundWebhook(WebhookEvent.REFUND_PROCESSED, refund);

            log.info("Refund {} processed", refundId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Refund processing interrupted: {}", refundId);
        }
    }
}
