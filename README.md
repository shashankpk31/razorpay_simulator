# 💳 Razorpay Simulator

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green) ![H2](https://img.shields.io/badge/H2-in--memory-blue)

> Local Razorpay payment gateway simulator for testing payment flows without real Razorpay account

---

## 📋 What is This?

**Razorpay Simulator** is a standalone Spring Boot application that mimics Razorpay's payment gateway APIs. It allows developers to:

- ✅ Test payment flows locally without real Razorpay credentials
- ✅ Simulate success/failure payment scenarios
- ✅ Test webhooks and callbacks
- ✅ Verify payment signatures
- ✅ View transaction history in admin dashboard

**Use Cases:**
- Local development and testing
- CI/CD integration tests
- Demo and presentation purposes
- Recruiter portfolio testing

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.9+**

### Run Locally

```bash
# Clone repository
git clone https://github.com/yourusername/razorpay-simulator.git
cd razorpay-simulator

# Build
mvn clean package -DskipTests

# Run
java -jar target/razorpay-simulator-1.0.0.jar
```

**Access Points:**
- **Admin Dashboard**: http://localhost:9000/admin
- **Checkout Page**: http://localhost:9000/checkout/{orderId}
- **API Base URL**: http://localhost:9000

---

## 🏗️ Architecture

### Components

```
razorpay-simulator/
├── controller/
│   ├── OrderApiController      # POST /v1/orders
│   ├── PaymentApiController    # POST /v1/payments/{id}/capture
│   ├── CheckoutController      # GET /checkout/{orderId}
│   └── AdminController         # GET /admin
├── service/
│   ├── OrderService           # Order management
│   ├── PaymentService         # Payment processing
│   └── WebhookService         # Webhook delivery
├── entity/
│   ├── SimOrder               # Order entity
│   ├── SimPayment             # Payment entity
│   └── TestCard               # Test card configurations
└── repository/                # H2 in-memory database
```

### Database

**H2 In-Memory Database**
- Resets on restart (perfect for testing)
- Console: http://localhost:9000/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/razorpay-sim`
- **Username**: `sa`
- **Password**: *(empty)*

---

## 📚 API Endpoints

### Order APIs

#### Create Order

```bash
POST /v1/orders
Content-Type: application/json
Authorization: Basic <base64(keyId:keySecret)>

{
  "amount": 10000,  # Amount in paise (₹100)
  "currency": "INR",
  "receipt": "order_rcptid_11"
}
```

**Response:**
```json
{
  "id": "order_xxx",
  "amount": 10000,
  "currency": "INR",
  "receipt": "order_rcptid_11",
  "status": "created"
}
```

#### Get Order

```bash
GET /v1/orders/{orderId}
Authorization: Basic <base64(keyId:keySecret)>
```

---

### Payment APIs

#### Capture Payment

```bash
POST /v1/payments/{paymentId}/capture
Content-Type: application/json
Authorization: Basic <base64(keyId:keySecret)>

{
  "amount": 10000
}
```

---

### Checkout UI

#### Open Checkout

```bash
GET /checkout/{orderId}
```

Opens a Razorpay-like checkout page where users can:
- Select payment method (Card, UPI, Net Banking, Wallet)
- Enter payment details
- Complete payment

---

## 🧪 Test Cards

The simulator includes pre-configured test cards:

| Card Number | Network | Result | Use Case |
|-------------|---------|--------|----------|
| 4111 1111 1111 1111 | Visa | ✅ Success | Happy path testing |
| 4000 0000 0000 0002 | Visa | ❌ Card Declined | Failure testing |
| 4000 0000 0000 0069 | Visa | ❌ Expired Card | Expiry testing |
| 4000 0000 0000 0127 | Visa | ❌ Invalid CVV | CVV testing |
| 5555 5555 5555 4444 | Mastercard | ✅ Success | MC testing |

**Any CVV**: 123
**Any Expiry**: 12/25

---

## 🎛️ Admin Dashboard

**URL**: http://localhost:9000/admin

### Features

- 📊 **Statistics**: Orders, payments, revenue
- 📋 **Orders**: View all orders and status
- 💳 **Payments**: Transaction history
- 🔄 **Webhooks**: Configure and test webhooks
- 💎 **Test Cards**: Manage test card scenarios

### Screenshots

*(Add screenshots here if available)*

---

## 🔧 Configuration

### application.yml

```yaml
server:
  port: 9000

razorpay:
  simulator:
    key-id: rzp_test_simulator
    key-secret: sim_secret_key_12345
    auto-capture: true  # Auto-capture authorized payments
    delays:
      payment-processing-ms: 1000

spring:
  h2:
    console:
      enabled: true
  datasource:
    url: jdbc:h2:file:./data/razorpay-sim
```

---

## 🌐 Deployment to Render

### Deploy Separately (Recommended)

1. **Create separate GitHub repo** for simulator
2. **Push code**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/razorpay-simulator.git
   git push -u origin main
   ```
3. **Deploy to Render**
   - Go to [Render Dashboard](https://dashboard.render.com/)
   - New → Blueprint
   - Connect GitHub repo
   - Render auto-detects `render.yaml`

**Access**: `https://razorpay-simulator.onrender.com/admin`

---

## 🔐 Security Notes

⚠️ **NOT FOR PRODUCTION USE**

This simulator is for **testing purposes only**:
- ❌ No real payment processing
- ❌ No PCI-DSS compliance
- ❌ No encryption
- ❌ No rate limiting

For production, use **real Razorpay** with proper credentials.

---

## 🐛 Troubleshooting

### Order already paid error

**Issue**: `RuntimeException: Order already paid`
**Solution**: Each order can only be paid once. Create a new order for testing.

### Payment stuck in AUTHORIZED

**Issue**: Payment shows AUTHORIZED but not CAPTURED
**Solution**: Set `auto-capture: true` in `application.yml`

### Webhook not delivered

**Issue**: Webhook events not triggering
**Solution**: Configure webhook URL in admin dashboard → Webhooks

---

## 📝 License

MIT License - Free to use for testing and development

---

## 🙏 Credits

- Inspired by Razorpay's actual payment flow
- Built for BiteDash project testing
- Not affiliated with Razorpay

---

**Happy Testing! 🎉**
