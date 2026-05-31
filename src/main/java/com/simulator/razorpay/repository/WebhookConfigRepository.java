package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByActiveTrue();

    List<WebhookConfig> findByAccountId(String accountId);

    Optional<WebhookConfig> findByWebhookUrl(String webhookUrl);

    List<WebhookConfig> findByAccountIdAndActiveTrue(String accountId);
}
