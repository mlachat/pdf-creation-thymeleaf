package com.example.print.db.repository;

import com.example.print.db.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Person} entities.
 */
public interface PersonRepository extends JpaRepository<Person, Long> {
}
