package com.simulator.razorpay.controller;

import com.simulator.razorpay.dto.RefundResponse;
import com.simulator.razorpay.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for Refunds.
 * Matches Razorpay's /v1/refunds endpoints.
 *
 * @see <a href="https://razorpay.com/docs/api/refunds/">Razorpay Refunds API</a>
 */
@RestController
@RequestMapping("/v1/refunds")
@RequiredArgsConstructor
@Slf4j
public class RefundApiController {

    private final RefundService refundService;

    /**
     * Fetch a refund by ID.
     * GET /v1/refunds/{id}
     */
    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(@PathVariable String refundId) {
        log.info("GET /v1/refunds/{}", refundId);
        RefundResponse refund = refundService.getRefund(refundId);
        return ResponseEntity.ok(refund);
    }

    /**
     * Fetch all refunds.
     * GET /v1/refunds
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRefunds(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "10") int count) {

        log.info("GET /v1/refunds - skip={}, count={}", skip, count);
        int page = skip / count;
        Page<RefundResponse> refunds = refundService.getAllRefunds(page, count);

        Map<String, Object> response = new HashMap<>();
        response.put("entity", "collection");
        response.put("count", refunds.getTotalElements());
        response.put("items", refunds.getContent());

        return ResponseEntity.ok(response);
    }
}
