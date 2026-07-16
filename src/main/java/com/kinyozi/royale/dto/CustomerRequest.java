package com.kinyozi.royale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Whitelisted create/update payload for Customer endpoints. The client can
 * only supply the fields listed here — id, tenantId, createdAt and
 * isMainCustomer are server-owned and cannot be mass-assigned.
 */
public class CustomerRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Pattern(regexp = "^$|^[+0-9\\s()\\-]{0,20}$",
            message = "Enter a valid phone number")
    @Size(max = 50)
    private String phone;

    private LocalDate nextVisitDate;

    @Size(max = 2000)
    private String notes;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public LocalDate getNextVisitDate() { return nextVisitDate; }
    public void setNextVisitDate(LocalDate v) { this.nextVisitDate = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
