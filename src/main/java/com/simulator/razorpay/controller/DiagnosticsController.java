package com.simulator.razorpay.controller;

import com.simulator.razorpay.repository.OrderRepository;
import com.simulator.razorpay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostics controller for debugging database and statistics issues.
 */
@RestController
@RequestMapping("/admin/api/diagnostics")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticsController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Get raw database counts and statistics for debugging.
     * GET /admin/api/diagnostics
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDiagnostics() {
        log.info("GET /admin/api/diagnostics");

        Map<String, Object> diagnostics = new HashMap<>();

        // Raw counts
        diagnostics.put("totalOrders", orderRepository.count());
        diagnostics.put("totalPayments", paymentRepository.count());

        // Order breakdown
        try {
            diagnostics.put("ordersCreated", orderRepository.countByStatus(com.simulator.razorpay.entity.SimOrder.OrderStatus.CREATED));
            diagnostics.put("ordersAttempted", orderRepository.countByStatus(com.simulator.razorpay.entity.SimOrder.OrderStatus.ATTEMPTED));
            diagnostics.put("ordersPaid", orderRepository.countByStatus(com.simulator.razorpay.entity.SimOrder.OrderStatus.PAID));
            diagnostics.put("totalRevenue", orderRepository.getTotalRevenue());
        } catch (Exception e) {
            log.error("Error fetching order stats", e);
            diagnostics.put("orderStatsError", e.getMessage());
        }

        // Payment breakdown
        try {
            diagnostics.put("paymentsCreated", paymentRepository.countByStatus(com.simulator.razorpay.entity.SimPayment.PaymentStatus.CREATED));
            diagnostics.put("paymentsAuthorized", paymentRepository.countByStatus(com.simulator.razorpay.entity.SimPayment.PaymentStatus.AUTHORIZED));
            diagnostics.put("paymentsCaptured", paymentRepository.countByStatus(com.simulator.razorpay.entity.SimPayment.PaymentStatus.CAPTURED));
            diagnostics.put("paymentsFailed", paymentRepository.countByStatus(com.simulator.razorpay.entity.SimPayment.PaymentStatus.FAILED));
            diagnostics.put("totalCapturedAmount", paymentRepository.getTotalCapturedAmount());
        } catch (Exception e) {
            log.error("Error fetching payment stats", e);
            diagnostics.put("paymentStatsError", e.getMessage());
        }

        // Recent orders
        try {
            var recentOrders = orderRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 5)
            );
            diagnostics.put("recentOrdersCount", recentOrders.getTotalElements());
            diagnostics.put("recentOrders", recentOrders.getContent().stream()
                .map(o -> Map.of(
                    "id", o.getId(),
                    "amount", o.getAmount(),
                    "status", o.getStatus().name(),
                    "amountPaid", o.getAmountPaid(),
                    "createdAt", o.getCreatedAt()
                ))
                .toList());
        } catch (Exception e) {
            log.error("Error fetching recent orders", e);
            diagnostics.put("recentOrdersError", e.getMessage());
        }

        // Recent payments
        try {
            var recentPayments = paymentRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 5)
            );
            diagnostics.put("recentPaymentsCount", recentPayments.getTotalElements());
            diagnostics.put("recentPayments", recentPayments.getContent().stream()
                .map(p -> Map.of(
                    "id", p.getId(),
                    "amount", p.getAmount(),
                    "status", p.getStatus().name(),
                    "method", p.getMethod() != null ? p.getMethod().name() : "N/A",
                    "createdAt", p.getCreatedAt()
                ))
                .toList());
        } catch (Exception e) {
            log.error("Error fetching recent payments", e);
            diagnostics.put("recentPaymentsError", e.getMessage());
        }

        log.info("Diagnostics: {}", diagnostics);
        return ResponseEntity.ok(diagnostics);
    }
}
