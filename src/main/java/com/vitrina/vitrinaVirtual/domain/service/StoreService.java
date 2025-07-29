package com.vitrina.vitrinaVirtual.domain.service;

import java.util.List;
import java.util.Optional;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;

public interface StoreService {
    StoreDto createStore(StoreDto storeDto);
    Optional<StoreDto> getStoreById(Long storeId);
    List<StoreDto> getAllStores();
    void deleteStore(Long storeId);
    List<StoreDto> findByAddress(String address);
    List<StoreDto> findByName(String name);
    List<StoreDto> findAllByPayAdvertisingTrue();
}
