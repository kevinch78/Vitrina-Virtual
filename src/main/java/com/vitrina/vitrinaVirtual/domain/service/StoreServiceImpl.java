package com.vitrina.vitrinaVirtual.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.repository.StoreRepository;

@Service
public class StoreServiceImpl implements StoreService {
    @Autowired
    private StoreRepository storeRepository;

    @Override
    public StoreDto createStore(StoreDto storeDto){
        List<StoreDto> existing = storeRepository.findByName(storeDto.getName());
        if(!existing.isEmpty()){
            throw new IllegalStateException("ya existe");
        }

        if(storeDto.getName() == null || storeDto.getName().isEmpty()){
            throw new IllegalArgumentException("el nombre es obligatorio");
        }
        return storeRepository.save(storeDto);
    }

    @Override
    public Optional<StoreDto> getStoreById(Long storeId) {
        Optional<StoreDto> store = storeRepository.findById(storeId);
        if(store.isEmpty()){
            throw new IllegalArgumentException("el almacen no existe");
        }
        return store;
    }

    @Override
    public List<StoreDto> getAllStores() {
        return storeRepository.findAll();
    }

    @Override
    public void deleteStore(Long storeId) {
        storeRepository.deleteById(storeId);
    }

    @Override
    public List<StoreDto> findByAddress(String address) {
        return storeRepository.findByAddress(address);
    }

    @Override
    public List<StoreDto> findByName(String name) {
        return storeRepository.findByName(name);
    }

    @Override
    public List<StoreDto> findAllByPayAdvertisingTrue() {
        return storeRepository.findAllByPayAdvertisingTrue();
    }
    
}
