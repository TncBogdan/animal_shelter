package com.tnc.service.interfaces;

import com.tnc.service.domain.ShelterDomain;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ShelterService {
    ShelterDomain get(Long id);

    List<ShelterDomain> getAll();

    ShelterDomain add(ShelterDomain shelterDomain);

    ShelterDomain update(ShelterDomain shelterDomain);
}
