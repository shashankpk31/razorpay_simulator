package com.simulator.razorpay.controller;

import com.simulator.razorpay.entity.TestCard;
import com.simulator.razorpay.entity.WebhookConfig;
import com.simulator.razorpay.entity.WebhookEvent;
import com.simulator.razorpay.repository.TestCardRepository;
import com.simulator.razorpay.service.OrderService;
import com.simulator.razorpay.service.PaymentService;
import com.simulator.razorpay.service.RefundService;
import com.simulator.razorpay.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for the admin dashboard.
 * Provides UI to view transactions, test cards, and manage webhooks.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final WebhookService webhookService;
    private final TestCardRepository testCardRepository;

    /**
     * Admin dashboard home.
     * GET /admin
     */
    @GetMapping
    public String dashboard(Model model) {
        log.info("GET /admin");

        // Get statistics
        Map<String, Object> orderStats = orderService.getStats();
        Map<String, Object> paymentStats = paymentService.getStats();
        Map<String, Object> refundStats = refundService.getStats();
        Map<String, Object> webhookStats = webhookService.getStats();

        model.addAttribute("orderStats", orderStats);
        model.addAttribute("paymentStats", paymentStats);
        model.addAttribute("refundStats", refundStats);
        model.addAttribute("webhookStats", webhookStats);

        // Recent transactions (first page)
        model.addAttribute("recentOrders", orderService.getAllOrders(0, 5).getContent());
        model.addAttribute("recentPayments", paymentService.getAllPayments(0, 5).getContent());

        return "admin/dashboard";
    }

    /**
     * Orders list.
     * GET /admin/orders
     */
    @GetMapping("/orders")
    public String orders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        model.addAttribute("orders", orderService.getAllOrders(page, size));
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/orders";
    }

    /**
     * Payments list.
     * GET /admin/payments
     */
    @GetMapping("/payments")
    public String payments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        model.addAttribute("payments", paymentService.getAllPayments(page, size));
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/payments";
    }

    /**
     * Refunds list.
     * GET /admin/refunds
     */
    @GetMapping("/refunds")
    public String refunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        model.addAttribute("refunds", refundService.getAllRefunds(page, size));
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/refunds";
    }

    /**
     * Test cards list.
     * GET /admin/test-cards
     */
    @GetMapping("/test-cards")
    public String testCards(Model model) {
        List<TestCard> cards = testCardRepository.findAll();
        model.addAttribute("testCards", cards);
        return "admin/test-cards";
    }

    /**
     * Webhook events list.
     * GET /admin/webhooks
     */
    @GetMapping("/webhooks")
    public String webhooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<WebhookEvent> events = webhookService.getAllEvents(page, size);
        List<WebhookConfig> configs = webhookService.listWebhookConfigs();

        model.addAttribute("events", events);
        model.addAttribute("configs", configs);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "admin/webhooks";
    }

    /**
     * Add webhook configuration.
     * POST /admin/webhooks/config
     */
    @PostMapping("/webhooks/config")
    public String addWebhookConfig(
            @RequestParam String webhookUrl,
            @RequestParam(required = false) String secret,
            @RequestParam(required = false) String events,
            RedirectAttributes redirectAttributes) {

        log.info("Adding webhook config: {}", webhookUrl);
        webhookService.addWebhookConfig(webhookUrl, secret, events);
        redirectAttributes.addFlashAttribute("success", "Webhook configuration added!");
        return "redirect:/admin/webhooks";
    }

    /**
     * Delete webhook configuration.
     * POST /admin/webhooks/config/{id}/delete
     */
    @PostMapping("/webhooks/config/{id}/delete")
    public String deleteWebhookConfig(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        log.info("Deleting webhook config: {}", id);
        webhookService.deleteWebhookConfig(id);
        redirectAttributes.addFlashAttribute("success", "Webhook configuration deleted!");
        return "redirect:/admin/webhooks";
    }

    /**
     * Retry a webhook.
     * POST /admin/webhooks/{eventId}/retry
     */
    @PostMapping("/webhooks/{eventId}/retry")
    public String retryWebhook(
            @PathVariable String eventId,
            RedirectAttributes redirectAttributes) {

        log.info("Retrying webhook: {}", eventId);
        webhookService.manuallyTriggerWebhook(eventId);
        redirectAttributes.addFlashAttribute("success", "Webhook retry triggered!");
        return "redirect:/admin/webhooks";
    }

    /**
     * API documentation / help page.
     * GET /admin/docs
     */
    @GetMapping("/docs")
    public String docs() {
        return "admin/docs";
    }
}
