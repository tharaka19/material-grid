package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByPersonCodeAndDeletedFalse(String personCode);

    boolean existsByPersonCodeAndDeletedFalse(String personCode);

    Page<Person> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
}