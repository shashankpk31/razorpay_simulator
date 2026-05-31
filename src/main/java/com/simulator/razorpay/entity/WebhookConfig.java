package com.simulator.razorpay.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Stores webhook configuration for merchant applications.
 * Each merchant can have multiple webhook URLs for different events.
 */
@Entity
@Table(name = "webhook_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", length = 32)
    @Builder.Default
    private String accountId = "acc_simulator";  // Merchant account ID

    @Column(name = "webhook_url", length = 500, nullable = false)
    private String webhookUrl;  // URL to send webhooks to

    @Column(name = "webhook_secret", length = 64)
    private String webhookSecret;  // Secret for HMAC signature

    @Column(name = "events", length = 500)
    private String events;  // Comma-separated event types (or * for all)

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private Long createdAt = Instant.now().getEpochSecond();

    @Column(name = "alert_email", length = 100)
    private String alertEmail;  // Email for delivery failures

    @Column(length = 100)
    private String description;
}
