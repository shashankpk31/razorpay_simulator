package com.simulator.razorpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Razorpay Simulator - A drop-in replacement for Razorpay during development.
 *
 * <p>This application provides:</p>
 * <ul>
 *   <li>Same REST API as Razorpay (/v1/orders, /v1/payments, etc.)</li>
 *   <li>Checkout UI that looks like Razorpay</li>
 *   <li>Webhook delivery to your application</li>
 *   <li>Test cards, UPI, netbanking for various scenarios</li>
 *   <li>Admin dashboard to monitor transactions</li>
 * </ul>
 *
 * <p>Usage: Just point your app to http://localhost:9000 instead of api.razorpay.com</p>
 *
 * @author Razorpay Simulator
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RazorpaySimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(RazorpaySimulatorApplication.class, args);

        System.out.println("\n" +
            "╔═══════════════════════════════════════════════════════════════════╗\n" +
            "║                    RAZORPAY SIMULATOR                             ║\n" +
            "╠═══════════════════════════════════════════════════════════════════╣\n" +
            "║  API Endpoint  : http://localhost:9000/v1                         ║\n" +
            "║  Checkout UI   : http://localhost:9000/checkout/{order_id}        ║\n" +
            "║  Admin Dashboard: http://localhost:9000/admin                     ║\n" +
            "╠═══════════════════════════════════════════════════════════════════╣\n" +
            "║  Test Key ID   : rzp_test_simulator                               ║\n" +
            "║  Test Secret   : sim_secret_key_12345                             ║\n" +
            "╠═══════════════════════════════════════════════════════════════════╣\n" +
            "║  For production, change base URL to: https://api.razorpay.com    ║\n" +
            "╚═══════════════════════════════════════════════════════════════════╝\n"
        );
    }
}
