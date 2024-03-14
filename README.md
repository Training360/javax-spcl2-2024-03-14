# Spring Cloud Function and Stream

## Spring Cloud Function használatba vétele

* `function-demo`
* Függőségek: Web, Function (`spring-cloud-function-context`, `spring-cloud-function-web`), Lombok

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequest {

    private double a;

    private double b;
}
```

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculationResponse {

    public double result;

}
```

```java
@Bean
public Function<CalculationRequest, CalculationResponse> calculate() {
  return request -> new CalculationResponse(request.getA() + request.getB());
}
```

```http
POST http://localhost:8080/calculate
Content-Type: application/json

{
  "a": 1.1234,
  "b": 2.2468
}
```

## Spring Cloud Function tesztelése

```java
package training.functiondemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CalculateTest {

    @Autowired
    FunctionCatalog functionCatalog;

    @Test
    void calculate() {
        var function = (Function<CalculationRequest, CalculationResponse>) functionCatalog.lookup("calculate");
        var response = function.apply(new CalculationRequest(1.1234, 2.1234));
        assertEquals(3.2468, response.getResult(), 0.00005);
    }
}
```

## Function Composition (kerekítés két tizedesjegyre)

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoundResponse {

    private String result;
}
```

* `CalculationResponse` osztályba:

```java
public RoundResponse round() {
    return new RoundResponse(BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP).toString());
}
```

* Application osztályba:

```java
@Bean
public Function<CalculationResponse, RoundResponse> round() {
    return response -> response.round();
}
```

```http
POST http://localhost:8080/calculate,round
Content-Type: application/json

{
  "a": 1.1234,
  "b": 3.2468
}
```

* Elválasztó karakter a `,`

## Spring Cloud Stream bevezetése Kafkával

* Függőségek: Stream, Kafka
* Futtatás
* `spring.cloud.function.definition`
* Kafka plugin
* Fogadás, küldés

## Topic-ok konfigurálása

```
spring.cloud.stream.bindings.calculate|round-in-0.destination=calculation-request
spring.cloud.stream.bindings.calculate|round-out-0.destination=calculation-response
```

## Átállás RabbitMQ-ra

* `org.springframework.cloud:spring-cloud-stream-binder-kafka` megjegyzésbe

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
```

```properties
spring.cloud.stream.bindings.calculate|round-out-0.producer.required-groups=calculator-group
# Routes all
spring.cloud.stream.rabbit.bindings.calculate|round-out-0.producer.binding-routing-key=#
```

* Üzenet küldhető a `calculation-request` exchange-be

```json
{"a": 1, "b": 2}
```

* Üzenet olvasható a `calculation-response.calculator-group` sorból

## Alkalmazás bemutatása - backend

* Adatbázis

```shell
docker run -d -e POSTGRES_DB=employees -e POSTGRES_USER=employees -e POSTGRES_PASSWORD=employees -p 5432:5432 --name employees-postgres postgres
```

* Spring Boot alkalmazás, `pom.xml`
* Spring Data JPA, Spring MVC, RestController
* Alkalmazás elindítása
* SwaggerUI, `.http` file
* `application.yaml`
* Liquibase
* Felépítése: entity, repo, service, resource, controller
* Thymeleaf templates
* DataSource

## Alkalmazás bemutatása - frontend

* Spring Boot alkalmazás, `pom.xml`
* Spring Data JPA, Spring MVC, RestController
* Alkalmazás elindítása
* Felület
* `application.yaml`
* Liquibase
* Felépítése: entity, repo, service, resource, controller
* Thymeleaf templates
* DataSource

# Spring Cloud Stream

Spring Integration projekt az EAI egy implementációja.
A Spring Boot és Spring Integration integrálásából született a Spring Cloud Stream projekt.
Különösen alkalmas microservice architektúrában event driven megközelítés implementálására.
Message broker implementáció cserélhető.
Erős content-type támogatás, konverziók.

* Alkalmazásban input és output van
* Binder köti ezt össze a brokerrel. Ilyen binder van pl. Kafkához, RabbitMQ-hoz,
  és a teszteléshez test binder.
* Egyszerre akár több bindert is használhat az alkalmazás, konfigurációban választható, hogy hol melyiket
  használja.
* Támogatja a publish-subscribe modellt
* Támogatja a terheléselosztást, Consumer Group használatával (Kafka alapján). Ugyanabba a Consumer
  Groupba tartozó alkalmazások közül csak egy kapja meg az üzenetet
* Partícionálás: több példány esetén az összetartozó üzeneteket ugyanaz a példány kapja meg.
  Hasznos az állapotfüggő feldolgozáskor. Küldő és fogadó oldalon is be kell állítani.

## Kafka üzenet küldése üzleti logikából

* Függőség: Stream, Kafka
* `CreateEmployeeCommand`

```java
@Service
@AllArgsConstructor
public class EmployeeBackendGateway {

    private StreamBridge streamBridge;

    public void createEmployee(Employee employee) {
        streamBridge.send("employee-backend-command",
                new CreateEmployeeCommand(employee.getName()));
    }

}
```

* Kafka topic

## Kafka üzenetfogadás és válasz

* Függőség: Stream, Kafka
* `CreateEmployeeCommand`
* `EmployeeHasBeenCreatedEvent`

```java
@Configuration(proxyBeanMethods = false)
@Slf4j
public class GatewayConfig {

    @Autowired
    private EmployeesService employeesService;

    @Bean
    public Function<CreateEmployeeCommand, EmployeeHasBeenCreatedEvent> createEmployee() {
        return command -> {
            var created = employeesService.createEmployee(new EmployeeResource(command.getName()));
            var event = new EmployeeHasBeenCreatedEvent(created.getId(), created.getName());
            log.debug("Event: {}", event);
            return event;
        };
    }
}
```

* Binding: binder hozza létre, kapcsolat a broker és a producer/consumer között. Mindig van neve.
  Alapértelmezetten: `<bean neve> + -in- + <index>`, vagy `out`. Hozzárendelhető a 
  broker topic-ja.

```
spring.cloud.stream.function.bindings.createEmployee-in-0=employee-backend-command
spring.cloud.stream.function.bindings.createEmployee-out-0=employee-backend-event
```

* Meg lehet az implicit binding névhez adni explicit nevet is, de talán ez egy felesleges absztrakciós szint.

```properties
spring.cloud.stream.function.bindings.createEmployee-in-0=createEmployeeInput
```

* Ha csak egy `java.util.function.[Supplier/Function/Consumer]` van, akkor azt automatikusan bekonfigurálja,
nem kell a `spring.cloud.function.definition` property. Azonban legjobb gyakorlat használni.

## Kafka üzenet fogadása

* `EmployeeHasBeenCreatedEvent`

```java
@Configuration(proxyBeanMethods = false)
@Slf4j
public class GatewayConfig {

    @Autowired
    private EmployeesService employeesService;

    @Bean
    public Consumer<EmployeeHasBeenCreatedEvent> employeeCreated() {
        return command -> log.debug("Event: {}", event);
    }
}
```

```
spring.cloud.stream.function.bindings.employeeCreated-in-0=employee-backend-event
```

## Polling Supplier esetén

```java
@Bean
public Supplier<String> tick() {
    return () -> {
        log.debug("Tick");
        return "Hello from Supplier " + LocalDateTime.now();
    };

}
```

* Alapból egy polling, másodpercenként

```yaml
spring:
  cloud:
    function:
          definition: createEmployee;tick
    stream:
      bindings:
        tick-out-0:
          producer:
            poller:
              initial-delay: 0
              fixed-delay: 5000
          destination: employee-backend-tick
```

* `spring.cloud.function.definition` elválasztókarakter a `;`
* Poller globálisan, és beanenként is felülírható

## Hibakezelés

```java
log.info("Command: {}", command);
if (command.getName().isBlank()) {
    throw new IllegalArgumentException("Name is blank");
}
```

Háromszor írja ki:

```plain
2024-03-13T18:07:51.986+01:00  INFO 21532 --- [container-0-C-1] employees.GatewayConfig                  : Command: {"name": ""}
```

* Először naplóz, utána eldobja az üzenetet

```java
@Bean
public Consumer<ErrorMessage> employeesErrorHandler() {
    return e -> log.error("Error handle message: {}", e.getPayload().getMessage());
}
```

```yaml
spring:
  cloud:
    stream:
      bindings:
        createEmployee-in-0:
          error-handler-definition: employeesErrorHandler
```

## DLQ

```yaml
spring:
  cloud:
    stream:
      bindings:
        createEmployee-in-0:    
          group: employee-backend
      kafka:
        bindings:
          createEmployee-in-0:
            consumer:
              enable-dlq: true
```

Ekkor nem fut le az error handler.

## Tracing

Frontend:

* `pom.xml`

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.reporter2</groupId>
  <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

* `application.yaml`

```yaml
management:
  tracing:
    sampling:
      probability: 1.0

spring:
  application:
    name: employees-frontend

  cloud:
    stream:
      kafka:
        binder:
          enable-observation: true
```

Backend:

* Ugyanez, `spring.application.name` értéke `employees-backend`

## Schema registry

Önálló Springes alkalmazás, REST API-val, H2 adatbázissal, JPA-val

* Spring Web függőség
* `spring-cloud-stream-schema-registry-core` függőség a `pom.xml`-be

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-schema-registry-core</artifactId>
    <version>4.0.5</version>
</dependency>
```

* `@EnableSchemaRegistryServer` annotáció
* `application.properties`

```properties
server.port=8990
spring.application.name=schema-registry
```

## Avro formátumú üzenet küldése - frontend

* `pom.xml`

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-stream-schema-registry-client</artifactId>
</dependency>
```

* Tranzitívan hivatkozik az Avrora

```xml
<plugin>
  <groupId>org.apache.avro</groupId>
  <artifactId>avro-maven-plugin</artifactId>
  <version>1.11.3</version>
  <configuration>
    <stringType>String</stringType>
  </configuration>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>schema</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

* Application osztályon `@EnableSchemaRegistryClient` annotáció

* IDEA Avro IDL Support plugin
* `src/main/avro/CreateEmployeeCommand.avsc`

```json
{
    "type": "record",
    "name": "CreateEmployeeCommand",
    "namespace": "employees",
    "fields": [
        {
            "name": "name",
            "type": "string"
        }
    ]
}
```

* `src/main/avro/EmployeeHasBeenCreatedEvent.avsc`

```json
{
    "type": "record",
    "name": "EmployeeHasBeenCreatedEvent",
    "namespace": "employees",
    "fields": [
        {
            "name": "id",
            "type": "long"
        },
        {
            "name": "name",
            "type": "string"
        }
    ]
}
```

* `CreateEmployeeCommand`, `EmployeeHasBeenCreatedEvent` törlése
* `mvn clean package`, Maven frissítés
* `EmployeeBackendGateway`

```java
streamBridge.send("employee-backend-command", command);
```

* `application.yaml`

```yaml
spring:
  cloud:
    stream:
      bindings:
          createEmployee:
            destination: employee-backend-command
            contentType: application/*+avro
          employeeCreated-in-0:
            destination: employee-backend-event
            contentType: application/*+avro
```

Schema registry:

```http
### Schema registry
GET http://localhost:8990/createemployeecommand/avro
```

## Avro formátumú üzenet fogadása, válasz - backend


```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-stream-schema-registry-client</artifactId>
</dependency>
```

```xml
<plugin>
  <groupId>org.apache.avro</groupId>
  <artifactId>avro-maven-plugin</artifactId>
  <version>1.11.3</version>
  <configuration>
    <stringType>String</stringType>
  </configuration>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>schema</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

* Application osztályon `@EnableSchemaRegistryClient` annotáció
* `.avsc` fájlok másolása
* `CreateEmployeeCommand`, `EmployeeHasBeenCreatedEvent` törlése
* `mvn clean package`, Maven frissítés
* `application.yaml`

```yaml
spring:
  cloud:
    function:
      definition: createEmployee
    stream:
        bindings:
          createEmployee-in-0:
            destination: employee-backend-command
            contentType: application/*+avro
          createEmployee-out-0:
            destination: employee-backend-event
            contentType: application/*+avro
```

* Avro formátumú üzenetek kezelése az IDEA Kafka pluginban
