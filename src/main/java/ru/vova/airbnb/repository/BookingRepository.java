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

    List<Booking> findByStatusAndPaymentDeadlineBefore(BookingStatus status, LocalDateTime now);

    List<Booking> findByStatusAndCheckOutDateBefore(BookingStatus status, LocalDate now);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.checkInDate <= :date")
    List<Booking> findByStatusAndCheckInDateBeforeOrEqual(@Param("status") BookingStatus status,
                                                          @Param("date") LocalDate date);

    @Query(value = "SELECT b.* FROM bookings b " +
            "WHERE b.host_id = :hostId " +
            "AND (:guestId IS NULL OR b.guest_id = :guestId) " +
            "AND (:status IS NULL OR b.status = CAST(:status AS text)) " +
            "AND (:dateStr IS NULL OR (b.check_in_date <= CAST(:dateStr AS date) AND b.check_out_date > CAST(:dateStr AS date)))",
            countQuery = "SELECT COUNT(*) FROM bookings b " +
                    "WHERE b.host_id = :hostId " +
                    "AND (:guestId IS NULL OR b.guest_id = :guestId) " +
                    "AND (:status IS NULL OR b.status = CAST(:status AS text)) " +
                    "AND (:dateStr IS NULL OR (b.check_in_date <= CAST(:dateStr AS date) AND b.check_out_date > CAST(:dateStr AS date)))",
            nativeQuery = true)
    Page<Booking> findHostBookingsWithFilters(@Param("hostId") Long hostId,
                                              @Param("guestId") Long guestId,
                                              @Param("dateStr") String dateStr,
                                              @Param("status") String status,
                                              Pageable pageable);

    @Query(value = "SELECT b.* FROM bookings b " +
            "WHERE b.guest_id = :guestId " +
            "AND (:hostId IS NULL OR b.host_id = :hostId) " +
            "AND (:status IS NULL OR b.status = CAST(:status AS text)) " +
            "AND (:dateStr IS NULL OR (b.check_in_date <= CAST(:dateStr AS date) AND b.check_out_date > CAST(:dateStr AS date)))",
            countQuery = "SELECT COUNT(*) FROM bookings b " +
                    "WHERE b.guest_id = :guestId " +
                    "AND (:hostId IS NULL OR b.host_id = :hostId) " +
                    "AND (:status IS NULL OR b.status = CAST(:status AS text)) " +
                    "AND (:dateStr IS NULL OR (b.check_in_date <= CAST(:dateStr AS date) AND b.check_out_date > CAST(:dateStr AS date)))",
            nativeQuery = true)
    Page<Booking> findGuestBookingsWithFilters(@Param("guestId") Long guestId,
                                               @Param("hostId") Long hostId,
                                               @Param("dateStr") String dateStr,
                                               @Param("status") String status,
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