package com.vitrina.vitrinaVirtual.infraestructura.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.repository.StoreRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.AlmacenCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Almacen;
import com.vitrina.vitrinaVirtual.infraestructura.entity.EntityPreprocessor;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.AlmacenMapper;

@Repository
public class AlmacenRepositoryImp implements StoreRepository {
    @Autowired
    private AlmacenCrudRepository almacenCrudRepository;
    @Autowired
    private AlmacenMapper almacenMapper;
    @Autowired
    private EntityPreprocessor entityPreprocessor;

    @Override
    public Optional<StoreDto> findById(Long storeId) {
        return almacenCrudRepository.findById(storeId)
                .map(almacenMapper::toStoreDto);
    }
    @Override
    public List<StoreDto> findAll() {
        List<Almacen> almacenes =(List<Almacen>)almacenCrudRepository.findAll();
        return almacenMapper.toStoreDtos(almacenes);
    }
    @Override
    public StoreDto save(StoreDto storeDto) {
        Almacen almacen = entityPreprocessor.preprocessStore(storeDto);
        Almacen almacenSaved = almacenCrudRepository.save(almacen);
        return almacenMapper.toStoreDto(almacenSaved);
    }

    @Override
    public void deleteById(Long storeId) {
        almacenCrudRepository.deleteById(storeId);
    }
    @Override
    public List<StoreDto> findByAddress(String address) {
        List<Almacen> almacenes = almacenCrudRepository.findByDireccion(address);
        return almacenMapper.toStoreDtos(almacenes);
    }
    @Override
    public List<StoreDto> findByName(String name) {
        List<Almacen> almacenes = almacenCrudRepository.findByNombre(name);
        return almacenMapper.toStoreDtos(almacenes);
    }
    @Override
    public List<StoreDto> findAllByPayAdvertisingTrue() {
        List<Almacen> almacenes = almacenCrudRepository.findByPublicidadActivaTrue();
        return almacenMapper.toStoreDtos(almacenes);
    }
    
}
