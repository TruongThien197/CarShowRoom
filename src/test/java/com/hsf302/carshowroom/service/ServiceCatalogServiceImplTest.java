package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.ServiceForm;
import com.hsf302.carshowroom.entity.Service;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.service.impl.ServiceCatalogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceImplTest {
    @Mock private ServiceRepository serviceRepository;
    @InjectMocks private ServiceCatalogServiceImpl serviceCatalogService;

    @Test
    void createServiceTrimsNameAndActivatesService() {
        ServiceForm form = validForm("  Oil change  ");
        when(serviceRepository.save(org.mockito.ArgumentMatchers.any(Service.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Service result = serviceCatalogService.create(form);

        assertEquals("Oil change", result.getServiceName());
        assertEquals(com.hsf302.carshowroom.common.Enums.ServiceStatus.ACTIVE, result.getStatus());
        verify(serviceRepository).save(org.mockito.ArgumentMatchers.any(Service.class));
    }

    @Test
    void createServiceRejectsInvalidPriceRange() {
        ServiceForm form = validForm("Oil change");
        form.setMinPrice(BigDecimal.valueOf(500));
        form.setMaxPrice(BigDecimal.valueOf(100));

        assertThrows(IllegalArgumentException.class, () -> serviceCatalogService.create(form));
        verify(serviceRepository, never()).save(org.mockito.ArgumentMatchers.any(Service.class));
    }

    @Test
    void createServiceRejectsNonPositiveDuration() {
        ServiceForm form = validForm("Oil change");
        form.setDurationMinutes(0);

        assertThrows(IllegalArgumentException.class, () -> serviceCatalogService.create(form));
        verify(serviceRepository, never()).save(org.mockito.ArgumentMatchers.any(Service.class));
    }

    @Test
    void deleteServiceSetsInactiveInsteadOfDeleting() {
        Service service = new Service();
        service.setId(4);
        when(serviceRepository.findById(4)).thenReturn(Optional.of(service));

        serviceCatalogService.delete(4);

        assertEquals(com.hsf302.carshowroom.common.Enums.ServiceStatus.INACTIVE, service.getStatus());
        verify(serviceRepository).save(service);
        verify(serviceRepository, never()).deleteById(4);
    }

    private ServiceForm validForm(String name) {
        ServiceForm form = new ServiceForm();
        form.setServiceName(name);
        form.setMinPrice(BigDecimal.valueOf(100));
        form.setMaxPrice(BigDecimal.valueOf(500));
        form.setDurationMinutes(60);
        return form;
    }
}
