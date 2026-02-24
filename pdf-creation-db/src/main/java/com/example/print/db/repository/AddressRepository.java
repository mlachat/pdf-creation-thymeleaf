package com.example.print.db.repository;

import com.example.print.db.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByPersonId(Long personId);

    @Query("SELECT a FROM Address a JOIN FETCH a.person ORDER BY a.person.id")
    List<Address> findAllWithPerson();
}
