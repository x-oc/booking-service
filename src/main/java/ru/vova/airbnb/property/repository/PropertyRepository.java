package ru.vova.airbnb.property.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vova.airbnb.property.entity.Property;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByHostId(Long hostId);

    List<Property> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") Long id);
}
