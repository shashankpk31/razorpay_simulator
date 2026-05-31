package com.simulator.razorpay.controller;

import com.simulator.razorpay.dto.CreateOrderRequest;
import com.simulator.razorpay.dto.OrderResponse;
import com.simulator.razorpay.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for Orders.
 * Matches Razorpay's /v1/orders endpoints.
 *
 * @see <a href="https://razorpay.com/docs/api/orders/">Razorpay Orders API</a>
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderApiController {

    private final OrderService orderService;

    /**
     * Create an order.
     * POST /v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /v1/orders - Creating order");
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    /**
     * Fetch an order by ID.
     * GET /v1/orders/{id}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("GET /v1/orders/{}", orderId);
        OrderResponse order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Fetch all orders.
     * GET /v1/orders
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "10") int count) {

        log.info("GET /v1/orders - skip={}, count={}", skip, count);
        int page = skip / count;
        Page<OrderResponse> orders = orderService.getAllOrders(page, count);

        Map<String, Object> response = new HashMap<>();
        response.put("entity", "collection");
        response.put("count", orders.getTotalElements());
        response.put("items", orders.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * Fetch payments for an order.
     * GET /v1/orders/{id}/payments
     */
    @GetMapping("/{orderId}/payments")
    public ResponseEntity<Map<String, Object>> getOrderPayments(@PathVariable String orderId) {
        log.info("GET /v1/orders/{}/payments", orderId);
        // This will be implemented when we have the full payment list
        Map<String, Object> response = new HashMap<>();
        response.put("entity", "collection");
        response.put("count", 0);
        response.put("items", new Object[]{});
        return ResponseEntity.ok(response);
    }
}
