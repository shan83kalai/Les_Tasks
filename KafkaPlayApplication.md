# Play-Scala-Kafka-Project

This project demonstrates a Kafka-based Scala application using [Play Framework](https://www.playframework.com/) and [Kafka](https://kafka.apache.org/).

---

## Features

- Consumes data from Kafka, processes it, and serves it via a REST API.
- Built with the Play Framework and Scala.
- Uses Docker to manage Kafka services.

---

## Prerequisites

- Docker and Docker-Compose installed.
- Java 21 installed on your system.
- SBT (Scala Build Tool) installed.

---

## Getting Started

### 1. Build the Project
Run the following command to build the project:
```bash
sbt compile
```

### 2. Package the Application
To package the application for deployment, run the following set of commands:

```bash
sbt clean stage
mkdir -p Docker/target/universal/
cp -r target/universal/stage/ Docker/target/universal/stage/
```

#### Command Explanation:

- `sbt clean stage`: Cleans previous builds and prepares the application for deployment by staging it in the `target/universal/stage/` directory.
- `mkdir -p Docker/target/universal/`: Ensures that the required directory for packaging exists.
- `cp -r target/universal/stage/ Docker/target/universal/stage/`: Copies the staged build files for deployment into the `Docker/target/universal/stage/` directory.

These steps prepare your application for deployment in a Docker container.

---

### 3. Run Docker-Compose
Start Kafka and related services:
```bash
docker-compose up
```

### 4. Run the Application
Run the Play Framework application:
```bash
sbt run
```

---

## Commands

### To List Topics from Kafka:
```bash
docker exec -it play-scala-kafka-kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### To Check Running Docker Containers:
```bash
docker ps
```

#### Description:
The `docker ps` command lists all running Docker containers with details such as:
- **Container ID**: A unique identifier for each container.
- **Image**: The Docker image the container is running.
- **Ports**: Mapping of container ports to host machine ports.
- **Names**: Friendly names assigned to containers (e.g., `play-scala-kafka-kafka`).

Use this command to confirm that required services (like `play-scala-kafka-kafka`) are running.

### To View Logs of the Kafka Container:
```bash
docker logs play-scala-kafka-kafka
```

#### Description:
The `docker logs` command retrieves logs from a specific Docker container. In this example:
- **`play-scala-kafka-kafka`**: The name of the Kafka container.
- Logs can help debug errors, verify initialization, or troubleshoot issues inside the container.

For example, you may use this command if the Kafka container is restarting or not functioning correctly.

---

## Producing and Consuming Messages in Kafka

### Producing a Message
To publish a message to the Kafka topic (`play-scala-kafka-topic`), use the **Kafka Console Producer**:

1. Run the following command:
   ```bash
   docker exec -it play-scala-kafka-kafka /opt/bitnami/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic play-scala-kafka-topic
   ```

2. A prompt will appear. Type your message and press **Enter** to send it to the topic. For example:
   ```text
   Hello Kafka my First Message!
   ```

3. You can send multiple messages by typing each message and pressing **Enter**.

4. To exit the producer, press **Ctrl+C**.

---

### Consuming Messages
To view messages sent to the Kafka topic, use the **Kafka Console Consumer**:

1. Run the following command:
   ```bash
   docker exec -it play-scala-kafka-kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic play-scala-kafka-topic --from-beginning --group play-scala-consumer-group-1
   ```

2. This command will display all messages sent to the `play-scala-kafka-topic` topic from the beginning, associating it with the consumer group `play-scala-consumer-group-1`.

3. Example output:
   ```plaintext
   Hello Kafka my First Message!
   Another message
   ```

4. If you re-run the command with the same consumer group (`play-scala-consumer-group-1`), only new messages will be consumed (since offsets are tracked for the group).

5. To stop reading messages, press **Ctrl+C**.

---

## Managing Kafka Consumer Groups

### Listing All Consumer Groups
To list all consumer groups in the Kafka cluster, use the following command:

```bash
docker exec -it play-scala-kafka-kafka /opt/bitnami/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

This command will display a list of all consumer groups that exist in Kafka. Example:
```plaintext
play-scala-consumer-group-1
another-consumer-group
```

---

These two commands—consuming messages with a consumer group and listing all existing consumer groups—now ensure better tracking and management in Kafka.