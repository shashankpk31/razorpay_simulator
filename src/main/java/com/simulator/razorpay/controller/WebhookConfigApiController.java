package com.simulator.razorpay.controller;

import com.simulator.razorpay.entity.WebhookConfig;
import com.simulator.razorpay.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for managing webhook configurations.
 * Allows client applications to configure their webhook URLs.
 */
@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigApiController {

    private final WebhookService webhookService;

    /**
     * List all webhook configurations.
     * GET /v1/webhooks
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listConfigs() {
        List<WebhookConfig> configs = webhookService.listWebhookConfigs();
        return ResponseEntity.ok(Map.of(
                "entity", "collection",
                "count", configs.size(),
                "items", configs
        ));
    }

    /**
     * Add a new webhook configuration.
     * POST /v1/webhooks
     */
    @PostMapping
    public ResponseEntity<WebhookConfig> addConfig(@RequestBody WebhookConfigRequest request) {
        log.info("Adding webhook config: {}", request.url());
        WebhookConfig config = webhookService.addWebhookConfig(
                request.url(),
                request.secret(),
                request.events()
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Delete a webhook configuration.
     * DELETE /v1/webhooks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        log.info("Deleting webhook config: {}", id);
        webhookService.deleteWebhookConfig(id);
        return ResponseEntity.noContent().build();
    }

    public record WebhookConfigRequest(String url, String secret, String events) {}
}
