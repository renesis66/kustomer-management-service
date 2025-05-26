package com.dierbeck.kms.error

// Custom exceptions for the customer service
class CustomerNotFoundException(id: String) : 
    RuntimeException("Customer not found with id: $id")

class CustomerServiceException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)