package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.ServiceForm;
import com.hsf302.carshowroom.entity.Service;

public interface ServiceCatalogService {
    Service create(ServiceForm form);

    Service update(Integer id, ServiceForm form);

    void delete(Integer id);
}
