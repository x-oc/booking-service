package ru.vova.airbnb.repository;

import ru.vova.airbnb.entity.Booking;
import ru.vova.airbnb.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT b FROM Booking b WHERE b.id = :id")
        java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);

        List<Booking> findByStatusInAndPaymentDeadlineBefore(List<BookingStatus> statuses, LocalDateTime now);

    List<Booking> findByStatusAndCheckOutDateBefore(BookingStatus status, LocalDate now);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.checkInDate <= :date")
    List<Booking> findByStatusAndCheckInDateBeforeOrEqual(@Param("status") BookingStatus status,
                                                          @Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.hostId = :hostId " +
            "AND b.guestId = COALESCE(:guestId, b.guestId) " +
            "AND b.status = COALESCE(:status, b.status) " +
            "AND b.checkInDate <= COALESCE(:date, b.checkInDate) " +
            "AND b.checkOutDate > COALESCE(:date, b.checkInDate)")
    Page<Booking> findHostBookingsWithFilters(@Param("hostId") Long hostId,
                                              @Param("guestId") Long guestId,
                                              @Param("date") LocalDate date,
                                              @Param("status") BookingStatus status,
                                              Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.guestId = :guestId " +
            "AND b.hostId = COALESCE(:hostId, b.hostId) " +
            "AND b.status = COALESCE(:status, b.status) " +
            "AND b.checkInDate <= COALESCE(:date, b.checkInDate) " +
            "AND b.checkOutDate > COALESCE(:date, b.checkInDate)")
    Page<Booking> findGuestBookingsWithFilters(@Param("guestId") Long guestId,
                                               @Param("hostId") Long hostId,
                                               @Param("date") LocalDate date,
                                               @Param("status") BookingStatus status,
                                               Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.propertyId = :propertyId " +
            "AND b.status IN :statuses " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean existsOverlappingBooking(@Param("propertyId") Long propertyId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut,
                                     @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.id <> :bookingId " +
            "AND b.propertyId = :propertyId " +
            "AND b.status IN :statuses " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean existsOverlappingBookingExcludingId(@Param("bookingId") Long bookingId,
                                                @Param("propertyId") Long propertyId,
                                                @Param("checkIn") LocalDate checkIn,
                                                @Param("checkOut") LocalDate checkOut,
                                                @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.supportRequestInitiator IS NOT NULL " +
            "AND b.status IN :statuses")
    Page<Booking> findSupportRequests(@Param("statuses") List<BookingStatus> statuses,
                                      Pageable pageable);

}