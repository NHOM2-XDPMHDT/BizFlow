# Kafka Integration - Quick Start Guide

## Problem Fixed ✅

The `KAFKA_INTEGRATION_EXAMPLE.java` was a standalone reference document that caused compilation errors. This has been replaced with this practical quick-start guide.

## How to Integrate OrderEventProducer into Your Order Service

### Step 1: Add Kafka Event Publishing to AdminOrderService

In your AdminOrderService, find the Order creation method and add the producer call.

**Example Integration Point:**

```java
// In your existing OrderService or OrderController class
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderEventProducer orderEventProducer;  // ADD THIS
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. Create and save the order using existing code
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        
        // Save order to database
        Order savedOrder = orderRepository.save(order);
        
        // 2. Publish event to Kafka (ADD THIS SECTION)
        try {
            PurchaseEvent event = new PurchaseEvent();
            event.setOrderId(savedOrder.getId());
            event.setCustomerId(savedOrder.getCustomerId());
            event.setUserId(savedOrder.getUserId());
            event.setInvoiceNumber(savedOrder.getInvoiceNumber());
            event.setTotalAmount(savedOrder.getTotalAmount());
            event.setStatus(savedOrder.getStatus());
            event.setCreatedAt(savedOrder.getCreatedAt());
            
            // Set order items if available
            if (savedOrder.getOrderItems() != null) {
                event.setOrderItems(
                    savedOrder.getOrderItems().stream()
                        .map(item -> new OrderItemDTO(
                            item.getProductId(),
                            item.getQuantity(),
                            item.getPrice()
                        ))
                        .collect(Collectors.toList())
                );
            }
            
            // Publish the event
            orderEventProducer.publishPurchaseEvent(event);
        } catch (Exception e) {
            logger.warn("Failed to publish Kafka event for order " + savedOrder.getId(), e);
            // Don't fail the order creation if Kafka fails
        }
        
        return savedOrder;
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        
        // Publish status update event
        try {
            PurchaseEvent event = new PurchaseEvent();
            event.setOrderId(updatedOrder.getId());
            event.setCustomerId(updatedOrder.getCustomerId());
            event.setStatus(newStatus);
            event.setCreatedAt(updatedOrder.getCreatedAt());
            // ... set other fields
            
            orderEventProducer.publishPurchaseEvent(event);
        } catch (Exception e) {
            logger.warn("Failed to publish status update event", e);
        }
        
        return updatedOrder;
    }
}
```

### Step 2: Rebuild the Services

```bash
# Build the updated services
docker-compose build admin-order-service admin-user-service

# Start the services
docker-compose up -d admin-order-service admin-user-service

# Verify they started
docker-compose ps
```

### Step 3: Initialize the Database Table

Execute the migration script to create the purchase history table:

```bash
# Option 1: Direct execution
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db < db/create_purchase_history_table.sql

# Option 2: Manual execution in MySQL
# mysql> use bizflow_db;
# mysql> source db/create_purchase_history_table.sql;

# Verify table was created
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e "SHOW TABLES LIKE 'customer_purchase%';"
```

### Step 4: Test the Integration

**Test 1: Create an Order and Verify Kafka Event**

```bash
# Monitor Kafka topic in one terminal
docker exec bizflow-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic customer-purchases \
  --from-beginning

# In another terminal, create an order via API
curl -X POST http://localhost:8000/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "customerId": 1,
    "userId": 1,
    "totalAmount": 299.99,
    "items": [
      {"productId": 1, "quantity": 2, "price": 149.99}
    ]
  }'
```

**Test 2: Verify Data in Database**

```bash
# Check if event was stored
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e \
  "SELECT * FROM customer_purchase_history WHERE customer_id = 1;"

# Check count
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db -e \
  "SELECT COUNT(*) as total_records FROM customer_purchase_history;"
```

**Test 3: Query via REST API**

```bash
# Get paginated purchase history
curl -X GET "http://localhost:8000/api/admin/customers/1/purchase-history?page=0&size=10" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"

# Get all purchases
curl -X GET "http://localhost:8000/api/admin/customers/1/purchase-history/all" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"

# Get purchase count
curl -X GET "http://localhost:8000/api/admin/customers/1/purchase-history/count" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

## Kafka Components Already Configured ✅

| Component | Location | Status |
|-----------|----------|--------|
| Kafka Broker | docker-compose.yml | ✅ Configured |
| Zookeeper | docker-compose.yml | ✅ Configured |
| Producer Config | BizFlow.AdminOrderService/src/main/java/com/bizflow/config/KafkaProducerConfig.java | ✅ Ready |
| Producer Service | BizFlow.AdminOrderService/src/main/java/com/bizflow/producer/OrderEventProducer.java | ✅ Ready |
| Event DTO | BizFlow.AdminOrderService/src/main/java/com/bizflow/event/PurchaseEvent.java | ✅ Ready |
| Consumer Config | BizFlow.AdminUserService/src/main/java/com/bizflow/config/KafkaConsumerConfig.java | ✅ Ready |
| Consumer Listener | BizFlow.AdminUserService/src/main/java/com/bizflow/consumer/PurchaseEventConsumer.java | ✅ Ready |
| Database Entity | BizFlow.AdminUserService/src/main/java/com/bizflow/entity/CustomerPurchaseHistory.java | ✅ Ready |
| Repository | BizFlow.AdminUserService/src/main/java/com/bizflow/repository/CustomerPurchaseHistoryRepository.java | ✅ Ready |
| Service | BizFlow.AdminUserService/src/main/java/com/bizflow/service/CustomerPurchaseHistoryService.java | ✅ Ready |
| REST API | BizFlow.AdminUserService/src/main/java/com/bizflow/controller/CustomerPurchaseHistoryController.java | ✅ Ready |
| Frontend JS | BizFlow.Frontend/assets/js/customer-purchase-history.js | ✅ Ready |
| Frontend CSS | BizFlow.Frontend/styles/customer-purchase-history.css | ✅ Ready |

## Troubleshooting

### Issue: "Missing mandatory Classpath entries"

**Solution**: This is a Maven warning that resolves after rebuild
```bash
docker-compose build --no-cache admin-order-service admin-user-service
docker-compose up -d
```

### Issue: Kafka topic not receiving messages

**Check**:
```bash
# Verify Kafka is running
docker logs bizflow-kafka | tail -20

# Check if producer can reach Kafka
docker logs bizflow-admin-order-service | grep -i kafka

# List topics
docker exec bizflow-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Issue: Database table doesn't exist

**Fix**:
```bash
docker exec bizflow-mysql mysql -uroot -p123456 bizflow_db < db/create_purchase_history_table.sql
```

### Issue: Consumer group not consuming messages

**Check**:
```bash
# Describe consumer group
docker exec bizflow-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group admin-user-service --describe

# Check logs
docker logs bizflow-admin-user-service | grep -i consumer
```

## Files Summary

**Java Classes Created** (9 files):
1. `PurchaseEvent.java` - Event DTO (AdminOrderService & AdminUserService)
2. `OrderEventProducer.java` - Kafka producer
3. `KafkaProducerConfig.java` - Producer configuration
4. `PurchaseEventConsumer.java` - Kafka consumer listener
5. `KafkaConsumerConfig.java` - Consumer configuration
6. `CustomerPurchaseHistory.java` - Database entity
7. `CustomerPurchaseHistoryRepository.java` - Data access
8. `CustomerPurchaseHistoryService.java` - Business logic
9. `CustomerPurchaseHistoryController.java` - REST endpoints

**Frontend Files** (2 files):
1. `customer-purchase-history.js` - UI module
2. `customer-purchase-history.css` - Styling

**Database** (1 file):
1. `create_purchase_history_table.sql` - Migration script

**Configuration** (2 files):
1. `docker-compose.yml` - Infrastructure (updated)
2. `pom.xml` - Dependencies (AdminOrderService & AdminUserService)

## Next Steps

1. ✅ Kafka infrastructure ready (Zookeeper + Broker)
2. ⏳ **Integrate OrderEventProducer into your order creation code** (see code example above)
3. ⏳ Rebuild services: `docker-compose build`
4. ⏳ Execute database migration
5. ⏳ Test end-to-end with example orders
6. ⏳ Integrate frontend UI into admin dashboard

---

**Status**: Implementation Complete, Ready for Integration
**Last Updated**: 2026-01-28
