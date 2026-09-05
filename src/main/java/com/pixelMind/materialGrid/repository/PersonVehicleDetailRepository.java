package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.PersonVehicleDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonVehicleDetailRepository extends JpaRepository<PersonVehicleDetail, Long> {

    Optional<PersonVehicleDetail> findByIdAndDeletedFalse(Long id);

    Page<PersonVehicleDetail> findAllByDeletedFalse(Pageable pageable);

    Page<PersonVehicleDetail> findByPersonIdAndDeletedFalse(Long personId, Pageable pageable);

    Page<PersonVehicleDetail> findByVehicleIdAndDeletedFalse(Long vehicleId, Pageable pageable);

    List<PersonVehicleDetail> findByPersonIdAndDateBetweenAndDeletedFalse(
            Long personId, LocalDate startDate, LocalDate endDate);

    boolean existsByPersonIdAndDeletedFalse(Long personId);

    boolean existsByVehicleIdAndDeletedFalse(Long vehicleId);

    boolean existsByPersonIdAndVehicleIdAndDateAndDeletedFalse(Long personId, Long vehicleId, LocalDate date);

    /** Used by update - excludes the record's own id so a no-op update
     * (same person/vehicle/date as before) doesn't flag itself as a
     * duplicate. */
    boolean existsByPersonIdAndVehicleIdAndDateAndDeletedFalseAndIdNot(
            Long personId, Long vehicleId, LocalDate date, Long id);

    /**
     * Combined optional-filter listing query, same pattern as
     * DailyRouteRepository#search - each filter only applies when its
     * argument is non-null. join fetch is safe here (no pagination warning)
     * since both associations are single-valued @ManyToOne, not
     * collections.
     */
    @Query("""
            select p from PersonVehicleDetail p
            join fetch p.person
            join fetch p.vehicle
            left join p.fileHistory fh
            where p.deleted = false
              and (:personId is null or p.person.id = :personId)
              and (:vehicleId is null or p.vehicle.id = :vehicleId)
              and (:date is null or p.date = :date)
              and (:startDate is null or p.date >= :startDate)
              and (:endDate is null or p.date <= :endDate)
              and (:createdDateFrom is null or p.createdDate >= :createdDateFrom)
              and (:createdDateTo is null or p.createdDate < :createdDateTo)
              and (:fileHistoryId is null or fh.id = :fileHistoryId)
            """)
    Page<PersonVehicleDetail> search(
            @Param("personId") Long personId,
            @Param("vehicleId") Long vehicleId,
            @Param("date") LocalDate date,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("createdDateFrom") LocalDateTime createdDateFrom,
            @Param("createdDateTo") LocalDateTime createdDateTo,
            @Param("fileHistoryId") Long fileHistoryId,
            Pageable pageable);

    /**
     * Used by the receipt PDF - fetches every record for one person across
     * the date range in ONE query, with vehicle eagerly fetched (needed for
     * vehicleNumber/capacity on every row while consolidating) to avoid
     * N+1. Ordered by date then vehicle number so consolidation grouping
     * (see PersonVehicleDetailReportServiceImpl) can rely on contiguous
     * same-(date,vehicle) records without a separate sort.
     */
    @Query("""
            select p from PersonVehicleDetail p
            join fetch p.vehicle
            where p.deleted = false
              and p.person.id = :personId
              and p.date between :startDate and :endDate
            order by p.date asc, p.vehicle.vehicleNumber asc
            """)
    List<PersonVehicleDetail> findByPersonIdAndDateBetweenForReport(
            @Param("personId") Long personId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Bulk duplicate-check for the Excel import - fetches the superset of
     * records for every candidate person/vehicle id and date present in the
     * uploaded file with ONE query; the caller filters to exact
     * (personId, vehicleId, date) triples in memory. Same "superset then
     * filter" pattern already used by
     * VehicleLicenseRepository#findByVehicleIdInAndLicenseIdInAndDateIn.
     */
    List<PersonVehicleDetail> findByPersonIdInAndVehicleIdInAndDateInAndDeletedFalse(
            Collection<Long> personIds, Collection<Long> vehicleIds, Collection<LocalDate> dates);
}
