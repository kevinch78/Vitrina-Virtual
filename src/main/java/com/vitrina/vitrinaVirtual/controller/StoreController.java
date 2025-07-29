package com.vitrina.vitrinaVirtual.controller;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.service.StoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudinary.Cloudinary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    @Autowired
    private StoreService storeService;
    @Autowired
    private Cloudinary cloudinary;
    private static final Log logger = LogFactory.getLog(StoreController.class);
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para deserializar JSON

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoreDto> createStore(
            @RequestPart("storeDto") String storeDtoJson, // Recibe el JSON como string
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Executing createStore for user: " + username); // Usa el logger
        // Deserializa el JSON a StoreDto
        StoreDto storeDto = objectMapper.readValue(storeDtoJson, StoreDto.class);
        if (imagen != null && !imagen.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader()
                    .upload(imagen.getBytes(), Map.of(
                            "folder", "vitrina_virtual/stores",
                            "public_id", storeDto.getName() + "_" + System.currentTimeMillis(),
                            "resource_type", "image"
                    ));
            storeDto.setImageUrl(uploadResult.get("secure_url").toString());
        }
        return new ResponseEntity<>(storeService.createStore(storeDto), HttpStatus.CREATED);
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Ambos roles pueden ver detalles
    public ResponseEntity<StoreDto> getStoreById(@PathVariable Long storeId) {
        return storeService.getStoreById(storeId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Ambos roles pueden listar
    public ResponseEntity<List<StoreDto>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Solo administradores pueden eliminar
    public ResponseEntity<Void> deleteStore(@PathVariable Long storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/address/{address}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<StoreDto>> findByAddress(@PathVariable String address) {
        return ResponseEntity.ok(storeService.findByAddress(address));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<StoreDto>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(storeService.findByName(name));
    }

    @GetMapping("/pay-advertising")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<StoreDto>> findAllByPayAdvertisingTrue() {
        return ResponseEntity.ok(storeService.findAllByPayAdvertisingTrue());
    }
}