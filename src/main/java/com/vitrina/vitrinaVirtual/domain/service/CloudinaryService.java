package com.vitrina.vitrinaVirtual.domain.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CloudinaryService {

    private static final Log logger = LogFactory.getLog(CloudinaryService.class);

    @Autowired
    private Cloudinary cloudinary;

    public record UploadResult(String url, String base64) {}

    public UploadResult uploadImageAndGetBase64(MultipartFile file, String folder, String publicIdPrefix) throws IOException {
        // Ejecutamos la subida y la conversión a Base64 en paralelo para ser más eficientes
        CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return uploadImage(file, folder, publicIdPrefix);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<String> base64Future = CompletableFuture.supplyAsync(() -> getBase64FromMultipartFile(file));
        return new UploadResult(uploadFuture.join(), base64Future.join());
    }
    /**
     * Sube una imagen a Cloudinary.
     *
     * @param file El archivo de imagen a subir.
     * @param folder La carpeta de destino en Cloudinary (ej. "vitrina_virtual/products").
     * @param publicIdPrefix Un prefijo para el public_id, usualmente el nombre del producto/tienda.
     * @return La URL segura de la imagen subida.
     * @throws IOException Si ocurre un error durante la subida del archivo.
     */
    public String uploadImage(MultipartFile file, String folder, String publicIdPrefix) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", folder, "public_id", publicIdPrefix + "_" + System.currentTimeMillis(), "resource_type", "image")
        );
        logger.info("Image uploaded to Cloudinary: " + uploadResult.get("secure_url").toString());
        return uploadResult.get("secure_url").toString();
    }

    private String getBase64FromMultipartFile(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert file to Base64", e);
        }
    }
}