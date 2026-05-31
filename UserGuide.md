# Razorpay Simulator - User Guide

A complete guide to using the Razorpay Simulator for development and testing.

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [API Reference](#2-api-reference)
3. [Checkout Integration](#3-checkout-integration)
4. [Test Cards & UPI](#4-test-cards--upi)
5. [Webhook Configuration](#5-webhook-configuration)
6. [Admin Dashboard](#6-admin-dashboard)
7. [Integration Examples](#7-integration-examples)
8. [Switching to Production](#8-switching-to-production)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Getting Started

### 1.1 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Your application (e.g., Spring Boot, Node.js, React)

### 1.2 Installation

```bash
# Clone or copy the project
cd D:/razorpay-simulator

# Build the project
mvn clean package -DskipTests

# Run the simulator
mvn spring-boot:run
```

### 1.3 Verify Installation

Open your browser and visit:
- **Admin Dashboard:** http://localhost:9000/admin
- **API Docs:** http://localhost:9000/admin/docs

You should see the Razorpay Simulator dashboard.

### 1.4 Default Credentials

| Credential | Value |
|------------|-------|
| API Key ID | `rzp_test_simulator` |
| API Key Secret | `sim_secret_key_12345` |
| Webhook Secret | `whsec_simulator_webhook_secret` |

---

## 2. API Reference

The simulator implements the same REST API as Razorpay. All endpoints are prefixed with `/v1`.

### 2.1 Authentication

Use HTTP Basic Authentication with your API Key ID and Secret:

```bash
curl -u rzp_test_simulator:sim_secret_key_12345 \
  http://localhost:9000/v1/orders
```

### 2.2 Orders API

#### Create Order

```bash
POST /v1/orders
Content-Type: application/json

{
  "amount": 10000,        # Amount in paise (₹100 = 10000)
  "currency": "INR",      # Currency code
  "receipt": "order_123", # Your internal order ID
  "notes": {              # Optional metadata
    "customer_id": "cust_001"
  }
}
```

**Response:**
```json
{
  "id": "order_ABC123xyz",
  "entity": "order",
  "amount": 10000,
  "amount_paid": 0,
  "amount_due": 10000,
  "currency": "INR",
  "receipt": "order_123",
  "status": "created",
  "attempts": 0,
  "created_at": 1234567890
}
```

#### Fetch Order

```bash
GET /v1/orders/{order_id}
```

#### List Orders

```bash
GET /v1/orders?skip=0&count=10
```

### 2.3 Payments API

#### Fetch Payment

```bash
GET /v1/payments/{payment_id}
```

**Response:**
```json
{
  "id": "pay_XYZ789abc",
  "entity": "payment",
  "amount": 10000,
  "currency": "INR",
  "status": "captured",
  "order_id": "order_ABC123xyz",
  "method": "card",
  "card": {
    "last4": "1111",
    "network": "Visa",
    "type": "credit"
  },
  "email": "customer@example.com",
  "contact": "+919876543210",
  "fee": 200,
  "tax": 36,
  "created_at": 1234567890,
  "captured_at": 1234567891
}
```

#### Capture Payment

Capture an authorized payment (required if auto-capture is disabled):

```bash
POST /v1/payments/{payment_id}/capture
Content-Type: application/json

{
  "amount": 10000  # Amount to capture (can be less than authorized)
}
```

#### List Payments

```bash
GET /v1/payments?skip=0&count=10
```

### 2.4 Refunds API

#### Create Refund

```bash
POST /v1/payments/{payment_id}/refund
Content-Type: application/json

{
  "amount": 5000,    # Partial refund (optional, defaults to full)
  "speed": "normal"  # "normal" or "optimum"
}
```

#### Fetch Refund

```bash
GET /v1/refunds/{refund_id}
```

#### List Refunds

```bash
GET /v1/refunds?skip=0&count=10
```

---

## 3. Checkout Integration

### 3.1 Hosted Checkout (Recommended)

The simplest way to accept payments is to redirect users to the hosted checkout page.

#### Step 1: Create an Order (Backend)

```java
// Spring Boot Example
@PostMapping("/create-order")
public Map<String, Object> createOrder(@RequestBody OrderRequest request) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("rzp_test_simulator", "sim_secret_key_12345");
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = Map.of(
        "amount", request.getAmount() * 100,  // Convert to paise
        "currency", "INR",
        "receipt", "order_" + System.currentTimeMillis()
    );

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "http://localhost:9000/v1/orders",
        entity,
        Map.class
    );

    return response.getBody();
}
```

#### Step 2: Redirect to Checkout (Frontend)

```javascript
// After creating order
const order = await createOrder({ amount: 100 }); // ₹100

// Redirect to checkout
window.location.href = `http://localhost:9000/checkout/${order.id}`;
```

#### Step 3: Handle Callback

After payment, users are redirected to:
- **Success:** `/checkout/{order_id}/success`
- **Failure:** `/checkout/{order_id}/failure`

You can configure a custom callback URL when creating the order:

```json
{
  "amount": 10000,
  "currency": "INR",
  "callback_url": "http://localhost:8089/payment/callback"
}
```

### 3.2 Embedded Checkout

For iframe embedding:

```html
<iframe
  src="http://localhost:9000/checkout/{order_id}/embed"
  width="400"
  height="600"
  frameborder="0">
</iframe>
```

### 3.3 Verify Payment Signature

After successful payment, verify the signature server-side:

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public boolean verifySignature(String orderId, String paymentId, String signature) {
    String secret = "sim_secret_key_12345";
    String payload = orderId + "|" + paymentId;

    try {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = Hex.encodeHexString(hash);

        return expectedSignature.equals(signature);
    } catch (Exception e) {
        return false;
    }
}
```

---

## 4. Test Cards & UPI

### 4.1 Success Cards

| Card Number | Network | Type | Description |
|-------------|---------|------|-------------|
| 4111 1111 1111 1111 | Visa | Credit | Always succeeds |
| 5267 3181 8797 5449 | Mastercard | Debit | Always succeeds |
| 4012 8888 8888 1881 | Visa | Debit | HDFC Bank |
| 5105 1051 0510 5100 | Mastercard | Credit | ICICI Bank |
| 6011 1111 1111 1117 | RuPay | Debit | SBI |
| 3782 8224 6310 005 | Amex | Credit | American Express |

### 4.2 Failure Cards

| Card Number | Error | Description |
|-------------|-------|-------------|
| 4000 0000 0000 0002 | CARD_DECLINED | Bank rejected the transaction |
| 4000 0000 0000 9995 | INSUFFICIENT_FUNDS | Not enough balance |
| 4000 0000 0000 0069 | EXPIRED_CARD | Card has expired |
| 4000 0000 0000 0127 | INVALID_CVV | Wrong CVV entered |
| 4000 0000 0000 0044 | TIMEOUT | Gateway timeout (30s delay) |

### 4.3 Special Cards

| Card Number | Behavior |
|-------------|----------|
| 4000 0000 0000 3220 | Requires OTP (3DS simulation) |

### 4.4 Card Details for Testing

- **CVV:** Any 3 digits (e.g., `123`, `456`)
- **Expiry:** Any future date (e.g., `12/30`, `06/28`)
- **Name:** Any name

### 4.5 UPI Test IDs

| UPI ID | Result |
|--------|--------|
| `success@upi` | Payment succeeds |
| `fail@upi` | Payment fails |
| `timeout@upi` | Payment times out |
| `(any other)` | Payment succeeds |

### 4.6 Netbanking

All bank selections succeed by default. Available banks:
- HDFC Bank (HDFC)
- ICICI Bank (ICIC)
- State Bank of India (SBIN)
- Axis Bank (UTIB)
- Kotak Mahindra Bank (KKBK)
- Yes Bank (YESB)
- Punjab National Bank (PUNB)
- Bank of Baroda (BARB)
- IDFC First Bank (IDFB)
- IndusInd Bank (INDB)

### 4.7 Wallets

All wallet selections succeed by default:
- Paytm
- PhonePe
- Amazon Pay
- MobiKwik
- Freecharge

---

## 5. Webhook Configuration

### 5.1 Add Webhook URL

**Via API:**
```bash
curl -X POST http://localhost:9000/v1/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "http://localhost:8089/webhooks/razorpay",
    "secret": "my_webhook_secret",
    "events": "*"
  }'
```

**Via Admin Dashboard:**
1. Go to http://localhost:9000/admin/webhooks
2. Enter your webhook URL
3. Optionally set a secret for signature verification
4. Click "Add Webhook"

### 5.2 Supported Events

| Event | Triggered When |
|-------|----------------|
| `payment.authorized` | Payment is authorized (card blocked) |
| `payment.captured` | Payment is captured (money received) |
| `payment.failed` | Payment attempt failed |
| `order.paid` | Order is fully paid |
| `refund.created` | Refund is initiated |
| `refund.processed` | Refund is completed |

### 5.3 Webhook Payload Structure

```json
{
  "entity": "event",
  "account_id": "acc_simulator",
  "event": "payment.captured",
  "contains": ["payment"],
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_XYZ789abc",
        "amount": 10000,
        "status": "captured",
        "order_id": "order_ABC123xyz",
        "method": "card",
        ...
      }
    }
  },
  "created_at": 1234567890
}
```

### 5.4 Verify Webhook Signature

Webhooks include an `X-Razorpay-Signature` header. Verify it:

```java
public boolean verifyWebhookSignature(String payload, String signature, String secret) {
    try {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = Hex.encodeHexString(hash);

        return expectedSignature.equals(signature);
    } catch (Exception e) {
        return false;
    }
}
```

### 5.5 Webhook Retry Logic

- **Max Attempts:** 3
- **Retry Delay:** 5 seconds between attempts
- **Timeout:** 10 seconds per delivery attempt

Failed webhooks can be manually retried from the admin dashboard.

---

## 6. Admin Dashboard

Access the admin dashboard at: **http://localhost:9000/admin**

### 6.1 Dashboard Overview

- Total orders, payments, refunds
- Revenue statistics
- Recent transactions
- Quick action links

### 6.2 Orders Page (`/admin/orders`)

- View all orders with pagination
- See order status (created, attempted, paid)
- Click "Checkout" to process payment for unpaid orders

### 6.3 Payments Page (`/admin/payments`)

- View all payment attempts
- See payment method, status, and details
- Track authorization and capture timestamps

### 6.4 Refunds Page (`/admin/refunds`)

- View all refunds
- Track refund status (pending, processed, failed)
- See processing timestamps

### 6.5 Webhooks Page (`/admin/webhooks`)

- Add/remove webhook configurations
- View delivery history
- Retry failed webhooks manually

### 6.6 Test Cards Page (`/admin/test-cards`)

- View all available test cards
- Copy card numbers with one click
- See expected behavior for each card

### 6.7 API Docs Page (`/admin/docs`)

- Quick API reference
- Code examples
- Webhook event documentation

### 6.8 H2 Database Console

Access at: **http://localhost:9000/h2-console**

- **JDBC URL:** `jdbc:h2:file:./data/razorpay-sim`
- **Username:** `sa`
- **Password:** (leave empty)

---

## 7. Integration Examples

### 7.1 Spring Boot Integration

**application.yml:**
```yaml
razorpay:
  base-url: http://localhost:9000
  key-id: rzp_test_simulator
  key-secret: sim_secret_key_12345
  webhook-secret: whsec_simulator_webhook_secret
```

**RazorpayService.java:**
```java
@Service
public class RazorpayService {

    @Value("${razorpay.base-url}")
    private String baseUrl;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private final RestTemplate restTemplate;

    public RazorpayService(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .basicAuthentication(keyId, keySecret)
            .build();
    }

    public OrderResponse createOrder(long amountInPaise, String receipt) {
        Map<String, Object> request = Map.of(
            "amount", amountInPaise,
            "currency", "INR",
            "receipt", receipt
        );

        return restTemplate.postForObject(
            baseUrl + "/v1/orders",
            request,
            OrderResponse.class
        );
    }

    public PaymentResponse capturePayment(String paymentId, long amount) {
        Map<String, Object> request = Map.of("amount", amount);

        return restTemplate.postForObject(
            baseUrl + "/v1/payments/" + paymentId + "/capture",
            request,
            PaymentResponse.class
        );
    }
}
```

**WebhookController.java:**
```java
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        if (!verifySignature(payload, signature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // Parse and process webhook
        JsonNode event = objectMapper.readTree(payload);
        String eventType = event.get("event").asText();

        switch (eventType) {
            case "payment.captured":
                handlePaymentCaptured(event);
                break;
            case "refund.processed":
                handleRefundProcessed(event);
                break;
        }

        return ResponseEntity.ok("OK");
    }
}
```

### 7.2 Node.js/Express Integration

```javascript
const axios = require('axios');
const crypto = require('crypto');

const razorpay = axios.create({
  baseURL: 'http://localhost:9000',
  auth: {
    username: 'rzp_test_simulator',
    password: 'sim_secret_key_12345'
  }
});

// Create order
async function createOrder(amount, receipt) {
  const response = await razorpay.post('/v1/orders', {
    amount: amount * 100, // Convert to paise
    currency: 'INR',
    receipt
  });
  return response.data;
}

// Verify webhook
function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');
  return expectedSignature === signature;
}

// Webhook handler
app.post('/webhooks/razorpay', express.raw({ type: 'application/json' }), (req, res) => {
  const signature = req.headers['x-razorpay-signature'];

  if (!verifyWebhook(req.body.toString(), signature, 'whsec_simulator_webhook_secret')) {
    return res.status(401).send('Invalid signature');
  }

  const event = JSON.parse(req.body);
  console.log('Received event:', event.event);

  res.send('OK');
});
```

### 7.3 React Frontend Integration

```jsx
import { useState } from 'react';

function PaymentButton({ amount, onSuccess }) {
  const [loading, setLoading] = useState(false);

  const handlePayment = async () => {
    setLoading(true);

    try {
      // Create order via your backend
      const response = await fetch('/api/create-order', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount })
      });
      const order = await response.json();

      // Redirect to Razorpay Simulator checkout
      window.location.href = `http://localhost:9000/checkout/${order.id}`;

    } catch (error) {
      console.error('Payment error:', error);
      setLoading(false);
    }
  };

  return (
    <button onClick={handlePayment} disabled={loading}>
      {loading ? 'Processing...' : `Pay ₹${amount}`}
    </button>
  );
}
```

---

## 8. Switching to Production

### 8.1 Configuration Changes

Update your application configuration:

```yaml
# DEVELOPMENT
razorpay:
  base-url: http://localhost:9000
  key-id: rzp_test_simulator
  key-secret: sim_secret_key_12345

# PRODUCTION
razorpay:
  base-url: https://api.razorpay.com
  key-id: ${RAZORPAY_KEY_ID}      # From Razorpay Dashboard
  key-secret: ${RAZORPAY_KEY_SECRET}
```

### 8.2 Checklist

- [ ] Update base URL to `https://api.razorpay.com`
- [ ] Replace test API keys with production keys
- [ ] Update webhook secret from Razorpay Dashboard
- [ ] Configure webhook URL in Razorpay Dashboard
- [ ] Test with Razorpay's test mode first
- [ ] Switch to live mode when ready

### 8.3 API Compatibility

The simulator implements 100% compatible API with Razorpay. No code changes needed beyond configuration.

| Feature | Simulator | Razorpay |
|---------|-----------|----------|
| Orders API | ✅ | ✅ |
| Payments API | ✅ | ✅ |
| Refunds API | ✅ | ✅ |
| Webhooks | ✅ | ✅ |
| Signature Verification | ✅ | ✅ |

---

## 9. Troubleshooting

### 9.1 Common Issues

#### Simulator not starting

```
Error: Port 9000 already in use
```

**Solution:** Change port in `application.yml`:
```yaml
server:
  port: 9001
```

#### Webhooks not being delivered

1. Check webhook URL is reachable from simulator
2. Verify webhook is configured: http://localhost:9000/admin/webhooks
3. Check for errors in webhook event list
4. Manually retry failed webhooks

#### Payment always failing

1. Check you're using a valid test card
2. Verify card number format (no spaces)
3. Check CVV is 3 digits
4. Ensure expiry is future date

#### Database reset

Delete the data folder to reset:
```bash
rm -rf data/
mvn spring-boot:run
```

### 9.2 Logging

Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.simulator.razorpay: DEBUG
```

### 9.3 H2 Console Access

If H2 console shows "Database not found":
1. Ensure application is running
2. Use JDBC URL: `jdbc:h2:file:./data/razorpay-sim`
3. Username: `sa`, Password: (empty)

### 9.4 Getting Help

- Check API docs: http://localhost:9000/admin/docs
- View test cards: http://localhost:9000/admin/test-cards
- Inspect database: http://localhost:9000/h2-console

---

## Appendix

### A. Payment Flow Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Your App   │     │  Simulator  │     │  Your Backend       │
└─────┬───────┘     └──────┬──────┘     └──────────┬──────────┘
      │                    │                       │
      │ 1. Create Order    │                       │
      │───────────────────>│                       │
      │                    │                       │
      │ 2. Order Response  │                       │
      │<───────────────────│                       │
      │                    │                       │
      │ 3. Redirect to Checkout                    │
      │───────────────────>│                       │
      │                    │                       │
      │ 4. User enters payment details             │
      │                    │                       │
      │ 5. Payment processed                       │
      │                    │                       │
      │ 6. Redirect to success/failure             │
      │<───────────────────│                       │
      │                    │                       │
      │                    │ 7. Webhook            │
      │                    │──────────────────────>│
      │                    │                       │
      │                    │ 8. Webhook ACK        │
      │                    │<──────────────────────│
      │                    │                       │
```

### B. Webhook Event Payloads

#### payment.authorized
```json
{
  "event": "payment.authorized",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_xxx",
        "status": "authorized",
        "amount": 10000
      }
    }
  }
}
```

#### payment.captured
```json
{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_xxx",
        "status": "captured",
        "amount": 10000,
        "fee": 200,
        "tax": 36
      }
    }
  }
}
```

#### refund.processed
```json
{
  "event": "refund.processed",
  "payload": {
    "refund": {
      "entity": {
        "id": "rfnd_xxx",
        "payment_id": "pay_xxx",
        "amount": 5000,
        "status": "processed"
      }
    },
    "payment": {
      "entity": {
        "id": "pay_xxx",
        "amount_refunded": 5000,
        "refund_status": "partial"
      }
    }
  }
}
```

---

**Happy Testing!** 🚀

For issues or feature requests, please create a GitHub issue.
