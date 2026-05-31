package com.simulator.razorpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.razorpay.dto.CreateOrderRequest;
import com.simulator.razorpay.dto.OrderResponse;
import com.simulator.razorpay.entity.SimOrder;
import com.simulator.razorpay.repository.OrderRepository;
import com.simulator.razorpay.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for managing orders.
 * Implements Razorpay-compatible order operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new order.
     * Equivalent to POST /v1/orders in Razorpay API.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order: amount={}, currency={}, receipt={}",
                request.getAmount(), request.getCurrency(), request.getReceipt());

        SimOrder order = SimOrder.builder()
                .id(IdGenerator.orderId())
                .amount(request.getAmount())
                .amountDue(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .receipt(request.getReceipt())
                .notes(convertNotesToJson(request.getNotes()))
                .callbackUrl(request.getCallbackUrl())
                .build();

        order = orderRepository.save(order);
        log.info("Order created: {}", order.getId());

        return OrderResponse.fromEntity(order);
    }

    /**
     * Fetches an order by ID.
     * Equivalent to GET /v1/orders/{id} in Razorpay API.
     */
    public OrderResponse getOrder(String orderId) {
        SimOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    /**
     * Fetches all orders with pagination.
     * Equivalent to GET /v1/orders in Razorpay API.
     */
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Page<SimOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return orders.map(OrderResponse::fromEntity);
    }

    /**
     * Gets order entity for internal use.
     */
    public SimOrder getOrderEntity(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * Updates order status after payment attempt.
     */
    @Transactional
    public void updateOrderAfterPayment(String orderId, boolean success, Long amountPaid) {
        SimOrder order = getOrderEntity(orderId);
        order.setAttempts(order.getAttempts() + 1);

        if (success) {
            order.setAmountPaid(amountPaid);
            order.setAmountDue(order.getAmount() - amountPaid);
            order.setStatus(order.getAmountDue() <= 0 ?
                    SimOrder.OrderStatus.PAID : SimOrder.OrderStatus.ATTEMPTED);
        } else {
            order.setStatus(SimOrder.OrderStatus.ATTEMPTED);
        }

        orderRepository.save(order);
        log.info("Order {} updated: status={}, amountPaid={}", orderId, order.getStatus(), amountPaid);
    }

    /**
     * Gets order statistics for admin dashboard.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "total", orderRepository.count(),
                "created", orderRepository.countByStatus(SimOrder.OrderStatus.CREATED),
                "attempted", orderRepository.countByStatus(SimOrder.OrderStatus.ATTEMPTED),
                "paid", orderRepository.countByStatus(SimOrder.OrderStatus.PAID),
                "totalRevenue", orderRepository.getTotalRevenue() != null ?
                        orderRepository.getTotalRevenue() : 0L
        );
    }

    private String convertNotesToJson(Map<String, String> notes) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(notes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notes", e);
            return null;
        }
    }
}
