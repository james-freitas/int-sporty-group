# Sports Betting Settlement Service - Complete Code Reference

## Project Structure Overview

```
sports-betting-settlement/
├── src/
│   ├── main/java/com/betting/settlement/
│   │   ├── SettlementApplication.java          # Main Spring Boot application
│   │   ├── config/                             # Configuration classes
│   │   │   ├── KafkaConfig.java                # Kafka producer/consumer config
│   │   │   ├── KafkaTopicConfig.java           # Kafka topic names config
│   │   │   └── RocketMQConfig.java             # RocketMQ configuration
│   │   ├── controller/                         # REST controllers
│   │   │   ├── EventOutcomeController.java     # POST /api/events/outcomes
│   │   │   └── GlobalExceptionHandler.java     # Centralized error handling
│   │   ├── dto/                                # Data Transfer Objects
│   │   │   ├── ApiResponse.java                # Standard API response
│   │   │   ├── BetSettlementDTO.java           # RocketMQ message format
│   │   │   ├── EventOutcomeDTO.java            # Kafka message format
│   │   │   └── PublishEventRequest.java        # REST API request
│   │   ├── entity/                             # JPA entities
│   │   │   ├── Bet.java                        # Bet entity with JPA annotations
│   │   │   └── BetStatus.java                  # Enum: PENDING, WON, LOST
│   │   ├── kafka/                              # Kafka components
│   │   │   ├── EventOutcomeConsumer.java       # @KafkaListener for event-outcomes
│   │   │   └── EventOutcomeProducer.java       # Sends to event-outcomes topic
│   │   ├── repository/                         # Data access layer
│   │   │   └── BetRepository.java              # JPA repository for Bet
│   │   ├── rocketmq/                           # RocketMQ components
│   │   │   ├── BetSettlementConsumer.java      # RocketMQ consumer (real)
│   │   │   ├── BetSettlementProducer.java      # Interface for producers
│   │   │   ├── MockBetSettlementConsumer.java  # Mock consumer
│   │   │   ├── MockBetSettlementProducer.java  # Mock producer (logs only)
│   │   │   └── RealBetSettlementProducer.java  # Real RocketMQ producer
│   │   └── service/                            # Business logic
│   │       ├── BetMatchingService.java         # Matches bets to outcomes
│   │       ├── BetSettlementService.java       # Updates bet status in DB
│   │       └── EventOutcomeService.java        # Publishes to Kafka
│   └── test/java/com/betting/settlement/unit/
│       ├── controller/
│       │   └── EventOutcomeControllerTest.java # @WebMvcTest
│       ├── repository/
│       │   └── BetRepositoryTest.java          # @DataJpaTest
│       └── service/
│           ├── BetMatchingServiceTest.java     # Mockito tests
│           └── BetSettlementServiceTest.java   # Mockito tests
├── src/integration-test/java/com/betting/settlement/integration/
│   └── BetSettlementIntegrationTest.java       # @SpringBootTest with Testcontainers
├── build.gradle                                # Gradle build configuration
├── docker-compose.yml                          # Kafka + RocketMQ infrastructure
└── README.md                                   # Project documentation
```

## Class Dependencies Summary

### Layer 1: Domain & DTOs (No Dependencies)

**Bet.java**
- Dependencies: JPA annotations, Lombok, BetStatus enum
- Purpose: JPA entity representing a bet in the database
- Key methods: `markAsWon()`, `markAsLost()`

**BetStatus.java**
- Dependencies: None
- Values: PENDING, WON, LOST

**EventOutcomeDTO.java**
- Dependencies: Jackson annotations, Bean Validation
- Purpose: Kafka message format for event outcomes

**BetSettlementDTO.java**
- Dependencies: Jackson annotations
- Purpose: RocketMQ message format for settlements

**PublishEventRequest.java**
- Dependencies: Bean Validation
- Purpose: REST API request body

**ApiResponse.java**
- Dependencies: Lombok
- Purpose: Standard REST response wrapper

### Layer 2: Repository (Depends on Domain)

**BetRepository.java**
- Dependencies: Spring Data JPA, Bet entity, BetStatus enum
- Key method: `findByEventIdAndStatus()` - finds pending bets for settlement

### Layer 3: Configuration

**KafkaConfig.java**
- Dependencies: Spring Kafka, EventOutcomeDTO
- Configures: Producer, Consumer, KafkaTemplate

**KafkaTopicConfig.java**
- Dependencies: @ConfigurationProperties
- Provides: Topic names from application.yml

**RocketMQConfig.java**
- Dependencies: @ConfigurationProperties
- Provides: RocketMQ settings (enabled flag, nameserver, topics)

### Layer 4: Services (Business Logic)

**BetMatchingService.java**
- Dependencies: BetRepository, EventOutcomeDTO, BetSettlementDTO
- Input: EventOutcomeDTO from Kafka
- Output: List<BetSettlementDTO> for RocketMQ
- Logic: Compares bet predictions with actual outcome

**BetSettlementService.java**
- Dependencies: BetRepository, BetSettlementDTO
- Input: BetSettlementDTO from RocketMQ
- Output: Updates database
- Transaction: @Transactional

**EventOutcomeService.java**
- Dependencies: KafkaTemplate, KafkaTopicConfig, EventOutcomeDTO
- Input: EventOutcomeDTO from REST API
- Output: Publishes to Kafka

### Layer 5: Messaging Components

**EventOutcomeProducer.java** (Kafka)
- Dependencies: KafkaTemplate, KafkaTopicConfig, EventOutcomeDTO
- Used by: EventOutcomeService

**EventOutcomeConsumer.java** (Kafka)
- Dependencies: @KafkaListener, BetMatchingService, BetSettlementProducer
- Listens to: event-outcomes topic
- Triggers: Bet matching and settlement

**BetSettlementProducer.java** (Interface)
- Implementations: MockBetSettlementProducer, RealBetSettlementProducer

**MockBetSettlementProducer.java**
- Dependencies: ObjectMapper, MockBetSettlementConsumer
- Active when: application.rocketmq.enabled=false
- Behavior: Logs and processes settlements directly

**RealBetSettlementProducer.java**
- Dependencies: RocketMQ Client, ObjectMapper, RocketMQConfig
- Active when: application.rocketmq.enabled=true
- Behavior: Sends to real RocketMQ broker

**BetSettlementConsumer.java** (RocketMQ)
- Dependencies: RocketMQ Client, BetSettlementService
- Active when: application.rocketmq.enabled=true
- Processes: Bet settlements from RocketMQ

**MockBetSettlementConsumer.java**
- Dependencies: BetSettlementService
- Active when: application.rocketmq.enabled=false
- Called by: MockBetSettlementProducer

### Layer 6: Controllers (HTTP Interface)

**EventOutcomeController.java**
- Dependencies: EventOutcomeService, PublishEventRequest, ApiResponse
- Endpoints:
    - POST /api/events/outcomes - Publish event outcome
    - GET /api/events/health - Health check

**GlobalExceptionHandler.java**
- Dependencies: @RestControllerAdvice
- Handles: Validation errors, IllegalArgumentException, generic exceptions

## Message Flow

1. **API Request** → EventOutcomeController
2. **Controller** → EventOutcomeService
3. **Service** → Kafka Producer (KafkaTemplate)
4. **Kafka Topic** → event-outcomes
5. **Kafka Consumer** → EventOutcomeConsumer
6. **Consumer** → BetMatchingService
7. **Matching Service** → BetRepository (query pending bets)
8. **Matching Service** → BetSettlementProducer (Mock or Real)
9. **RocketMQ Topic** → bet-settlements (if real) OR direct processing (if mock)
10. **Settlement Consumer** → BetSettlementService
11. **Settlement Service** → BetRepository (update bet status)

## Testing Strategy

### Unit Tests (Mockito + JUnit 5)
- **BetMatchingServiceTest**: Tests bet matching logic with mocked repository
- **BetSettlementServiceTest**: Tests status updates with mocked repository
- **EventOutcomeControllerTest**: @WebMvcTest with mocked service layer
- **BetRepositoryTest**: @DataJpaTest with H2 in-memory database

### Integration Tests (Testcontainers + AssertJ)
- **BetSettlementIntegrationTest**: Full flow with real Kafka (containerized)
    - Uses @SpringBootTest
    - Uses KafkaContainer from Testcontainers
    - Uses Awaitility for async assertions
    - Mock RocketMQ enabled

## Key Design Patterns

1. **Repository Pattern**: BetRepository for data access abstraction
2. **Strategy Pattern**: BetSettlementProducer interface with Mock/Real implementations
3. **Observer Pattern**: Kafka consumers listening to topic events
4. **DTO Pattern**: Separation of domain entities from API/messaging contracts
5. **Dependency Injection**: Constructor injection throughout (via Lombok @RequiredArgsConstructor)

## Configuration Profiles

### Development (application.yml)
- H2 in-memory database
- Kafka: localhost:9092
- RocketMQ: Mock (enabled=false)
- Logging: DEBUG for application code

### Test (application-test.yml)
- H2 test database
- Kafka: Testcontainers
- RocketMQ: Always mock
- Logging: Reduced verbosity

### Production (would add application-prod.yml)
- Real database (PostgreSQL/MySQL)
- Kafka cluster
- RocketMQ cluster (enabled=true)
- Logging: INFO/WARN only

## Build & Run Commands

### Build
```bash
./gradlew clean build
```

### Run Tests
```bash
./gradlew test                    # Unit tests only
./gradlew integrationTest         # Integration tests only
./gradlew test integrationTest    # All tests
./gradlew testCoverage           # All tests + coverage report
```

### Run Application
```bash
# Start infrastructure
docker-compose up -d

# Run application
./gradlew bootRun

# Or run JAR
java -jar build/libs/sports-betting-settlement-1.0.0-SNAPSHOT.jar
```

### View Coverage Report
```bash
./gradlew jacocoTestReport
open build/reports/jacoco/index.html
```

## Important Implementation Notes

1. **Transaction Management**: BetSettlementService uses @Transactional to ensure atomicity
2. **Manual Acknowledgment**: Kafka consumer uses manual ack for reliability
3. **Error Handling**: GlobalExceptionHandler provides consistent error responses
4. **Async Processing**: API returns 202 Accepted immediately, processing happens async
5. **Mock Mode**: Default configuration uses mock RocketMQ for easier local development
6. **Indexing**: Database index on (eventId, status) for fast bet queries
7. **Serialization**: Jackson for JSON, configured for both Kafka and RocketMQ
8. **Conditional Beans**: @ConditionalOnProperty for Mock vs Real RocketMQ beans

## Dependencies Summary

### Runtime
- Spring Boot 3.2.x (Web, Data JPA, Validation, Actuator)
- Spring Kafka 3.1.x
- Kafka 3.6.x
- RocketMQ Client 5.1.x
- H2 Database
- Jackson (JSON)
- Lombok

### Test
- JUnit 5
- Mockito
- Hamcrest
- AssertJ
- Spring Boot Test
- Testcontainers (Kafka)
- Awaitility (async testing)

### Build
- Gradle 8.6
- JaCoCo (coverage)