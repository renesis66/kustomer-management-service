package com.dierbeck.kms.data

import com.dierbeck.kms.error.CustomerNotFoundException
import com.dierbeck.kms.error.CustomerServiceException
import com.dierbeck.kms.error.logger
import io.micronaut.core.annotation.Introspected
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlinx.serialization.Serializable
import io.micronaut.serde.annotation.Serdeable

@Serdeable.Serializable
@Serdeable.Deserializable
data class CustomerDTO(
    val id: String? = null,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,  // Add this field
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serdeable.Serializable
@Serdeable.Deserializable
data class Customer(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,  // Add this
    val createdAt: String,
    val updatedAt: String
)
@Singleton
class CustomerRepository(
    @Value("\${dynamodb.table.name}") private val tableName: String
) {
    private val logger = logger()
    
    private val dynamoDbClient: DynamoDbAsyncClient = run {
        val endpointUrl = System.getenv("DYNAMODB_ENDPOINT") ?: "http://dynamodb-local:8000"
        logger.info("Using DynamoDB endpoint: $endpointUrl")
        DynamoDbAsyncClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy", "dummy")
                )
            )
            .endpointOverride(URI.create(endpointUrl))
            .build()
    }

    init {
        logger.info("Initializing CustomerRepository with table: $tableName and client: $dynamoDbClient")
        logDynamoDbEndpoint()
        runBlocking {
            createTableIfNotExists()
        }
    }

    fun logDynamoDbEndpoint() {
        logger.info("DynamoDB client is configured to use endpoint: ${dynamoDbClient}")
    }

    suspend fun create(customerDTO: CustomerDTO): Customer {
        try {
            val timestamp = LocalDateTime.now().toString()
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                firstName = customerDTO.firstName,
                lastName = customerDTO.lastName,
                email = customerDTO.email,
                phone = customerDTO.phone,
                address = customerDTO.address,  // Add this
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            logger.info("Creating customer with DTO: $customerDTO")
            logger.info("Saving customer to DynamoDB: $customer")
            
            val item = mapOf(
                "id" to AttributeValue.builder().s(customer.id).build(),
                "firstName" to AttributeValue.builder().s(customer.firstName).build(),
                "lastName" to AttributeValue.builder().s(customer.lastName).build(),
                "email" to AttributeValue.builder().s(customer.email).build(),
                "phone" to customer.phone?.let { AttributeValue.builder().s(it).build() },
                "address" to customer.address?.let { AttributeValue.builder().s(it).build() },  // Add this
                "createdAt" to AttributeValue.builder().s(customer.createdAt).build(),
                "updatedAt" to AttributeValue.builder().s(customer.updatedAt).build()
            ).filterValues { it != null }

            dynamoDbClient.putItem { builder ->
                builder.tableName(tableName)
                    .item(item)
            }.await()

            return customer
        } catch (e: Exception) {
            logger.error("Error creating customer", e)
            throw e
        }
    }

    suspend fun findAll(): List<Customer> {
        try {
            val request = ScanRequest.builder()
                .tableName(tableName)
                .build()

            val response = dynamoDbClient.scan(request).await()
            
            logger.info("Raw DynamoDB items: ${response.items()}")

            return response.items().map { item ->
                Customer(
                    id = item["id"]!!.s(),
                    firstName = item["firstName"]!!.s(),
                    lastName = item["lastName"]!!.s(),
                    email = item["email"]!!.s(),
                    phone = item["phone"]?.s(),
                    address = item["address"]?.s(),
                    createdAt = item["createdAt"]!!.s(),
                    updatedAt = item["updatedAt"]!!.s()
                ).also {
                    logger.info("Mapped Customer: $it")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch all customers", e)
            throw CustomerServiceException("Failed to fetch all customers", e)
        }
    }

    suspend fun findById(id: String): Customer? {
        try {
            val request = GetItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("id" to AttributeValue.builder().s(id).build()))
                .build()

            val response = dynamoDbClient.getItem(request).await()
            
            return response.item()?.let { item ->
                Customer(
                    id = item["id"]!!.s(),
                    firstName = item["firstName"]!!.s(),
                    lastName = item["lastName"]!!.s(),
                    email = item["email"]!!.s(),
                    phone = item["phone"]?.s(),
                    address = item["address"]?.s(),
                    createdAt = item["createdAt"]!!.s(),
                    updatedAt = item["updatedAt"]!!.s()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch customer with id: $id", e)
            throw CustomerServiceException("Failed to fetch customer with id: $id", e)
        }
    }

    suspend fun update(id: String, updates: CustomerDTO): Customer {
        try {
            // First check if customer exists
            findById(id) ?: throw CustomerNotFoundException(id)

            val now = Instant.now().toString()
            
            val updateExpressions = mutableListOf<String>()
            val expressionAttributeValues = mutableMapOf<String, AttributeValue>()
            val expressionAttributeNames = mutableMapOf<String, String>()

            // Add all fields to update
            updateExpressions.add("#updatedAt = :updatedAt")
            expressionAttributeValues[":updatedAt"] = AttributeValue.builder().s(now).build()
            expressionAttributeNames["#updatedAt"] = "updatedAt"

            updates.firstName.let {
                updateExpressions.add("#firstName = :firstName")
                expressionAttributeValues[":firstName"] = AttributeValue.builder().s(it).build()
                expressionAttributeNames["#firstName"] = "firstName"
            }

            updates.lastName.let {
                updateExpressions.add("#lastName = :lastName")
                expressionAttributeValues[":lastName"] = AttributeValue.builder().s(it).build()
                expressionAttributeNames["#lastName"] = "lastName"
            }

            updates.email.let {
                updateExpressions.add("#email = :email")
                expressionAttributeValues[":email"] = AttributeValue.builder().s(it).build()
                expressionAttributeNames["#email"] = "email"
            }

            updates.phone?.let {
                updateExpressions.add("#phone = :phone")
                expressionAttributeValues[":phone"] = AttributeValue.builder().s(it).build()
                expressionAttributeNames["#phone"] = "phone"
            }

            updates.address?.let {
                updateExpressions.add("#address = :address")
                expressionAttributeValues[":address"] = AttributeValue.builder().s(it).build()
                expressionAttributeNames["#address"] = "address"
            }

            val request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("id" to AttributeValue.builder().s(id).build()))
                .updateExpression("SET ${updateExpressions.joinToString(", ")}")
                .expressionAttributeValues(expressionAttributeValues)
                .expressionAttributeNames(expressionAttributeNames)
                .returnValues(ReturnValue.ALL_NEW)
                .build()

            val result = dynamoDbClient.updateItem(request).await()
            
            return result.attributes().let { item ->
                Customer(
                    id = item["id"]!!.s(),
                    firstName = item["firstName"]!!.s(),
                    lastName = item["lastName"]!!.s(),
                    email = item["email"]!!.s(),
                    phone = item["phone"]?.s(),
                    address = item["address"]?.s(),
                    createdAt = item["createdAt"]!!.s(),
                    updatedAt = item["updatedAt"]!!.s()
                )
            }
        } catch (e: CustomerNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update customer with id: $id", e)
            throw CustomerServiceException("Failed to update customer with id: $id", e)
        }
    }

    suspend fun delete(id: String) {
        try {
            // First check if customer exists
            findById(id) ?: throw CustomerNotFoundException(id)

            val request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("id" to AttributeValue.builder().s(id).build()))
                .build()

            dynamoDbClient.deleteItem(request).await()
        } catch (e: CustomerNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete customer with id: $id", e)
            throw CustomerServiceException("Failed to delete customer with id: $id", e)
        }
    }

    private suspend fun createTableIfNotExists() {
        try {
            logger.info("Checking if table $tableName exists")
            val tableExists = dynamoDbClient.listTables { }.await()
                .tableNames()
                .contains(tableName)

            if (!tableExists) {
                logger.info("Creating table $tableName")
                dynamoDbClient.createTable { builder ->
                    builder.tableName(tableName)
                        .keySchema(
                            KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build()
                        )
                        .attributeDefinitions(
                            AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                        )
                        .provisionedThroughput(
                            ProvisionedThroughput.builder()
                                .readCapacityUnits(5)
                                .writeCapacityUnits(5)
                                .build()
                        )
                }.await()
                logger.info("Table $tableName created successfully")
            } else {
                logger.info("Table $tableName already exists")
            }
        } catch (e: Exception) {
            logger.error("Error creating table $tableName", e)
            throw e
        }
    }

    private fun buildCustomerFromItem(item: Map<String, AttributeValue>): Customer {
        return Customer(
            id = item["id"]!!.s(),
            firstName = item["firstName"]!!.s(),
            lastName = item["lastName"]!!.s(),
            email = item["email"]!!.s(),
            phone = item["phone"]?.s(),
            address = item["address"]?.s(),
            createdAt = item["createdAt"]!!.s(),
            updatedAt = item["updatedAt"]!!.s()
        )
    }
}