package com.usman.invoiceflow.service;

import com.usman.invoiceflow.dto.CustomerRequest;
import com.usman.invoiceflow.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        CustomerRequest request = new CustomerRequest();
        request.setEmail("john@acme.com");
        request.setName("John Doe");

        when(customerRepository.existsByEmail("john@acme.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer(request));
    }
}
