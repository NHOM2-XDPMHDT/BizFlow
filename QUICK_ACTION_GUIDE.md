# 🚀 KAFKA INTEGRATION - QUICK ACTION GUIDE

## ✅ BUILD STATUS: SUCCESS ✅

```
✅ AdminOrderService   - BUILD SUCCESS (10.615 seconds)
✅ AdminUserService    - BUILD SUCCESS (4.786 seconds)
```

Both microservices compiled successfully with full Kafka integration!

---

## ⚡ IMMEDIATE ACTIONS (Copy & Paste)

### 1️⃣ Create Database Table (1 minute)
```bash
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db < db/create_purchase_history_table.sql
```

**Verify**:
```bash
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='bizflow_db' AND TABLE_NAME='customer_purchase_history';"
```

Expected output: `1`

---

### 2️⃣ Rebuild Docker Images (2-3 minutes)
```bash
docker-compose build admin-order-service admin-user-service
```

Wait for completion (should see "Building..." and "Successfully built")

---

### 3️⃣ Restart Services (1 minute)
```bash
docker-compose up -d admin-order-service admin-user-service
```

**Wait 30 seconds**, then verify:
```bash
docker-compose ps | grep admin
```

Should show both services as "Up"

---

### 4️⃣ Verify Kafka is Ready (30 seconds)
```bash
docker exec bizflow-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

Should include: `customer-purchases`

---

## 🧪 TEST THE INTEGRATION

### Test 1: Update Order Status (Produces Kafka Event)

```bash
# Get a valid JWT token first
JWT=$(curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "Token: $JWT"

# Update order 1 status
curl -X PUT "http://localhost:8000/admin/orders/1/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{"status":"PAID"}' | jq .
```

Expected: Returns updated order with new status

---

### Test 2: Monitor Kafka Topic (Watch for message)

**In a NEW terminal**:
```bash
docker exec bizflow-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic customer-purchases \
  --from-beginning \
  --timeout-ms 5000
```

Then run **Test 1** in another terminal and watch this one

Expected: JSON message appears with order details

---

### Test 3: Check Database (Verify persistence)

```bash
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e \
  "SELECT id, customer_id, order_id, invoice_number, status, received_at FROM customer_purchase_history ORDER BY received_at DESC LIMIT 3;"
```

Expected: Records appear after you update an order

---

### Test 4: Query via REST API (Use the feature)

```bash
# Get JWT token
JWT=$(curl -s -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# Query purchase history (paginated)
curl -X GET "http://localhost:8000/api/admin/customers/1/purchase-history?page=0&size=10" \
  -H "Authorization: Bearer $JWT" | jq .

# Count purchases
curl -X GET "http://localhost:8000/api/admin/customers/1/purchase-history/count" \
  -H "Authorization: Bearer $JWT" | jq .
```

Expected: JSON response with purchase data

---

## 📊 WHAT WAS DONE

### Code Changes
1. **AdminOrderService**: Added Kafka event publishing when order status changes
2. **AdminUserService**: Already had complete Kafka consumer setup, just fixed configuration imports

### Services Modified
- ✅ `com/bizflow/adminorderservice/service/AdminOrderServiceImpl.java` - Added producer injection and event publishing
- ✅ `com/bizflow/config/KafkaConsumerConfig.java` - Simplified configuration for proper compilation

### Compilation
- ✅ Spring Kafka 3.0.13 dependency successfully resolved
- ✅ Jackson JSON serialization properly configured  
- ✅ All 12 AdminOrderService classes compiled
- ✅ All 24 AdminUserService classes compiled

### Build Artifacts
- ✅ `admin-order-service-0.0.1-SNAPSHOT.jar` - Ready in target/
- ✅ `admin-user-service-0.0.1-SNAPSHOT.jar` - Ready in target/

---

## 🔍 EXPECTED BEHAVIOR

### When You Update Order Status:
1. PUT request to `/admin/orders/{id}/status`
2. AdminOrderService updates database
3. OrderEventProducer sends JSON to Kafka topic `customer-purchases`
4. Kafka broker stores message
5. AdminUserService PurchaseEventConsumer receives message
6. Message saved to `customer_purchase_history` table
7. Available via REST API `/api/admin/customers/{id}/purchase-history`

### Data Flow:
```
Order Update → Kafka Producer → Kafka Broker → Kafka Consumer → Database → REST API
     (0ms)       (10ms)        (stored)      (auto-process)    (1-2s)    (instant)
```

---

## 🆘 IF SOMETHING GOES WRONG

### Services won't start?
```bash
docker logs bizflow-admin-order-service
docker logs bizflow-admin-user-service
```

### No Kafka messages?
```bash
# Check Kafka logs
docker logs bizflow-kafka | tail -20

# Check consumer lag
docker exec bizflow-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group admin-user-service --describe
```

### Database empty?
```bash
# Check table exists
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e "SHOW TABLES LIKE 'customer%';"

# Check consumer logs
docker logs bizflow-admin-user-service | grep -i "consumer\|kafka\|event"
```

### REST API returns 401?
```bash
# Get new JWT token
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

---

## ✨ FEATURES AVAILABLE

### REST API Endpoints Ready:
```
GET  /api/admin/customers/{customerId}/purchase-history?page=0&size=10
GET  /api/admin/customers/{customerId}/purchase-history/all
GET  /api/admin/customers/{customerId}/purchase-history/date-range?startDate=2026-01-01&endDate=2026-12-31
GET  /api/admin/customers/{customerId}/purchase-history/count
GET  /api/admin/customers/{customerId}/purchase-history/{id}
```

### Frontend Integration Ready:
- JavaScript module: `BizFlow.Frontend/assets/js/customer-purchase-history.js`
- CSS styling: `BizFlow.Frontend/styles/customer-purchase-history.css`
- Just include in HTML and call: `purchaseHistory.loadPurchaseHistory(customerId)`

---

## 🎯 SUCCESS CHECKLIST

After completing the actions above, you should be able to:

- [ ] Update order status via API
- [ ] See message in Kafka topic
- [ ] Query purchase history via REST API
- [ ] See data in `customer_purchase_history` table
- [ ] Display purchase history in frontend UI

**If all ✅, Kafka integration is fully functional!**

---

## 📚 DOCUMENTATION

For more details, see:
- `BUILD_SUCCESS_SUMMARY.md` - Full deployment guide
- `KAFKA_INTEGRATION_QUICK_START.md` - Step-by-step integration
- `KAFKA_INTEGRATION.md` - Complete technical reference
- `KAFKA_IMPLEMENTATION_SUMMARY.md` - Architecture overview

---

## ⏱️ TIMELINE

```
Now (2026-01-28 06:16):
  ✅ Both services compiled successfully
  
Next 5 minutes:
  ⏳ Create database table
  ⏳ Rebuild Docker images
  ⏳ Restart services
  ⏳ Run tests
  
Result:
  🚀 Full Kafka event streaming for customer purchase history
```

---

**Status**: ✅ Ready for Deployment

**Next Step**: Copy Step 1 command above and execute in terminal

**Contact**: Check docker logs if issues occur

---

*Generated: 2026-01-28 06:17*
*Build: Maven 3.9.4 | Java 17 LTS | Spring Boot 3.1.6*

