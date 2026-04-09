package com.example.mototrips.repository;

import com.example.mototrips.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
}


