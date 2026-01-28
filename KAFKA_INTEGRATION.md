# Kafka Integration for Customer Purchase History

## Overview

This document describes the Kafka integration implemented in BizFlow to track and view customer purchase history in the AdminUserService. The system uses Kafka event streaming to propagate purchase events from AdminOrderService to AdminUserService for real-time purchase history tracking.

## Architecture

```
AdminOrderService (Producer)
    ↓ (Publishes PurchaseEvent)
    ↓
Kafka Topic: customer-purchases
    ↓ (Subscribes)
    ↓
AdminUserService (Consumer)
    ↓ (Stores in database)
    ↓
MySQL: customer_purchase_history table
    ↓ (REST API queries)
    ↓
Frontend (admin-orders.html)
```

## Components

### 1. **Kafka Infrastructure**
- **Zookeeper**: Coordination service for Kafka cluster
- **Kafka Broker**: Message broker for event streaming
- **Topic**: `customer-purchases` (auto-created with retention policy)

### 2. **AdminOrderService (Producer)**

#### Files Added:
- `src/main/java/com/bizflow/event/PurchaseEvent.java` - Event DTO
- `src/main/java/com/bizflow/producer/OrderEventProducer.java` - Kafka producer
- `src/main/java/com/bizflow/config/KafkaProducerConfig.java` - Producer configuration

#### Key Classes:

**PurchaseEvent.java**
```java
- orderId: Long
- customerId: Long (routing key for partitioning)
- userId: Long
- invoiceNumber: String
- totalAmount: BigDecimal
- status: String (PAID, UNPAID, RETURNED, etc.)
- createdAt: LocalDateTime
- orderItems: List<OrderItemDTO>
- paymentMethod: String
- paymentId: Long
```

**OrderEventProducer.java**
```java
publishPurchaseEvent(PurchaseEvent event)
- Publishes events to kafka topic
- Uses customerId as message key for partition routing
- Includes error handling and logging
```

#### Usage in AdminOrderService:
```java
// Inject into your order creation/update service
@Autowired
private OrderEventProducer orderEventProducer;

// When order is created or updated, publish event
PurchaseEvent event = new PurchaseEvent(
    orderId, customerId, userId, invoiceNumber,
    totalAmount, status, createdAt, orderItems, 
    paymentMethod, paymentId
);
orderEventProducer.publishPurchaseEvent(event);
```

### 3. **AdminUserService (Consumer)**

#### Files Added:
- `src/main/java/com/bizflow/event/PurchaseEvent.java` - Event DTO (same as producer)
- `src/main/java/com/bizflow/entity/CustomerPurchaseHistory.java` - JPA Entity
- `src/main/java/com/bizflow/repository/CustomerPurchaseHistoryRepository.java` - Data access
- `src/main/java/com/bizflow/consumer/PurchaseEventConsumer.java` - Kafka consumer
- `src/main/java/com/bizflow/config/KafkaConsumerConfig.java` - Consumer configuration
- `src/main/java/com/bizflow/service/CustomerPurchaseHistoryService.java` - Business logic
- `src/main/java/com/bizflow/controller/CustomerPurchaseHistoryController.java` - REST endpoints

#### Key Classes:

**CustomerPurchaseHistory.java (Entity)**
```sql
CREATE TABLE `customer_purchase_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL UNIQUE,
  `invoice_number` VARCHAR(30),
  `total_amount` DECIMAL(15,2),
  `status` VARCHAR(30),
  `order_created_at` DATETIME,
  `payment_method` VARCHAR(50),
  `order_items_json` LONGTEXT,
  `received_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_customer_id` (`customer_id`),
  KEY `IDX_customer_created` (`customer_id`, `order_created_at` DESC)
)
```

**PurchaseEventConsumer.java**
```java
@KafkaListener(topics = "customer-purchases", groupId = "admin-user-service")
public void consumePurchaseEvent(PurchaseEvent event)
- Listens on customer-purchases topic
- Stores events in customer_purchase_history table
- Handles duplicate orders (updates if order_id exists)
- Serializes order items as JSON for flexibility
```

### 4. **REST API Endpoints**

Base URL: `/api/admin/customers`

#### Endpoints:

**1. Get Paginated Purchase History**
```
GET /api/admin/customers/{customerId}/purchase-history?page=0&size=10
```
Response:
```json
{
  "success": true,
  "data": [...],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 10
}
```

**2. Get All Purchases (No Pagination)**
```
GET /api/admin/customers/{customerId}/purchase-history/all
```
Response:
```json
{
  "success": true,
  "data": [...],
  "totalCount": 25
}
```

**3. Get Purchases by Date Range**
```
GET /api/admin/customers/{customerId}/purchase-history/date-range?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59
```
Response:
```json
{
  "success": true,
  "data": [...],
  "totalCount": 5,
  "dateRange": {
    "start": "2026-01-01T00:00:00",
    "end": "2026-01-31T23:59:59"
  }
}
```

**4. Get Purchase Count**
```
GET /api/admin/customers/{customerId}/purchase-history/count
```
Response:
```json
{
  "success": true,
  "customerId": 1,
  "purchaseCount": 25
}
```

**5. Get Purchase Details**
```
GET /api/admin/customers/{customerId}/purchase-history/{id}
```
Response:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "customerId": 1,
    "orderId": 123,
    "invoiceNumber": "INV-001",
    "totalAmount": 1500000,
    "status": "PAID",
    "orderCreatedAt": "2026-01-15T10:30:00",
    "paymentMethod": "TRANSFER",
    "orderItemsJson": "[...]",
    "receivedAt": "2026-01-15T10:32:00"
  }
}
```

### 5. **Frontend Integration**

#### Files Added:
- `BizFlow.Frontend/assets/js/customer-purchase-history.js` - JavaScript module
- `BizFlow.Frontend/styles/customer-purchase-history.css` - Styling

#### Usage:

```html
<!-- Include in HTML head -->
<link rel="stylesheet" href="/styles/customer-purchase-history.css">
<script src="/assets/js/customer-purchase-history.js"></script>

<!-- In your admin page -->
<div id="purchaseHistoryContainer"></div>
<div id="purchaseDetailsModal"></div>

<script>
// Load purchase history for customer ID 1
purchaseHistory.loadPurchaseHistory(1);

// Or load all purchases
purchaseHistory.loadAllPurchases(1).then(purchases => {
    console.log('All purchases:', purchases);
});

// Or get purchase count
purchaseHistory.getPurchaseCount(1).then(count => {
    console.log('Total purchases:', count);
});

// Or load by date range
purchaseHistory.loadPurchasesByDateRange(1, '2026-01-01T00:00:00', '2026-01-31T23:59:59');
</script>
```

## Database Setup

Run the migration script to create the purchase history table:

```bash
# Via command line
mysql -h localhost -P 3307 -u root -p123456 bizflow_db < db/create_purchase_history_table.sql

# Or in MySQL client
USE bizflow_db;
SOURCE db/create_purchase_history_table.sql;
```

## Configuration

### AdminOrderService (application.properties/yml)
```properties
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.linger-ms=10
```

### AdminUserService (application.properties/yml)
```properties
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.consumer.group-id=admin-user-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.max-poll-records=100
spring.kafka.consumer.session-timeout-ms=30000
```

### Docker Compose
The `docker-compose.yml` has been updated with:
- **Zookeeper** service (port 2181)
- **Kafka** broker service (ports 9092, 9101)
- Gateway dependency on Kafka startup

## Data Flow

1. **Event Generation** (AdminOrderService)
   - When an order is created/updated, create a `PurchaseEvent`
   - `OrderEventProducer.publishPurchaseEvent()` sends it to Kafka
   - Event is partitioned by `customerId` for ordering guarantees

2. **Event Consumption** (AdminUserService)
   - `PurchaseEventConsumer` listens on topic `customer-purchases`
   - Converts event to `CustomerPurchaseHistory` entity
   - Serializes `OrderItemDTO` list as JSON
   - Saves to database (upserts if order_id exists)

3. **API Query**
   - Frontend calls REST endpoints in `CustomerPurchaseHistoryController`
   - Services query `customer_purchase_history` table with filters
   - Returns paginated/filtered results as JSON

4. **Frontend Display**
   - JavaScript module formats data as HTML tables
   - Shows purchase list with status badges
   - Allows drilling down to details view
   - Supports pagination and date range filtering

## Key Design Decisions

1. **Customer ID as Message Key**: Ensures orders from same customer go to same Kafka partition, maintaining order
2. **JSON Storage for Items**: Flexible storage of order items without needing separate tables
3. **Upsert Logic**: Handles potential duplicate events (idempotent)
4. **Async Processing**: Kafka consumption is non-blocking, doesn't impact order creation
5. **Pagination**: REST API supports pagination for large datasets

## Performance Considerations

1. **Partitioning**: Topic partitioned by `customerId` for parallel processing
2. **Consumer Group**: Single consumer group ensures even distribution
3. **Batch Processing**: `max-poll-records=100` for efficient batch consumption
4. **Database Indexes**:
   - Primary key on `id`
   - Unique key on `order_id` (prevents duplicates)
   - Composite index on `(customer_id, order_created_at)` for range queries

## Error Handling

1. **Producer Side**:
   - Retry policy (3 retries) with exponential backoff
   - Logging of failed publishes
   - Non-blocking error handling

2. **Consumer Side**:
   - DefaultErrorHandler with logging
   - Graceful handling of deserialization failures
   - Duplicate detection via unique order_id

## Monitoring

Monitor Kafka consumer lag:
```bash
# Inside Kafka container
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group admin-user-service --describe
```

Monitor topic:
```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Topic details
kafka-topics.sh --bootstrap-server localhost:9092 --topic customer-purchases --describe
```

## Troubleshooting

1. **Events not appearing in database**
   - Check Kafka broker is running: `docker logs bizflow-kafka`
   - Check consumer logs: `docker logs bizflow-admin-user-service`
   - Verify consumer group: Kafka Consumer Groups command above

2. **API returns no data**
   - Verify table exists: `SHOW TABLES LIKE 'customer_purchase%';`
   - Check table contents: `SELECT COUNT(*) FROM customer_purchase_history;`
   - Verify customer exists in database

3. **Serialization errors**
   - Check Jackson is properly configured
   - Verify `PurchaseEvent` class versions match in both services
   - Review `JsonDeserializer.TRUSTED_PACKAGES` configuration

## Future Enhancements

1. **Real-time Dashboard**: WebSocket support for live updates
2. **Analytics**: Kafka Streams for customer purchase analytics
3. **Event Sourcing**: Complete event history for audit trail
4. **Caching**: Redis cache for frequently accessed purchase history
5. **Notifications**: Send email/SMS notifications on purchase events
6. **Aggregations**: Periodic aggregation of purchase statistics

## Testing

### Manual Testing Steps:

1. **Start Services**:
   ```bash
   docker-compose up -d
   ```

2. **Create Order** (via AdminOrderService):
   - Use existing API or UI
   - This triggers Kafka event publish

3. **Verify Kafka Topic**:
   ```bash
   docker exec bizflow-kafka kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic customer-purchases \
     --from-beginning
   ```

4. **Check Database**:
   ```sql
   SELECT * FROM customer_purchase_history WHERE customer_id = 1;
   ```

5. **Test API**:
   ```bash
   curl -H "Authorization: Bearer <token>" \
     http://localhost:8000/api/admin/customers/1/purchase-history
   ```

6. **Check Frontend**:
   - Navigate to admin panel
   - Click on customer
   - View purchase history section

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Docker Compose Networking](https://docs.docker.com/compose/networking/)
