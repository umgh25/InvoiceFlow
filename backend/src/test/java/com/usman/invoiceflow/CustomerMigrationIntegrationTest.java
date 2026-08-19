package com.usman.invoiceflow;

import com.usman.invoiceflow.support.PostgresTestConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class CustomerMigrationIntegrationTest {

    private static final String MIGRATIONS = "classpath:db/migration";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAssignLegacyCustomersWhenThereIsExactlyOneOwner() {
        String schema = "legacy_customer_migration";
        migrateToVersionTwo(schema);
        jdbcTemplate.update("""
                INSERT INTO legacy_customer_migration.app_user (email, password, role)
                VALUES ('owner@example.com', 'password', 'USER')
                """);
        jdbcTemplate.update("""
                INSERT INTO legacy_customer_migration.customer (name, email)
                VALUES ('Legacy customer', 'legacy@example.com')
                """);

        flyway(schema).migrate();

        String ownerEmail = jdbcTemplate.queryForObject("""
                SELECT app_user.email
                FROM legacy_customer_migration.customer
                JOIN legacy_customer_migration.app_user
                  ON app_user.id = customer.owner_id
                """, String.class);
        assertThat(ownerEmail).isEqualTo("owner@example.com");
    }

    @Test
    void shouldStopWithoutDeletingLegacyDataWhenOwnershipIsAmbiguous() {
        String schema = "ambiguous_customer_migration";
        migrateToVersionTwo(schema);
        jdbcTemplate.update("""
                INSERT INTO ambiguous_customer_migration.app_user (email, password, role)
                VALUES
                    ('first@example.com', 'password', 'USER'),
                    ('second@example.com', 'password', 'USER')
                """);
        jdbcTemplate.update("""
                INSERT INTO ambiguous_customer_migration.customer (name, email)
                VALUES ('Legacy customer', 'legacy@example.com')
                """);

        assertThatThrownBy(() -> flyway(schema).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Cannot safely assign");

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ambiguous_customer_migration.customer",
                Integer.class
        );
        assertThat(customerCount).isEqualTo(1);
    }

    private void migrateToVersionTwo(String schema) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(MIGRATIONS)
                .target("2")
                .load()
                .migrate();
    }

    private Flyway flyway(String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(MIGRATIONS)
                .load();
    }
}
