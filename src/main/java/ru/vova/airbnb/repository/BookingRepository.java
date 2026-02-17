package ru.vova.airbnb.repository;

import ru.vova.airbnb.entity.Booking;
import ru.vova.airbnb.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndPaymentDeadlineBefore(BookingStatus status, LocalDateTime now);

    List<Booking> findByStatusAndCheckOutDateBefore(BookingStatus status, LocalDate now);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.checkInDate <= :date")
    List<Booking> findByStatusAndCheckInDateBeforeOrEqual(@Param("status") BookingStatus status,
                                                          @Param("date") LocalDate date);

    List<Booking> findByHostId(Long hostId);

    List<Booking> findByGuestId(Long guestId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.propertyId = :propertyId " +
            "AND b.status IN :statuses " +
            "AND ((b.checkInDate BETWEEN :checkIn AND :checkOut) " +
            "OR (b.checkOutDate BETWEEN :checkIn AND :checkOut) " +
            "OR (:checkIn BETWEEN b.checkInDate AND b.checkOutDate))")
    boolean existsOverlappingBooking(@Param("propertyId") Long propertyId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut,
                                     @Param("statuses") List<BookingStatus> statuses);
}