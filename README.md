## Micronaut 4.2.3 Documentation

- [User Guide](https://docs.micronaut.io/4.2.3/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.2.3/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.2.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Shadow Gradle Plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)
- [Micronaut Gradle Plugin documentation](https://micronaut-projects.github.io/micronaut-gradle-plugin/latest/)
- [GraalVM Gradle Plugin documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature ksp documentation

- [Micronaut Kotlin Symbol Processing (KSP) documentation](https://docs.micronaut.io/latest/guide/#kotlin)

- [https://kotlinlang.org/docs/ksp-overview.html](https://kotlinlang.org/docs/ksp-overview.html)



NOTES
## Create DynamoDB Table

Run the following AWS CLI command to create the `Customers` table in your local DynamoDB:
```shell
aws dynamodb create-table \
  --table-name Customers \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

## Verify DynamoDB Table

After creating the table, you can verify it exists with the AWS CLI:
```shell
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

To view the details of the `Customers` table:
```shell
aws dynamodb describe-table \
  --table-name Customers \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

## Run with Docker

1. Start DynamoDB Local
```shell
docker run -d --name dynamodb-local \
  -p 8000:8000 \
  amazon/dynamodb-local
```

2. Build the Docker image
```shell
./gradlew clean buildDockerImage
```

3. Run the Micronaut service
```shell
docker run --rm \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=dummy \
  -e AWS_SECRET_ACCESS_KEY=dummy \
  -e DYNAMODB_ENDPOINT=http://host.docker.internal:8000 \
  -p 8080:8080 \
  com.dierbeck.kms/kustomer-management-service:0.1
```

## Create a Customer
```shell
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "phone": "555-1234",
    "address": "123 Main St"
  }'
```

# Verify
Open http://localhost:8080/api/customers in your browser or:
```shell
curl http://localhost:8080/api/customers
```

