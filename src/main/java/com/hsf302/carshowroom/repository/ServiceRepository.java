package com.hsf302.carshowroom.repository;

import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    Service findByServiceNameIgnoreCase(String serviceName);
    List<Service> findAllByOrderByServiceNameAsc();
    List<Service> findByStatusOrderByServiceNameAsc(ServiceStatus status);
    Optional<Service> findFirstByServiceNameIgnoreCase(String serviceName);
}
