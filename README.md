# Sports Betting Settlement Trigger Service

A backend microservice that simulates sports betting event outcome handling and bet settlement using Kafka for event streaming and RocketMQ for settlement processing.

## 🏗️ Architecture Overview

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   REST API   │────>│  Kafka Producer │────>│ event-outcomes   │
│  (Publish    │     │                 │     │     (Topic)      │
│   Outcome)   │     └─────────────────┘     └──────────────────┘
└──────────────┘                                        │
                                                        ▼
                                              ┌──────────────────┐
                                              │ Kafka Consumer   │
                                              │ (Bet Matching)   │
                                              └──────────────────┘
                                                        │
                    ┌───────────────────────────────────┘
                    │
                    ▼
          ┌─────────────────┐     ┌──────────────────┐
          │ RocketMQ Prod.  │────>│ bet-settlements  │
          │                 │     │     (Topic)      │
          └─────────────────┘     └──────────────────┘
                                            │
                                            ▼
                                  ┌──────────────────┐
                                  │ RocketMQ Consumer│
                                  │ (Bet Settlement) │
                                  └──────────────────┘
                                            │
                                            ▼
                                  ┌──────────────────┐
                                  │   H2 Database    │
                                  │  (Bet Updates)   │
                                  └──────────────────┘
```

## 📋 Prerequisites

- **Java 17 LTS** or higher
- **Gradle 8.6** (or use wrapper)
- **Docker** and **Docker Compose** (for Kafka and RocketMQ)
- **Git** (for cloning the repository)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd int-sporty-group
```

### 2. Start Infrastructure Services

Start Kafka, ZooKeeper, and RocketMQ using Docker Compose:

```bash
docker-compose up -d
```

Verify services are running:

```bash
docker-compose ps
```

Expected output:
- zookeeper (port 2181)
- kafka (port 9092)
- rocketmq-namesrv (port 9876)
- rocketmq-broker (ports 10909, 10911, 10912)
- kafka-ui (port 8090) - Optional monitoring UI
- rocketmq-console (port 8091) - Optional monitoring UI

### 3. Build the Application

```bash
./gradlew clean build
```

This will:
- Compile the source code
- Run unit tests
- Run integration tests
- Generate JaCoCo coverage report

### 4. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

Alternative: Run using the JAR file:
```bash
java -jar build/libs/int-sporty-group-1.0.0.jar
```

### 5. Verify Application is Running

Access the H2 Console:
```
http://localhost:8080/h2-console
```

JDBC URL: `jdbc:h2:mem:bettingdb`
Username: `sa`
Password: (leave blank)

Check Actuator Health:
```bash
curl http://localhost:8080/actuator/health
```

## 📡 API Documentation

### Publish Event Outcome

**Endpoint:** `POST /api/events/outcomes`

**Description:** Publishes a sports event outcome to Kafka, triggering the bet settlement process.

**Request Body:**
```json
{
  "eventId": "EVT-001",
  "eventName": "Manchester United vs Liverpool",
  "eventWinnerId": "TEAM-A"
}
```

**Response:** `202 Accepted`
```json
{
  "message": "Event outcome published successfully",
  "eventId": "EVT-001"
}
```

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/events/outcomes \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-001",
    "eventName": "Manchester United vs Liverpool",
    "eventWinnerId": "TEAM-A"
  }'
```

**Example using HTTPie:**
```bash
http POST http://localhost:8080/api/events/outcomes \
  eventId="EVT-001" \
  eventName="Manchester United vs Liverpool" \
  eventWinnerId="TEAM-A"
```

### Test Scenarios

The application comes with pre-loaded test data (see `data.sql`). Here are some test scenarios:

#### Scenario 1: Settle EVT-001 with TEAM-A winning
```bash
curl -X POST http://localhost:8080/api/events/outcomes \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-001",
    "eventName": "Football Match",
    "eventWinnerId": "TEAM-A"
  }'
```
**Expected Result:** Bets from USER-001, USER-002, USER-003, USER-015 marked as WON. Bets from USER-004, USER-005, USER-006, USER-016 marked as LOST.

#### Scenario 2: Settle EVT-002 with LAKERS winning
```bash
curl -X POST http://localhost:8080/api/events/outcomes \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-002",
    "eventName": "Basketball Game",
    "eventWinnerId": "LAKERS"
  }'
```
**Expected Result:** Bet from USER-009 marked as WON. Bet from USER-010 marked as LOST.

## 🧪 Testing

### Run All Tests

```bash
./gradlew test integrationTest
```

### Run Unit Tests Only

```bash
./gradlew test
```

### Run Integration Tests Only

```bash
./gradlew integrationTest
```

### Generate Coverage Report

```bash
./gradlew jacocoTestReport
```

View the report at: `build/reports/jacoco/index.html`

### Coverage Verification

```bash
./gradlew jacocoTestCoverageVerification
```

**Coverage Thresholds:**
- Line Coverage: WIP
- Branch Coverage: WIP

### Test with Coverage in One Command

```bash
./gradlew testCoverage
```

## 🔧 Configuration

### Application Profiles

The application uses Spring Boot profiles for different environments:

- **Default** - Development mode with H2 database
- **Test** - Integration test mode

### RocketMQ Configuration

The application supports both real and mock RocketMQ implementations:

**Mock Mode (Default):**
```yaml
application:
  rocketmq:
    enabled: false
```
Settlement messages are logged instead of sent to RocketMQ.

**Real RocketMQ Mode:**
```yaml
application:
  rocketmq:
    enabled: true
    name-server: localhost:9876
```
Settlement messages are sent to actual RocketMQ broker.

### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Application port |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka broker address |
| `application.kafka.topics.event-outcomes` | event-outcomes | Kafka topic for event outcomes |
| `application.rocketmq.enabled` | false | Enable/disable real RocketMQ |
| `application.rocketmq.name-server` | localhost:9876 | RocketMQ NameServer address |
| `application.rocketmq.topics.bet-settlements` | bet-settlements | RocketMQ topic for settlements |

## 📊 Monitoring

### Kafka UI

Access Kafka UI for monitoring topics, messages, and consumer groups:
```
http://localhost:8090
```

### RocketMQ Console

Access RocketMQ Console for monitoring topics and message queues:
```
http://localhost:8091
```

### Spring Boot Actuator

Available endpoints:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

## 🏗️ Project Structure

```
sports-betting-settlement/
├── src/
│   ├── main/
│   │   ├── java/com/betting/settlement/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── service/             # Business logic services
│   │   │   ├── kafka/               # Kafka producers/consumers
│   │   │   ├── rocketmq/            # RocketMQ producers/consumers
│   │   │   └── SettlementApplication.java
│   │   └── resources/
│   │       ├── application.yml      # Application configuration
│   │       └── data.sql             # Initial test data
│   ├── test/
│   │   ├── java/com/betting/settlement/
│   │   │   └── unit/                # Unit tests
│   │   └── resources/
│   │       └── application-test.yml
│   └── integration-test/
│       ├── java/com/betting/settlement/
│       │   └── integration/         # Integration tests
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml               # Docker services configuration
├── build.gradle                     # Gradle build configuration
├── settings.gradle
└── README.md
```

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | OpenJDK | 17 LTS |
| Build Tool | Gradle | 8.6 |
| Framework | Spring Boot | 3.2.x |
| Messaging | Apache Kafka | 3.6.x |
| Messaging | Apache RocketMQ | 5.1.x |
| Database | H2 | In-memory |
| ORM | Spring Data JPA + Hibernate | - |
| Serialization | Jackson | - |
| Testing | JUnit 5 + Mockito | - |
| Integration Testing | Testcontainers + AssertJ | - |
| Code Coverage | JaCoCo | 0.8.11 |

## 🔄 System Flow

1. **Event Outcome Publication**: Client sends POST request to `/api/events/outcomes`
2. **Kafka Producer**: EventOutcomeService publishes message to `event-outcomes` topic
3. **Kafka Consumer**: EventOutcomeConsumer receives the message
4. **Bet Matching**: BetMatchingService queries database for pending bets matching the event
5. **Settlement Determination**: Compares predicted winner with actual winner
6. **RocketMQ Producer**: Sends settlement messages to `bet-settlements` topic
7. **RocketMQ Consumer**: Receives settlement messages
8. **Database Update**: BetSettlementService updates bet status (WON/LOST) and timestamp

## 🐛 Troubleshooting

### Kafka Connection Issues

**Problem:** Application can't connect to Kafka
```
Failed to send message to Kafka: Connection refused
```

**Solution:**
1. Verify Kafka is running: `docker-compose ps kafka`
2. Check Kafka logs: `docker-compose logs kafka`
3. Ensure port 9092 is not in use: `lsof -i :9092`
4. Restart Kafka: `docker-compose restart kafka`

### RocketMQ Connection Issues

**Problem:** RocketMQ connection timeout
```
connect to <localhost:9876> failed
```

**Solution:**
1. Verify RocketMQ services are running: `docker-compose ps rocketmq-namesrv rocketmq-broker`
2. Check RocketMQ logs: `docker-compose logs rocketmq-namesrv rocketmq-broker`
3. Use mock mode by setting `application.rocketmq.enabled=false`

### H2 Database Issues

**Problem:** Can't access H2 console

**Solution:**
1. Ensure application is running
2. Verify URL: `http://localhost:8080/h2-console`
3. Check JDBC URL: `jdbc:h2:mem:bettingdb`
4. Use username: `sa`, password: (blank)

### Test Failures

**Problem:** Integration tests fail with container startup errors

**Solution:**
1. Ensure Docker is running
2. Check Docker has sufficient resources (memory, CPU)
3. Clean Gradle cache: `./gradlew clean`
4. Retry: `./gradlew integrationTest --rerun-tasks`

### Port Conflicts

**Problem:** Port already in use

**Solution:**
1. Find process using port: `lsof -i :<port_number>`
2. Kill process: `kill -9 <PID>`
3. Or change port in `application.yml` or `docker-compose.yml`

## 📝 Development Notes

### Adding New Features

1. Create feature branch: `git checkout -b feature/your-feature`
2. Implement changes following the existing package structure
3. Write unit tests (target 80%+ coverage)
4. Write integration tests for end-to-end scenarios
5. Update documentation
6. Run all tests: `./gradlew test integrationTest`
7. Submit pull request

### Code Quality

The project enforces code quality through:
- JaCoCo coverage thresholds (WIP)
- Unit and integration test requirements
- Spring Boot best practices
- Clean architecture with separation of concerns

### Best Practices

- Use constructor injection over field injection
- Keep services focused on single responsibility
- Use DTOs for data transfer between layers
- Implement proper exception handling
- Add comprehensive logging
- Write meaningful test cases

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is created for educational and assessment purposes.

## 👥 Contact

For questions or issues, please open an issue in the repository.

---

**Happy Coding! 🚀**
