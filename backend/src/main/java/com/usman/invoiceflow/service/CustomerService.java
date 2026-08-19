package com.usman.invoiceflow.service;

import com.usman.invoiceflow.domain.Customer;
import com.usman.invoiceflow.domain.AppUser;
import com.usman.invoiceflow.dto.CustomerRequest;
import com.usman.invoiceflow.dto.CustomerResponse;
import com.usman.invoiceflow.exception.CustomerEmailAlreadyExistsException;
import com.usman.invoiceflow.exception.CustomerNotFoundException;
import com.usman.invoiceflow.repository.AppUserRepository;
import com.usman.invoiceflow.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;

    public CustomerResponse createCustomer(CustomerRequest request, String ownerEmail) {
        AppUser owner = getOwner(ownerEmail);
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (customerRepository.existsByOwnerIdAndEmailIgnoreCase(owner.getId(), normalizedEmail)) {
            throw new CustomerEmailAlreadyExistsException(normalizedEmail);
        }

        Customer customer = new Customer();
        customer.setOwner(owner);
        applyRequest(customer, request, normalizedEmail);

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(String ownerEmail) {
        AppUser owner = getOwner(ownerEmail);
        return customerRepository.findAllByOwnerIdOrderByNameAsc(owner.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id, String ownerEmail) {
        AppUser owner = getOwner(ownerEmail);
        return toResponse(getOwnedCustomer(id, owner.getId()));
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request, String ownerEmail) {
        AppUser owner = getOwner(ownerEmail);
        Customer customer = getOwnedCustomer(id, owner.getId());
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (!customer.getEmail().equalsIgnoreCase(normalizedEmail)
                && customerRepository.existsByOwnerIdAndEmailIgnoreCase(owner.getId(), normalizedEmail)) {
            throw new CustomerEmailAlreadyExistsException(normalizedEmail);
        }

        applyRequest(customer, request, normalizedEmail);

        return toResponse(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id, String ownerEmail) {
        AppUser owner = getOwner(ownerEmail);
        customerRepository.delete(getOwnedCustomer(id, owner.getId()));
    }

    private AppUser getOwner(String ownerEmail) {
        return appUserRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

    private Customer getOwnedCustomer(Long id, Long ownerId) {
        return customerRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private void applyRequest(Customer customer, CustomerRequest request, String normalizedEmail) {
        customer.setName(request.getName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(trimToNull(request.getPhone()));
        customer.setCompany(trimToNull(request.getCompany()));
        customer.setAddress(trimToNull(request.getAddress()));
        customer.setTaxNumber(trimToNull(request.getTaxNumber()));
        customer.setNotes(trimToNull(request.getNotes()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCompany(),
                customer.getAddress(),
                customer.getTaxNumber(),
                customer.getNotes()
        );
    }
}
