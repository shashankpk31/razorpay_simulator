package com.simulator.razorpay.controller;

import com.simulator.razorpay.dto.CheckoutRequest;
import com.simulator.razorpay.dto.OrderResponse;
import com.simulator.razorpay.dto.PaymentResponse;
import com.simulator.razorpay.entity.TestCard;
import com.simulator.razorpay.repository.TestCardRepository;
import com.simulator.razorpay.service.OrderService;
import com.simulator.razorpay.service.PaymentService;
import com.simulator.razorpay.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the checkout UI.
 * Provides Thymeleaf-based checkout pages that look like Razorpay.
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TestCardRepository testCardRepository;

    @Value("${razorpay.simulator.key-secret:sim_secret_key_12345}")
    private String keySecret;

    /**
     * Displays the checkout page for an order.
     * GET /checkout/{orderId}
     */
    @GetMapping("/{orderId}")
    public String showCheckout(@PathVariable String orderId, Model model) {
        log.info("GET /checkout/{}", orderId);

        try {
            OrderResponse order = orderService.getOrder(orderId);

            if ("paid".equals(order.getStatus())) {
                return "redirect:/checkout/" + orderId + "/success";
            }

            model.addAttribute("order", order);
            model.addAttribute("amount", formatAmount(order.getAmount()));
            model.addAttribute("amountInRupees", order.getAmount() / 100.0);

            // Banks for netbanking
            model.addAttribute("banks", getBankList());

            // Wallets
            model.addAttribute("wallets", getWalletList());

            // Test cards for reference
            List<TestCard> testCards = testCardRepository.findAll();
            model.addAttribute("testCards", testCards);

            return "checkout";
        } catch (Exception e) {
            log.error("Error loading checkout for order {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Order not found: " + orderId);
            return "error";
        }
    }

    /**
     * Processes the payment form submission.
     * POST /checkout/{orderId}/pay
     */
    @PostMapping("/{orderId}/pay")
    public String processPayment(
            @PathVariable String orderId,
            @ModelAttribute CheckoutRequest request,
            RedirectAttributes redirectAttributes) {

        log.info("POST /checkout/{}/pay - method={}", orderId, request.getMethod());
        request.setOrderId(orderId);

        try {
            PaymentResponse payment = paymentService.processPayment(request);

            if ("captured".equals(payment.getStatus()) || "authorized".equals(payment.getStatus())) {
                // Generate signature for verification
                String signature = SignatureUtil.generatePaymentSignature(
                        orderId, payment.getId(), keySecret);

                redirectAttributes.addFlashAttribute("payment", payment);
                redirectAttributes.addFlashAttribute("signature", signature);
                return "redirect:/checkout/" + orderId + "/success";
            } else {
                redirectAttributes.addFlashAttribute("payment", payment);
                redirectAttributes.addFlashAttribute("error", payment.getErrorDescription());
                return "redirect:/checkout/" + orderId + "/failure";
            }
        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/" + orderId + "/failure";
        }
    }

    /**
     * Displays the success page.
     * GET /checkout/{orderId}/success
     */
    @GetMapping("/{orderId}/success")
    public String showSuccess(@PathVariable String orderId, Model model) {
        log.info("GET /checkout/{}/success", orderId);

        OrderResponse order = orderService.getOrder(orderId);
        model.addAttribute("order", order);
        model.addAttribute("amount", formatAmount(order.getAmount()));

        return "checkout-success";
    }

    /**
     * Displays the failure page.
     * GET /checkout/{orderId}/failure
     */
    @GetMapping("/{orderId}/failure")
    public String showFailure(@PathVariable String orderId, Model model) {
        log.info("GET /checkout/{}/failure", orderId);

        OrderResponse order = orderService.getOrder(orderId);
        model.addAttribute("order", order);
        model.addAttribute("amount", formatAmount(order.getAmount()));

        return "checkout-failure";
    }

    /**
     * Returns checkout page as embeddable iframe.
     * GET /checkout/{orderId}/embed
     */
    @GetMapping("/{orderId}/embed")
    public String showEmbeddedCheckout(@PathVariable String orderId, Model model) {
        log.info("GET /checkout/{}/embed", orderId);

        OrderResponse order = orderService.getOrder(orderId);
        model.addAttribute("order", order);
        model.addAttribute("amount", formatAmount(order.getAmount()));
        model.addAttribute("embedded", true);

        return "checkout";
    }

    // ============== Helper Methods ==============

    private String formatAmount(Long amountInPaise) {
        double rupees = amountInPaise / 100.0;
        return String.format("₹%.2f", rupees);
    }

    private List<BankOption> getBankList() {
        return List.of(
                new BankOption("HDFC", "HDFC Bank"),
                new BankOption("ICIC", "ICICI Bank"),
                new BankOption("SBIN", "State Bank of India"),
                new BankOption("UTIB", "Axis Bank"),
                new BankOption("KKBK", "Kotak Mahindra Bank"),
                new BankOption("YESB", "Yes Bank"),
                new BankOption("PUNB", "Punjab National Bank"),
                new BankOption("BARB", "Bank of Baroda"),
                new BankOption("IDFB", "IDFC First Bank"),
                new BankOption("INDB", "IndusInd Bank")
        );
    }

    private List<WalletOption> getWalletList() {
        return List.of(
                new WalletOption("paytm", "Paytm", "paytm-logo.png"),
                new WalletOption("phonepe", "PhonePe", "phonepe-logo.png"),
                new WalletOption("amazonpay", "Amazon Pay", "amazonpay-logo.png"),
                new WalletOption("mobikwik", "MobiKwik", "mobikwik-logo.png"),
                new WalletOption("freecharge", "Freecharge", "freecharge-logo.png")
        );
    }

    // Inner classes for dropdowns
    public record BankOption(String code, String name) {}
    public record WalletOption(String code, String name, String logo) {}
}
