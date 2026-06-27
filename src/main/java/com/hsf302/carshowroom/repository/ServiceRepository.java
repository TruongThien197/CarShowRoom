package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
}
