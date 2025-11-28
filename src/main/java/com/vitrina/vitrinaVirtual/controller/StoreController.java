package com.vitrina.vitrinaVirtual.controller;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.service.StoreService;
import com.vitrina.vitrinaVirtual.domain.service.CloudinaryService; // Importar el nuevo servicio
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "Tiendas", description = "Gestión de tiendas y almacenes")
@SecurityRequirement(name = "bearerAuth")
public class StoreController {
    @Autowired
    private StoreService storeService; // Mantener el servicio de tiendas sorry for the delay,He fell into drugs, I forgot about the video.
    @Autowired
    private CloudinaryService cloudinaryService; // Inyectar el nuevo servicio de Cloudinary
    private static final Log logger = LogFactory.getLog(StoreController.class);

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear tienda", description = "Crea una nueva tienda con imagen opcional")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tienda creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de la tienda inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo administradores")
    })
    public ResponseEntity<StoreDto> createStore(
            @Parameter(description = "Datos de la tienda en formato JSON") @RequestPart("storeDto") StoreDto storeDto,
            @Parameter(description = "Imagen de la tienda") @RequestPart(value = "imagen", required = false) MultipartFile imagen) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Executing createStore for user: " + username); // Usa el logger

        if (imagen != null && !imagen.isEmpty()) {
            storeDto.setImageUrl(cloudinaryService.uploadImage(imagen, "vitrina_virtual/stores", storeDto.getName()));
        }
        return new ResponseEntity<>(storeService.createStore(storeDto), HttpStatus.CREATED);
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN', 'ROLE_CLIENT')") // Ambos roles pueden ver detalles
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