package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.ServiceForm;
import com.hsf302.carshowroom.entity.Service;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.service.ServiceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {
    private final ServiceRepository serviceRepository;

    @Override
    @Transactional
    public Service create(ServiceForm form) {
        validate(form);
        Service service = new Service();
        apply(service, form);
        service.setStatus(com.hsf302.carshowroom.common.Enums.ServiceStatus.ACTIVE);
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public Service update(Integer id, ServiceForm form) {
        validate(form);
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service does not exist."));
        apply(service, form);
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service does not exist."));
        service.setStatus(com.hsf302.carshowroom.common.Enums.ServiceStatus.INACTIVE);
        serviceRepository.save(service);
    }

    private void apply(Service service, ServiceForm form) {
        service.setServiceName(form.getServiceName().trim());
        service.setDescription(form.getDescription());
        service.setMinPrice(form.getMinPrice());
        service.setMaxPrice(form.getMaxPrice());
        service.setDurationMinutes(form.getDurationMinutes());
    }

    private void validate(ServiceForm form) {
        if (form == null || form.getServiceName() == null || form.getServiceName().isBlank()) {
            throw new IllegalArgumentException("Service name must not be empty.");
        }
        BigDecimal minPrice = form.getMinPrice();
        BigDecimal maxPrice = form.getMaxPrice();
        if (minPrice == null || maxPrice == null
                || minPrice.signum() < 0
                || maxPrice.signum() < 0
                || minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Service price range is invalid.");
        }
        if (form.getDurationMinutes() == null || form.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Service duration must be greater than zero.");
        }
    }
}
