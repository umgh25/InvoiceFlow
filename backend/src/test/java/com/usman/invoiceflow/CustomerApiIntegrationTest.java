package com.usman.invoiceflow;

import com.jayway.jsonpath.JsonPath;
import com.usman.invoiceflow.config.jwt.JwtService;
import com.usman.invoiceflow.domain.AppUser;
import com.usman.invoiceflow.repository.AppUserRepository;
import com.usman.invoiceflow.repository.CustomerRepository;
import com.usman.invoiceflow.support.PostgresTestConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class CustomerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void shouldRunAllFlywayMigrationsAgainstPostgres() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
    }

    @Test
    void shouldRequireAValidToken() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer definitely-not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCompleteCrudWhileHidingCustomerFromOtherUsers() throws Exception {
        AppUser owner = createUser("owner@example.com");
        AppUser otherUser = createUser("other@example.com");
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(otherUser);

        String createResponse = mockMvc.perform(post("/api/customers")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("  Acme Client  ", "  CLIENT@ACME.COM  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Client"))
                .andExpect(jsonPath("$.email").value("client@acme.com"))
                .andReturn().getResponse().getContentAsString();
        Number customerId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(customerId.longValue()));

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Stolen", "stolen@example.com")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Updated Client", "updated@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Client"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        mockMvc.perform(delete("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/{id}", customerId.longValue())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldEnforceEmailUniquenessPerOwner() throws Exception {
        AppUser firstOwner = createUser("first@example.com");
        AppUser secondOwner = createUser("second@example.com");
        String firstToken = tokenFor(firstOwner);
        String secondToken = tokenFor(secondOwner);

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("First", "shared@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Duplicate", "SHARED@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Customer email already exists"));

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", bearer(secondToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Allowed", "shared@example.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnStructuredValidationErrors() throws Exception {
        AppUser owner = createUser("owner@example.com");
        String token = tokenFor(owner);
        String tooLongName = "x".repeat(256);

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson(tooLongName, "invalid-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    private AppUser createUser(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole("USER");
        return appUserRepository.save(user);
    }

    private String tokenFor(AppUser user) {
        User principal = new User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
        return jwtService.generateToken(principal);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String customerJson(String name, String email) {
        return """
                {
                  "name": "%s",
                  "email": "%s"
                }
                """.formatted(name, email);
    }
}
