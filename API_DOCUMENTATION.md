# Documentación de la API - Vitrina Virtual

## 📚 Acceso a la Documentación

Una vez que hayas iniciado la aplicación Spring Boot, podrás acceder a la documentación de la API en los siguientes endpoints:

### 🔗 URLs de Documentación

1. **Swagger UI** (Interfaz interactiva):
   ```
   http://localhost:8080/swagger-ui.html
   ```

2. **ReDoc** (Documentación alternativa):
   ```
   http://localhost:8080/redoc.html
   ```

3. **OpenAPI JSON** (Especificación en formato JSON):
   ```
   http://localhost:8080/v3/api-docs
   ```

4. **OpenAPI YAML** (Especificación en formato YAML):
   ```
   http://localhost:8080/v3/api-docs.yaml
   ```

## 🔐 Autenticación

### 📚 **Acceso a la Documentación**
Los endpoints de documentación **NO requieren autenticación** y están disponibles públicamente:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- ReDoc: `http://localhost:8080/redoc.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 🔑 **Autenticación para Endpoints de API**
La mayoría de los endpoints de la API requieren autenticación JWT. Para usar la documentación interactiva:

1. **Registra un usuario** usando el endpoint `/api/auth/register`
2. **Inicia sesión** usando el endpoint `/api/auth/login` para obtener un token JWT
3. **Autoriza en Swagger UI**:
   - Haz clic en el botón "Authorize" (🔒) en la parte superior de Swagger UI
   - Ingresa tu token JWT en el formato: `Bearer tu_token_aqui`
   - Haz clic en "Authorize"

## 📋 Endpoints Disponibles

### 🔑 Autenticación (`/api/auth`)
- `POST /api/auth/register` - Registrar nuevo usuario
- `POST /api/auth/login` - Iniciar sesión

### 👤 Usuarios (`/api/users`)
- `GET /api/users` - Obtener perfil de usuario (requiere autenticación)

### 🏪 Tiendas (`/api/stores`)
- `POST /api/stores` - Crear tienda (solo administradores)
- `GET /api/stores` - Listar todas las tiendas
- `GET /api/stores/{id}` - Obtener tienda por ID
- `DELETE /api/stores/{id}` - Eliminar tienda (solo administradores)
- `GET /api/stores/address/{address}` - Buscar tiendas por dirección
- `GET /api/stores/name/{name}` - Buscar tiendas por nombre
- `GET /api/stores/pay-advertising` - Tiendas con publicidad pagada

### 👕 Productos (`/api/products`)
- `POST /api/products` - Crear producto (solo administradores)
- `GET /api/products` - Listar todos los productos
- `GET /api/products/{id}` - Obtener producto por ID
- `DELETE /api/products/{id}` - Eliminar producto (solo administradores)
- `GET /api/products/style/{style}` - Productos por estilo
- `GET /api/products/store/{storeId}` - Productos por tienda
- `GET /api/products/recommended` - Productos recomendados
- `GET /api/products/with-stores` - Productos con información de tiendas
- `GET /api/products/outfit` - Generar outfit con IA (solo clientes)
- `POST /api/products/chat` - Generar outfit desde chat (solo administradores)

## 🚀 Cómo Iniciar

1. **Instalar dependencias**:
   ```bash
   mvn clean install
   ```

2. **Iniciar la aplicación**:
   ```bash
   mvn spring-boot:run
   ```

3. **Acceder a la documentación**:
   - Abre tu navegador y ve a `http://localhost:8080/swagger-ui.html`

## 🛠️ Características de la Documentación

- **Interfaz interactiva**: Prueba los endpoints directamente desde Swagger UI
- **Autenticación JWT**: Soporte completo para tokens Bearer
- **Documentación detallada**: Descripciones, parámetros y respuestas documentadas
- **Múltiples formatos**: JSON, YAML y HTML
- **Filtros y búsqueda**: Encuentra endpoints rápidamente
- **Códigos de respuesta**: Documentación completa de todos los códigos HTTP

## 📝 Notas Importantes

- Los endpoints marcados como "solo administradores" requieren el rol `ROLE_ADMIN`
- Los endpoints de generación de outfits requieren el rol `ROLE_CLIENT`
- La autenticación se realiza mediante JWT tokens
- Los archivos de imagen se suben a Cloudinary
- La generación de outfits utiliza la API de Google Gemini

## 🔧 Configuración

La documentación está configurada en:
- `OpenApiConfig.java` - Configuración principal de OpenAPI
- `application.properties` - Propiedades de SpringDoc
- Anotaciones en los controladores - Documentación de endpoints

¡Disfruta explorando la API de Vitrina Virtual! 🎉
