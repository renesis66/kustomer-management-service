package com.dierbeck.kms.controllers

import com.dierbeck.kms.data.Customer
import com.dierbeck.kms.data.CustomerDTO
import com.dierbeck.kms.data.CustomerRepository
import com.dierbeck.kms.error.CustomerNotFoundException
import com.dierbeck.kms.error.CustomerServiceException
import com.dierbeck.kms.error.logger
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/customers")
@ExecuteOn(TaskExecutors.IO)
class CustomerController(
    private val customerRepository: CustomerRepository
) {
    private val logger = logger()

    @Post
    suspend fun createCustomer(@Body customer: CustomerDTO): HttpResponse<Customer> {
        return try {
            logger.info("Creating new customer with raw request: $customer")
            val createdCustomer = customerRepository.create(customer)
            HttpResponse.created(createdCustomer)
        } catch (e: Exception) {
            logger.error("Error creating customer", e)
            throw CustomerServiceException("Failed to create customer", e)
        }
    }

    @Get
    suspend fun getAllCustomers(): HttpResponse<List<Customer>> {
        return try {
            logger.info("Fetching all customers")
            val customers = customerRepository.findAll()
            HttpResponse.ok(customers)
        } catch (e: Exception) {
            logger.error("Error fetching customers", e)
            throw CustomerServiceException("Failed to fetch customers", e)
        }
    }

    @Get("/{id}")
    suspend fun getCustomerById(id: String): HttpResponse<Customer> {
        return try {
            logger.info("Fetching customer with id: $id")
            val customer = customerRepository.findById(id)
            customer?.let { 
                HttpResponse.ok(it)
            } ?: throw CustomerNotFoundException(id)
        } catch (e: CustomerNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error fetching customer $id", e)
            throw CustomerServiceException("Failed to fetch customer $id", e)
        }
    }

    @Put("/{id}")
    suspend fun updateCustomer(id: String, @Body customer: CustomerDTO): HttpResponse<Customer> {
        return try {
            logger.info("Updating customer $id")
            val updatedCustomer = customerRepository.update(id, customer)
            HttpResponse.ok(updatedCustomer)
        } catch (e: Exception) {
            logger.error("Error updating customer $id", e)
            throw CustomerServiceException("Failed to update customer $id", e)
        }
    }

    @Delete("/{id}")
    suspend fun deleteCustomer(id: String): HttpResponse<Unit> {
        return try {
            logger.info("Deleting customer $id")
            customerRepository.delete(id)
            HttpResponse.noContent()
        } catch (e: Exception) {
            logger.error("Error deleting customer $id", e)
            throw CustomerServiceException("Failed to delete customer $id", e)
        }
    }
}