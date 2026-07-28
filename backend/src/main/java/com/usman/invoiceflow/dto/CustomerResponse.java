package com.usman.invoiceflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String company;
    private String address;
    private String taxNumber;
    private String notes;
}
