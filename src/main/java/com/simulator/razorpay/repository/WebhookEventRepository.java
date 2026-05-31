package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {

    @Query("SELECT w FROM WebhookEvent w WHERE w.delivered = false AND w.deliveryAttempts < ?1")
    List<WebhookEvent> findPendingWebhooks(int maxAttempts);

    Page<WebhookEvent> findByDelivered(Boolean delivered, Pageable pageable);

    @Query("SELECT w FROM WebhookEvent w ORDER BY w.createdAt DESC")
    Page<WebhookEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<WebhookEvent> findByEventType(String eventType);

    @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.delivered = true")
    Long countDelivered();

    @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.delivered = false")
    Long countPending();
}
