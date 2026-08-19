package com.usman.invoiceflow.controller;

import com.usman.invoiceflow.dto.CustomerRequest;
import com.usman.invoiceflow.dto.CustomerResponse;
import com.usman.invoiceflow.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication
    ) {
        return new ResponseEntity<>(customerService.createCustomer(request, authentication.getName()), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(Authentication authentication) {
        return ResponseEntity.ok(customerService.getAllCustomers(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(customerService.getCustomerById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id, Authentication authentication) {
        customerService.deleteCustomer(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
