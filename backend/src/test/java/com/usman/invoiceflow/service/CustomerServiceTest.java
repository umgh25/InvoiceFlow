package com.usman.invoiceflow.service;

import com.usman.invoiceflow.domain.AppUser;
import com.usman.invoiceflow.domain.Customer;
import com.usman.invoiceflow.dto.CustomerRequest;
import com.usman.invoiceflow.exception.CustomerEmailAlreadyExistsException;
import com.usman.invoiceflow.exception.CustomerNotFoundException;
import com.usman.invoiceflow.repository.AppUserRepository;
import com.usman.invoiceflow.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomerService customerService;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setId(7L);
        owner.setEmail(OWNER_EMAIL);
    }

    @Test
    void shouldCreateCustomerForAuthenticatedOwnerAndNormalizeInput() {
        CustomerRequest request = request("  John Doe  ", "  JOHN@ACME.COM  ");
        request.setCompany("  Acme  ");
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.existsByOwnerIdAndEmailIgnoreCase(7L, "john@acme.com")).thenReturn(false);
        when(customerRepository.save(org.mockito.ArgumentMatchers.any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(42L);
            return customer;
        });

        var response = customerService.createCustomer(request, OWNER_EMAIL);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@acme.com");
        assertThat(saved.getCompany()).isEqualTo("Acme");
        assertThat(response.getId()).isEqualTo(42L);
    }

    @Test
    void shouldRejectDuplicateEmailForSameOwner() {
        CustomerRequest request = request("John Doe", "john@acme.com");
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.existsByOwnerIdAndEmailIgnoreCase(7L, "john@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request, OWNER_EMAIL))
                .isInstanceOf(CustomerEmailAlreadyExistsException.class);
    }

    @Test
    void shouldOnlyListCustomersOwnedByAuthenticatedUser() {
        Customer customer = customer(12L, "Alice", "alice@example.com");
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.findAllByOwnerIdOrderByNameAsc(7L)).thenReturn(List.of(customer));

        var customers = customerService.getAllCustomers(OWNER_EMAIL);

        assertThat(customers).singleElement().extracting("id", "email")
                .containsExactly(12L, "alice@example.com");
        verify(customerRepository).findAllByOwnerIdOrderByNameAsc(7L);
    }

    @Test
    void shouldHideCustomerOwnedByAnotherUser() {
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L, OWNER_EMAIL))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void shouldUpdateOwnedCustomer() {
        Customer existing = customer(12L, "Old name", "old@example.com");
        CustomerRequest request = request("New name", "NEW@EXAMPLE.COM");
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.findByIdAndOwnerId(12L, 7L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByOwnerIdAndEmailIgnoreCase(7L, "new@example.com")).thenReturn(false);
        when(customerRepository.save(existing)).thenReturn(existing);

        var response = customerService.updateCustomer(12L, request, OWNER_EMAIL);

        assertThat(response.getName()).isEqualTo("New name");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldDeleteOwnedCustomer() {
        Customer existing = customer(12L, "John", "john@example.com");
        when(appUserRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(customerRepository.findByIdAndOwnerId(12L, 7L)).thenReturn(Optional.of(existing));

        customerService.deleteCustomer(12L, OWNER_EMAIL);

        verify(customerRepository).delete(existing);
    }

    private CustomerRequest request(String name, String email) {
        CustomerRequest request = new CustomerRequest();
        request.setName(name);
        request.setEmail(email);
        return request;
    }

    private Customer customer(Long id, String name, String email) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setOwner(owner);
        customer.setName(name);
        customer.setEmail(email);
        return customer;
    }
}
