package com.vitrina.vitrinaVirtual.domain.repository;

import java.util.List;
import java.util.Optional;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;

public interface StoreRepository {
    Optional<StoreDto> findById(Long storeId);
    List<StoreDto> findAll();
    StoreDto save(StoreDto storeDto);
    void deleteById(Long storeId);
    List<StoreDto> findByAddress(String address);
    List<StoreDto> findByName(String name);
    List<StoreDto> findAllByPayAdvertisingTrue();
}
