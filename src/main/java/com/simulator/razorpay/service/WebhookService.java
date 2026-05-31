package com.simulator.razorpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.razorpay.dto.*;
import com.simulator.razorpay.entity.*;
import com.simulator.razorpay.repository.WebhookConfigRepository;
import com.simulator.razorpay.repository.WebhookEventRepository;
import com.simulator.razorpay.util.IdGenerator;
import com.simulator.razorpay.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for managing webhook delivery.
 * Implements Razorpay-compatible webhook signatures and retry logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${razorpay.simulator.webhook.secret:whsec_simulator_webhook_secret}")
    private String webhookSecret;

    @Value("${razorpay.simulator.webhook.retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${razorpay.simulator.webhook.retry-delay-ms:5000}")
    private int retryDelayMs;

    /**
     * Triggers a payment webhook.
     */
    @Async
    public void triggerWebhook(String eventType, SimPayment payment) {
        try {
            PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
            WebhookPayload payload = WebhookPayload.forPayment(eventType, paymentResponse);
            String payloadJson = objectMapper.writeValueAsString(payload);

            createAndDeliverWebhook(eventType, payloadJson);
        } catch (Exception e) {
            log.error("Failed to trigger webhook: {} for payment {}", eventType, payment.getId(), e);
        }
    }

    /**
     * Triggers an order webhook.
     */
    @Async
    public void triggerOrderPaidWebhook(SimOrder order) {
        try {
            OrderResponse orderResponse = OrderResponse.fromEntity(order);
            WebhookPayload payload = WebhookPayload.forOrder(WebhookEvent.ORDER_PAID, orderResponse);
            String payloadJson = objectMapper.writeValueAsString(payload);

            createAndDeliverWebhook(WebhookEvent.ORDER_PAID, payloadJson);
        } catch (Exception e) {
            log.error("Failed to trigger order.paid webhook for order {}", order.getId(), e);
        }
    }

    /**
     * Triggers a refund webhook.
     */
    @Async
    public void triggerRefundWebhook(String eventType, SimRefund refund) {
        try {
            RefundResponse refundResponse = RefundResponse.fromEntity(refund);
            PaymentResponse paymentResponse = PaymentResponse.fromEntity(refund.getPayment());
            WebhookPayload payload = WebhookPayload.forRefund(eventType, refundResponse, paymentResponse);
            String payloadJson = objectMapper.writeValueAsString(payload);

            createAndDeliverWebhook(eventType, payloadJson);
        } catch (Exception e) {
            log.error("Failed to trigger webhook: {} for refund {}", eventType, refund.getId(), e);
        }
    }

    /**
     * Creates a webhook event and delivers it to all configured URLs.
     */
    private void createAndDeliverWebhook(String eventType, String payloadJson) {
        List<WebhookConfig> configs = webhookConfigRepository.findByActiveTrue();

        if (configs.isEmpty()) {
            log.debug("No webhook configs found. Storing event for manual retrieval.");
            // Still store the event for admin dashboard
            WebhookEvent event = WebhookEvent.builder()
                    .id(IdGenerator.eventId())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build();
            webhookEventRepository.save(event);
            return;
        }

        for (WebhookConfig config : configs) {
            // Check if this config handles this event type
            if (config.getEvents() != null && !config.getEvents().equals("*") &&
                    !config.getEvents().contains(eventType)) {
                continue;
            }

            WebhookEvent event = WebhookEvent.builder()
                    .id(IdGenerator.eventId())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .webhookUrl(config.getWebhookUrl())
                    .accountId(config.getAccountId())
                    .build();

            event = webhookEventRepository.save(event);
            deliverWebhook(event, config.getWebhookSecret() != null ?
                    config.getWebhookSecret() : webhookSecret);
        }
    }

    /**
     * Delivers a single webhook with retry logic.
     */
    @Async
    public void deliverWebhook(WebhookEvent event, String secret) {
        if (event.getWebhookUrl() == null || event.getWebhookUrl().isEmpty()) {
            log.debug("No webhook URL for event {}", event.getId());
            return;
        }

        try {
            String signature = SignatureUtil.generateSignature(event.getPayload(), secret);

            log.info("Delivering webhook {} to {}", event.getId(), event.getWebhookUrl());

            WebClient client = webClientBuilder.build();
            client.post()
                    .uri(event.getWebhookUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Razorpay-Signature", signature)
                    .header("X-Razorpay-Event-Id", event.getId())
                    .bodyValue(event.getPayload())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response -> {
                        event.setDelivered(true);
                        event.setDeliveredAt(Instant.now().getEpochSecond());
                        event.setHttpStatus(response.getStatusCode().value());
                        event.setDeliveryAttempts(event.getDeliveryAttempts() + 1);
                        webhookEventRepository.save(event);
                        log.info("Webhook {} delivered successfully", event.getId());
                    })
                    .doOnError(error -> {
                        event.setDeliveryAttempts(event.getDeliveryAttempts() + 1);
                        event.setLastAttemptAt(Instant.now().getEpochSecond());
                        event.setLastError(error.getMessage());
                        webhookEventRepository.save(event);
                        log.warn("Webhook {} delivery failed: {}", event.getId(), error.getMessage());
                    })
                    .onErrorResume(e -> Mono.empty())
                    .subscribe();

        } catch (Exception e) {
            log.error("Failed to deliver webhook {}: {}", event.getId(), e.getMessage());
        }
    }

    /**
     * Retries failed webhooks periodically.
     */
    @Scheduled(fixedDelayString = "${razorpay.simulator.webhook.retry-delay-ms:5000}")
    public void retryFailedWebhooks() {
        List<WebhookEvent> pendingWebhooks = webhookEventRepository.findPendingWebhooks(maxRetryAttempts);

        for (WebhookEvent event : pendingWebhooks) {
            if (event.getWebhookUrl() != null) {
                log.info("Retrying webhook {} (attempt {})", event.getId(), event.getDeliveryAttempts() + 1);
                deliverWebhook(event, webhookSecret);
            }
        }
    }

    /**
     * Fetches all webhook events with pagination.
     */
    public Page<WebhookEvent> getAllEvents(int page, int size) {
        return webhookEventRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * Manually triggers a webhook for testing.
     */
    public void manuallyTriggerWebhook(String eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        event.setDelivered(false);
        event.setDeliveryAttempts(0);
        webhookEventRepository.save(event);

        deliverWebhook(event, webhookSecret);
    }

    /**
     * Gets webhook statistics for admin dashboard.
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "total", webhookEventRepository.count(),
                "delivered", webhookEventRepository.countDelivered(),
                "pending", webhookEventRepository.countPending()
        );
    }

    /**
     * Adds a new webhook configuration.
     */
    public WebhookConfig addWebhookConfig(String webhookUrl, String secret, String events) {
        WebhookConfig config = WebhookConfig.builder()
                .webhookUrl(webhookUrl)
                .webhookSecret(secret)
                .events(events != null ? events : "*")
                .build();
        return webhookConfigRepository.save(config);
    }

    /**
     * Lists all webhook configurations.
     */
    public List<WebhookConfig> listWebhookConfigs() {
        return webhookConfigRepository.findAll();
    }

    /**
     * Deletes a webhook configuration.
     */
    public void deleteWebhookConfig(Long id) {
        webhookConfigRepository.deleteById(id);
    }
}
