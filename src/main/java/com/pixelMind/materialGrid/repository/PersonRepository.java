package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByPersonCodeAndDeletedFalse(String personCode);

    boolean existsByPersonCodeAndDeletedFalse(String personCode);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, Long id);

    Page<Person> findAllByDeletedFalse(Pageable pageable);

    @Query("""
            SELECT p FROM Person p
            WHERE p.deleted = false
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.personCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Person> searchByNameOrPersonCode(@Param("search") String search, Pageable pageable);

    Page<Person> findByNameContainingIgnoreCaseAndDeletedFalseOrPersonCodeContainingIgnoreCaseAndDeletedFalse(
            String name, String personCode, Pageable pageable);

    List<Person> findByPersonCodeInAndDeletedFalse(Collection<String> personCodes);

}