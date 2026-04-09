// FICHIER NON MODIFIABLE
package com.mototrip.repository;

import com.mototrip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {}

