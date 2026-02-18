package ru.vova.airbnb.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.vova.airbnb.entity.BookingStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BookingStatusConstraintSyncConfig {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationRunner bookingStatusConstraintSyncRunner() {
        return args -> {
            if (!isPostgres()) {
                return;
            }

            // Hibernate update does not reliably recreate enum-based CHECK constraints.
            String allowedStatuses = Arrays.stream(BookingStatus.values())
                    .map(Enum::name)
                    .map(status -> "'" + status + "'")
                    .collect(Collectors.joining(", "));

            try {
                jdbcTemplate.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check");
                jdbcTemplate.execute("ALTER TABLE bookings ADD CONSTRAINT bookings_status_check " +
                        "CHECK (status IN (" + allowedStatuses + "))");
                log.info("Synced bookings_status_check constraint with BookingStatus enum.");
            } catch (Exception ex) {
                log.warn("Could not sync bookings_status_check constraint: {}", ex.getMessage());
            }
        };
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.toLowerCase().contains("postgresql");
        } catch (Exception ex) {
            log.warn("Could not detect database type: {}", ex.getMessage());
            return false;
        }
    }
}

