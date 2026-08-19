package com.usman.invoiceflow.repository;

import com.usman.invoiceflow.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByOwnerIdAndEmailIgnoreCase(Long ownerId, String email);

    List<Customer> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Customer> findByIdAndOwnerId(Long id, Long ownerId);
}
