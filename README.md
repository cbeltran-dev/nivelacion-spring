# API REST con Java y Spring Boot

---

## 1. Creación del Proyecto

Configurar lo siguiente en:  [https://start.spring.io](https://start.spring.io)

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.0.5 |
| Group | com.example |
| Artifact | tienda |
| Packaging | Jar |
| Java | 17 |

### Dependencias iniciales

Agrega las siguientes dependencias desde el buscador de Spring Initializr:

- **Spring Web** — permite crear endpoints HTTP (controllers, REST)
- **Spring Data JPA** — manejo de base de datos con JPA/Hibernate
- **Lombok** — reduce código repetitivo (getters, setters, constructores)
- **PostgreSQL Driver** — conector para comunicarse con PostgreSQL

Clic en **Generate**, descomprime el zip y abrir el proyecto.

### Arquitectura que seguiremos

```
Cliente (Postman)
    │
    ▼
Controller  ──  recibe peticiones HTTP, valida datos
    │
    ▼
Service  ────  lógica de negocio, convierte con ModelMapper
    │         │
    │         ▼
    │      FeignClient  ──  consulta API externa (SUNAT por RUC)
    ▼
Repository  ──  accede a PostgreSQL vía JPA
    │
    ▼
Entity  ─────  representa la tabla en la BD
```

---

### Estructura de paquetes

```
com.example.tienda
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── exception
├── feignClient
├── mapper
├── repository
└── service
```

> Cada paquete tiene una responsabilidad clara.
---

## 2. Hola Mundo

Dentro del paquete `controller`, crea la clase `HolaMundoController`:

```java
@RestController
@RequestMapping("/api/v1/test")
public class HolaMundoController {

    @GetMapping("/hola")
    public String holaMundo() {
        return "¡Hola Mundo desde Spring Boot!";
    }
}
```
Levantar el proyecto y en Postman realizamos una petición GET a:
```
GET http://localhost:8080/api/v1/test/hola
```
Respuesta:

```
¡Hola Mundo desde Spring Boot!
```
---

## 3. Base de Datos y application.yaml

Crear una base de datos en PostgreSQL.

```sql
CREATE DATABASE tienda;
```

### Configurar application.yaml

```yaml
spring:
  application:
    name: tienda
  datasource:
    username: postgres
    password: tu_password
    url: jdbc:postgresql://localhost:5432/tienda
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

api:
  token: tu_token_de_reniec
```

---

## 4. Entidad UsuarioEntity

Dentro del paquete `entity`, crear la clase `UsuarioEntity`:

```java
package com.example.tienda.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    private String nombres;
    private String apellidos;
    private String username;
    private String password;
    private String email;

    @Column(name = "fecha_nacimiento")
    private Date fechaNacimiento;

    private String dni;

    @Column(name = "fecha_creacion")
    private Date fechaCreacion = new Date();

    @Column(name = "fecha_actualizacion")
    private Date fechaActualizacion;
}
```

### Anotaciones

| Anotación | Función |
|---|---|
| `@Entity` | Mapea la clase a una tabla en la BD |
| `@Table(name)` | Nombre de la tabla |
| `@Id` + `@GeneratedValue(generator = "UUID")` | PK con UUID autogenerado |
| `@Column(name)` | Nombre personalizado de la columna |
| `@Getter` / `@Setter` | Genera getters y setters |
| `@NoArgsConstructor` | Constructor vacío |
| `@AllArgsConstructor` | Constructor con todos los campos |

---

## 5. UsuarioRepository

Dentro del paquete `repository`, crear la interfaz `UsuarioRepository`:

```java
package com.example.tienda.repository;

import com.example.tienda.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    // Query Method
    List<UsuarioEntity> findByNombresContainingIgnoreCase(String nombre);

    // JPQL
    @Query("SELECT u FROM UsuarioEntity u WHERE LOWER(u.nombres) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<UsuarioEntity> findByNombresJpql(@Param("nombre") String nombre);

    // Native Query
    @Query(value = "SELECT * FROM usuarios WHERE nombres ILIKE CONCAT('%', :nombre, '%')", nativeQuery = true)
    List<UsuarioEntity> findByNombresNative(@Param("nombre") String nombre);
}
```

`JpaRepository<UsuarioEntity, UUID>` provee: `save()` · `findById()` · `findAll()` · `deleteById()` · `existsById()`

## Referencias

- [JPA Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

---

## 6. UsuarioService

Dentro del paquete `service`, crear la clase `UsuarioService`:

```java
package com.example.tienda.service;

import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioEntity saveUsuario(UsuarioEntity usuario) {
        return usuarioRepository.save(usuario);
    }
}
```

| Concepto | Descripción |
|---|---|
| `@Service` | Marca la clase como Bean de Spring |
| Inyección por constructor | Spring detecta la dependencia y la inyecta automáticamente al arrancar |
| Beans | Objetos creados y administrados por Spring: `@Service`, `@Repository`, `@RestController`, `@Component`, `@Bean` |

---

## 7. UsuarioController

Dentro del paquete `controller`, crear la clase `UsuarioController`:

```java
package com.example.tienda.controller;

import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/save")
    public UsuarioEntity save(@RequestBody UsuarioEntity usuario) {
        return usuarioService.saveUsuario(usuario);
    }
}
```

| Anotación | Función |
|---|---|
| `@PostMapping` | Mapea peticiones HTTP POST |
| `@RequestBody` | Deserializa el JSON del body al objeto Java |

### Flujo completo

```
Cliente (Postman)
      │
      │  POST /api/v1/usuario/save
      ▼
UsuarioController        ← Recibe la petición HTTP, valida la ruta
      │
      │  llama a saveUsuario()
      ▼
UsuarioService           ← Ejecuta la lógica de negocio
      │
      │  llama a save()
      ▼
UsuarioRepository        ← Traduce a SQL y ejecuta en la BD
      │
      │  INSERT INTO usuarios ...
      ▼
Base de datos (PostgreSQL)
      │
      │  retorna el registro guardado
      ▼
UsuarioRepository → UsuarioService → UsuarioController → Cliente
```

---

## 8. 🧪 Prueba — POST /save

Levantar el proyecto y abrir Postman. Crear una petición POST:

```
POST http://localhost:8080/api/v1/usuario/save
```
Body (raw JSON):

```json
{
    "nombres": "Juan Carlos",
    "apellidos": "Pérez López",
    "username": "juan.perez",
    "password": "12345678",
    "email": "juan@email.com",
    "fechaNacimiento": "1995-05-15",
    "dni": "12345678"
}
```

Respuesta de la API:

```json
{
    "id": "a3b4c5d6-e7f8-...",
    "nombres": "Juan Carlos",
    "apellidos": "Pérez López",
    "username": "juan.perez",
    "password": "12345678",
    "email": "juan@email.com",
    "fechaNacimiento": "1995-05-15T00:00:00.000+00:00",
    "dni": "12345678",
    "fechaCreacion": "2025-01-01T00:00:00.000+00:00",
    "fechaActualizacion": null
}
```

---

## 9. ApiResponse\<T\>

Dentro del paquete `dto/response`, crear la clase `ApiResponse`:

```java
package com.example.tienda.dto.response;

import lombok.*;

@Getter @Setter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```
Con esto, todas las respuestas de tu API tendrán siempre esta forma:

```json
{
    "success": true,
    "message": "Creado exitosamente",
    "data": { ... }
}
```
---

## 10. Integrar ApiResponse en el Controller

Modificar `UsuarioController` para usar `ApiResponse`:

```java
@PostMapping("/save")
public ApiResponse<UsuarioEntity> save(@RequestBody UsuarioEntity usuario) {
    UsuarioEntity response = usuarioService.saveUsuario(usuario);
    return new ApiResponse<>(true, "Usuario creado exitosamente", response);
}
```

---

## 11. Controller con ResponseEntity

```java
@PostMapping("/save")
public ResponseEntity<ApiResponse<UsuarioEntity>> save(@RequestBody UsuarioEntity usuario) {
    UsuarioEntity response = usuarioService.saveUsuario(usuario);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Usuario creado exitosamente", response));
}
```
**Códigos HTTP más comunes:**

| Código | Constante | Cuándo usarlo |
|---|---|---|
| 200 | `HttpStatus.OK` | Operación exitosa (GET, PUT) |
| 201 | `HttpStatus.CREATED` | Recurso creado exitosamente (POST) |
| 400 | `HttpStatus.BAD_REQUEST` | Datos inválidos en la petición |
| 404 | `HttpStatus.NOT_FOUND` | Recurso no encontrado |
| 502 | `HttpStatus.BAD_GATEWAY` | Fallo al comunicarse con un servicio externo |

### 🧪 Prueba — Verificar formato ApiResponse y código 201

---

## 12. CRUD Completo — Nuevos Métodos

### Actualizar UsuarioService

Crear los métodos de búsqueda:

```java
package com.example.tienda.service;

import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioEntity saveUsuario(UsuarioEntity usuario) {
        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity findById(UUID id) {
        Optional<UsuarioEntity> optional = usuarioRepository.findById(id);
        if (optional.isEmpty()) return null;
        return optional.get();
    }

    public List<UsuarioEntity> findAll() {
        return usuarioRepository.findAll();
    }

    public List<UsuarioEntity> findByNombre(String nombre) {
        List<UsuarioEntity> usuarios = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
        if (usuarios.isEmpty()) return null;
        return usuarios;
    }
}
```

`Optional<T>` — contenedor que puede o no tener valor. `isEmpty()` verifica si está vacío. `get()` obtiene el valor.

### Actualizar UsuarioController

Agregar los nuevos endpoints:

```java
package com.example.tienda.controller;

import com.example.tienda.dto.response.ApiResponse;
import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.service.UsuarioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("api/v1/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<UsuarioEntity>> save(@RequestBody UsuarioEntity usuario) {
        UsuarioEntity response = usuarioService.saveUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario creado exitosamente", response));
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<UsuarioEntity>> findById(@PathVariable UUID id) {
        UsuarioEntity response = usuarioService.findById(id);
        if (response == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Usuario no encontrado", null));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", response));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<UsuarioEntity>>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", usuarioService.findAll()));
    }

    @GetMapping("/find/name/{nombre}")
    public ResponseEntity<ApiResponse<List<UsuarioEntity>>> findByName(@PathVariable String nombre) {
        List<UsuarioEntity> response = usuarioService.findByNombre(nombre);
        if (response == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "No se encontraron coincidencias", null));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", response));
    }
}
```

| Anotación | Función |
|---|---|
| `@GetMapping("/find/{id}")` | `{id}` es variable de ruta |
| `@PathVariable UUID id` | Extrae y convierte el valor de la URL |

### 🧪 Prueba — Nuevos endpoints

```
GET http://localhost:8080/api/v1/usuario/find/{id}
GET http://localhost:8080/api/v1/usuario/find
GET http://localhost:8080/api/v1/usuario/find/name/juan
```

---

## 13. DTOs — Request y Response

Los **DTOs (Data Transfer Objects)** son clases que controlan exactamente qué datos entran y salen de la API, sin exponer la entidad directamente.

### UsuarioRequest

Dentro de `dto/request`, crear `UsuarioRequest`:

```java
package com.example.tienda.dto.request;

import lombok.*;
import java.util.Date;

@Getter @Setter
public class UsuarioRequest {
    private String nombres;
    private String apellidos;
    private String username;
    private String password;
    private String email;
    private Date fechaNacimiento;
    private String dni;
}
```

### UsuarioResponse

Dentro de `dto/response`, crear `UsuarioResponse`:

```java
package com.example.tienda.dto.response;

import lombok.*;
import java.util.Date;

@Getter @Setter
public class UsuarioResponse {
    private String nombres;
    private String apellidos;
    private String username;
    private String email;
    private Date fechaNacimiento;
    private String dni;
}
```

> `password` se omite intencionalmente del response.

---

## 14. Mapeo Manual Entity ↔ DTO

### UsuarioService

Modificar `UsuarioService` para trabajar con DTOs y agregar los métodos de mapeo:

```java
package com.example.tienda.service;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    private UsuarioEntity mapToEntity(UsuarioRequest request) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setNombres(request.getNombres());
        entity.setApellidos(request.getApellidos());
        entity.setUsername(request.getUsername());
        entity.setPassword(request.getPassword());
        entity.setEmail(request.getEmail());
        entity.setFechaNacimiento(request.getFechaNacimiento());
        entity.setDni(request.getDni());
        return entity;
    }

    private UsuarioResponse mapToResponse(UsuarioEntity entity) {
        UsuarioResponse response = new UsuarioResponse();
        response.setNombres(entity.getNombres());
        response.setApellidos(entity.getApellidos());
        response.setUsername(entity.getUsername());
        response.setEmail(entity.getEmail());
        response.setFechaNacimiento(entity.getFechaNacimiento());
        response.setDni(entity.getDni());
        return response;
    }

    public UsuarioResponse saveUsuario(UsuarioRequest request) {
        UsuarioEntity entity = mapToEntity(request);
        UsuarioEntity saved = usuarioRepository.save(entity);
        return mapToResponse(saved);
    }

    public UsuarioResponse findById(UUID id) {
        Optional<UsuarioEntity> optional = usuarioRepository.findById(id);
        if (optional.isEmpty()) return null;
        return mapToResponse(optional.get());
    }

    public List<UsuarioResponse> findAll() {
        List<UsuarioEntity> entities = usuarioRepository.findAll();
        List<UsuarioResponse> responseList = new ArrayList<>();
        for (UsuarioEntity entity : entities) {
            responseList.add(mapToResponse(entity));
        }
        return responseList;
    }

    public List<UsuarioResponse> findByNombre(String nombre) {
        List<UsuarioEntity> entities = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
        if (entities.isEmpty()) return null;
        List<UsuarioResponse> responseList = new ArrayList<>();
        for (UsuarioEntity entity : entities) {
            responseList.add(mapToResponse(entity));
        }
        return responseList;
    }
}
```

### UsuarioController

Modificar `UsuarioController` para usar `UsuarioRequest` y `UsuarioResponse`:

```java
package com.example.tienda.controller;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.ApiResponse;
import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.service.UsuarioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("api/v1/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<UsuarioResponse>> save(@RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.saveUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario creado exitosamente", response));
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> findById(@PathVariable UUID id) {
        UsuarioResponse response = usuarioService.findById(id);
        if (response == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Usuario no encontrado", null));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", response));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", usuarioService.findAll()));
    }

    @GetMapping("/find/name/{nombre}")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> findByName(@PathVariable String nombre) {
        List<UsuarioResponse> response = usuarioService.findByNombre(nombre);
        if (response == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "No se encontraron coincidencias", null));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "OK", response));
    }
}
```

### 🧪 Prueba — DTOs en acción

```
POST http://localhost:8080/api/v1/usuario/save
```

```json
{
    "nombres": "Juan Carlos",
    "apellidos": "Pérez López",
    "username": "juan.perez",
    "password": "miPassword123",
    "email": "juan@email.com",
    "fechaNacimiento": "1995-05-15",
    "dni": "12345678"
}
```

Respuesta esperada:

```json
{
    "success": true,
    "message": "Usuario creado exitosamente",
    "data": {
        "nombres": "Juan Carlos",
        "apellidos": "Pérez López",
        "username": "juan.perez",
        "email": "juan@email.com",
        "fechaNacimiento": "1995-05-15T00:00:00.000+00:00",
        "dni": "12345678"
    }
}
```

> Nota que `password` no aparece en el response — el DTO de respuesta lo omite.

---
---

## 15. Validaciones con Jakarta Validation

### Agregar dependencia en `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Modificar `UsuarioRequest`

Agregar las anotaciones de validación a cada campo:

```java
package com.example.tienda.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Date;

@Getter @Setter
public class UsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private Date fechaNacimiento;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente 8 dígitos numéricos")
    private String dni;
}
```

| Anotación | Valida que... |
|---|---|
| `@NotBlank` | El `String` no sea `null`, vacío ni solo espacios |
| `@NotNull` | El campo no sea `null` (cualquier tipo) |
| `@Size(min, max)` | La longitud del `String` esté dentro del rango |
| `@Email` | El `String` tenga formato de email válido |
| `@Past` | La fecha sea anterior a hoy |
| `@Pattern(regexp)` | El `String` cumpla la expresión regular |

### Activar validación en `UsuarioController`

Agregar `@Valid` en el método `save`:

```java
import jakarta.validation.Valid;

@PostMapping("/save")
public ResponseEntity<ApiResponse<UsuarioResponse>> save(@Valid @RequestBody UsuarioRequest request) {
    UsuarioResponse response = usuarioService.saveUsuario(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Usuario creado exitosamente", response));
}
```

`@Valid` activa las validaciones del DTO antes de ejecutar el método. Si falla alguna, lanza `MethodArgumentNotValidException`.

### 🧪 Prueba — Datos inválidos

```
POST http://localhost:8080/api/v1/usuario/save
```

```json
{
    "nombres": "",
    "password": "123",
    "email": "no-es-email",
    "fechaNacimiento": "2099-01-01",
    "dni": "abcd"
}
```

Spring retorna un error largo sin formato. En el punto 17 se convierte en una respuesta limpia.

---

## 16. ModelMapper

### Agregar dependencia en `pom.xml`

```xml
<dependency>
    <groupId>org.modelmapper</groupId>
    <artifactId>modelmapper</artifactId>
    <version>3.2.4</version>
</dependency>
```

### Crear `ModelMapperConfig`

Dentro del paquete `mapper`, crear la clase `ModelMapperConfig`:

```java
package com.example.tienda.mapper;

import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.entity.UsuarioEntity;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(UsuarioEntity.class, UsuarioResponse.class);
        return modelMapper;
    }
}
```

| Anotación | Función |
|---|---|
| `@Configuration` | Spring lee esta clase al arrancar |
| `@Bean` | Spring administra e inyecta el objeto retornado |
| `MatchingStrategies.STRICT` | Los campos deben coincidir exactamente en nombre y tipo |

### Refactorizar `UsuarioService` con ModelMapper

Modificar `UsuarioService` reemplazando el mapeo manual. Las listas ahora se mapean con `stream().map()`:

```java
package com.example.tienda.service;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.modelMapper = modelMapper;
    }

    public UsuarioResponse saveUsuario(UsuarioRequest request) {
        UsuarioEntity entity = new UsuarioEntity();
        modelMapper.map(request, entity);
        UsuarioEntity saved = usuarioRepository.save(entity);
        return modelMapper.map(saved, UsuarioResponse.class);
    }

    public UsuarioResponse findById(UUID id) {
        Optional<UsuarioEntity> optional = usuarioRepository.findById(id);
        if (optional.isEmpty()) return null;
        return modelMapper.map(optional.get(), UsuarioResponse.class);
    }

    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll()
                .stream()
                .map(entity -> modelMapper.map(entity, UsuarioResponse.class))
                .collect(Collectors.toList());
    }

    public List<UsuarioResponse> findByNombre(String nombre) {
        List<UsuarioEntity> entities = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
        if (entities.isEmpty()) return null;
        return entities.stream()
                .map(entity -> modelMapper.map(entity, UsuarioResponse.class))
                .collect(Collectors.toList());
    }
}
```

**`stream().map()` para listas:**
- `stream()` — convierte la lista en un flujo de elementos
- `.map()` — aplica la transformación a cada elemento
- `.collect(Collectors.toList())` — agrupa los resultados en una nueva lista

---

## 17. Manejo de Excepciones

### Crear `ResourceNotFoundException`

Dentro del paquete `exception`, crear `ResourceNotFoundException`:

```java
package com.example.tienda.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### Crear `ExternalServiceException`

Dentro del paquete `exception`, crear `ExternalServiceException`:

```java
package com.example.tienda.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
```

### Crear `GlobalExceptionHandler`

Dentro del paquete `exception`, crear `GlobalExceptionHandler`:

```java
package com.example.tienda.exception;

import com.example.tienda.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors
                    .computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Error de validación", fieldErrors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(
            ExternalServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }
}
```

| Anotación | Función |
|---|---|
| `@RestControllerAdvice` | Intercepta excepciones de todos los controllers |
| `@ExceptionHandler(X.class)` | Maneja el tipo de excepción indicado |

### Refactorizar `UsuarioService` — reemplazar `null` por excepciones

Modificar los métodos `findById` y `findByNombre` en `UsuarioService`:

```java
public UsuarioResponse findById(UUID id) {
    UsuarioEntity entity = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    return modelMapper.map(entity, UsuarioResponse.class);
}

public List<UsuarioResponse> findByNombre(String nombre) {
    List<UsuarioEntity> entities = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
    if (entities.isEmpty())
        throw new ResourceNotFoundException("No se encontraron usuarios con el nombre: " + nombre);
    return entities.stream()
            .map(entity -> modelMapper.map(entity, UsuarioResponse.class))
            .collect(Collectors.toList());
}
```

`.orElseThrow()` — retorna el valor del `Optional` si existe, o lanza la excepción indicada si está vacío.

### Limpiar `UsuarioController`

Modificar `UsuarioController` — el `GlobalExceptionHandler` se encarga de los errores, el controller solo retorna el resultado exitoso:

```java
package com.example.tienda.controller;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.ApiResponse;
import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("api/v1/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<UsuarioResponse>> save(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario creado exitosamente", usuarioService.saveUsuario(request)));
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", usuarioService.findById(id)));
    }

    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> findAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", usuarioService.findAll()));
    }

    @GetMapping("/find/name/{nombre}")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> findByName(@PathVariable String nombre) {
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", usuarioService.findByNombre(nombre)));
    }
}
```

### 🧪 Prueba — Errores limpios

**Validación fallida:**

```json
{
    "success": false,
    "message": "Error de validación",
    "data": {
        "password": ["La contraseña debe tener al menos 8 caracteres"],
        "email": ["El formato del email no es válido"],
        "fechaNacimiento": ["La fecha de nacimiento debe ser una fecha pasada"],
        "dni": ["El DNI debe contener exactamente 8 dígitos numéricos"]
    }
}
```

**Recurso no encontrado:**

```
GET http://localhost:8080/api/v1/usuario/find/00000000-0000-0000-0000-000000000000
```

```json
{
    "success": false,
    "message": "Usuario no encontrado con id: 00000000-...",
    "data": null
}
```

---

## 18. RENIEC con OpenFeign

### Agregar dependencia en `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>5.0.1</version>
</dependency>
```

### Habilitar Feign en `TiendaApplication`

Modificar la clase principal agregando `@EnableFeignClients`:

```java
package com.example.tienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TiendaApplication {
    public static void main(String[] args) {
        SpringApplication.run(TiendaApplication.class, args);
    }
}
```

### Crear `ReniecResponse`

Dentro de `dto/response`, crear `ReniecResponse`:

```java
package com.example.tienda.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ReniecResponse {

    @JsonProperty("first_name")       private String firstName;
    @JsonProperty("first_last_name")  private String firstLastName;
    @JsonProperty("second_last_name") private String secondLastName;
    @JsonProperty("full_name")        private String fullName;
    @JsonProperty("document_number")  private String documentNumber;
}
```

`@JsonProperty` — mapea el campo `snake_case` del JSON al atributo `camelCase` de la clase.

### Crear `ReniecClient`

Dentro del paquete `feignClient`, crear la interfaz `ReniecClient`:

```java
package com.example.tienda.feignClient;

import com.example.tienda.dto.response.ReniecResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "reniec-client", url = "https://api.decolecta.com/v1/reniec/dni")
public interface ReniecClient {

    @GetMapping()
    ReniecResponse getData(
            @RequestParam String numero,
            @RequestHeader("Authorization") String token
    );
}
```

| Anotación | Función |
|---|---|
| `@FeignClient(url)` | Declara el cliente apuntando a la URL externa |
| `@RequestParam` | Agrega `?numero=...` a la URL |
| `@RequestHeader` | Agrega el header `Authorization` a la petición |

### Refactorizar `UsuarioRequest`

Modificar `UsuarioRequest` eliminando `nombres`, `apellidos` y `username` — estos campos ahora vienen de RENIEC:

```java
package com.example.tienda.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Date;

@Getter @Setter
public class UsuarioRequest {

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private Date fechaNacimiento;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente 8 dígitos numéricos")
    private String dni;
}
```

### Refactorizar `UsuarioService` con RENIEC

Modificar `UsuarioService` agregando `ReniecClient` e integrando la llamada a RENIEC dentro de `saveUsuario`:

```java
package com.example.tienda.service;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.ReniecResponse;
import com.example.tienda.dto.response.UsuarioResponse;
import com.example.tienda.entity.UsuarioEntity;
import com.example.tienda.exception.ExternalServiceException;
import com.example.tienda.exception.ResourceNotFoundException;
import com.example.tienda.feignClient.ReniecClient;
import com.example.tienda.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;
    private final ReniecClient reniecClient;

    @Value("${api.token}")
    private String token;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ModelMapper modelMapper,
                          ReniecClient reniecClient) {
        this.usuarioRepository = usuarioRepository;
        this.modelMapper = modelMapper;
        this.reniecClient = reniecClient;
    }

    public UsuarioResponse saveUsuario(UsuarioRequest request) {
        // 1. Consultar RENIEC con el DNI
        ReniecResponse reniecData;
        try {
            reniecData = reniecClient.getData(request.getDni(), token);
        } catch (Exception e) {
            throw new ExternalServiceException("Error al consultar RENIEC: " + e.getMessage());
        }

        // 2. Generar username: primerNombre.primerApellido en minúsculas
        // "JUAN CARLOS".split("\\s+")[0] → "JUAN" → "juan"
        String primerNombre = reniecData.getFirstName().split("\\s+")[0].toLowerCase();
        String username = primerNombre + "." + reniecData.getFirstLastName().toLowerCase();

        // 3. Mapear request → entity y completar con datos de RENIEC
        UsuarioEntity entity = new UsuarioEntity();
        modelMapper.map(request, entity);
        entity.setNombres(reniecData.getFirstName());
        entity.setApellidos(reniecData.getFirstLastName() + " " + reniecData.getSecondLastName());
        entity.setUsername(username);

        // 4. Guardar y retornar
        return modelMapper.map(usuarioRepository.save(entity), UsuarioResponse.class);
    }

    public UsuarioResponse findById(UUID id) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return modelMapper.map(entity, UsuarioResponse.class);
    }

    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, UsuarioResponse.class))
                .collect(Collectors.toList());
    }

    public List<UsuarioResponse> findByNombre(String nombre) {
        List<UsuarioEntity> entities = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
        if (entities.isEmpty())
            throw new ResourceNotFoundException("No se encontraron usuarios con el nombre: " + nombre);
        return entities.stream()
                .map(entity -> modelMapper.map(entity, UsuarioResponse.class))
                .collect(Collectors.toList());
    }
}
```

`@Value("${api.token}")` — inyecta el valor de `api.token` del `application.yaml`.

### 🧪 Prueba — RENIEC autocompleta datos

```
POST http://localhost:8080/api/v1/usuario/save
```

```json
{
    "password": "miPassword123",
    "email": "juan@email.com",
    "fechaNacimiento": "1995-05-15",
    "dni": "12345678"
}
```

Respuesta esperada:

```json
{
    "success": true,
    "message": "Usuario creado exitosamente",
    "data": {
        "nombres": "JUAN CARLOS",
        "apellidos": "PÉREZ LÓPEZ",
        "username": "juan.pérez",
        "email": "juan@email.com",
        "fechaNacimiento": "1995-05-15T00:00:00.000+00:00",
        "dni": "12345678"
    }
}
```

---

## 19. Relaciones JPA

### Crear `RolEntity`

Dentro del paquete `entity`, crear `RolEntity`:

```java
package com.example.tienda.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RolEntity {

    @Id @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nombre;
}
```

### Crear `DireccionEntity`

Dentro del paquete `entity`, crear `DireccionEntity`:

```java
package com.example.tienda.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "direcciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DireccionEntity {

    @Id @GeneratedValue(generator = "UUID")
    private UUID id;

    private String calle;
    private String ciudad;
    private String departamento;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;
}
```

### Refactorizar `UsuarioEntity` — agregar relaciones

Modificar `UsuarioEntity` agregando los campos de relación al final de la clase:

```java
@ManyToOne
@JoinColumn(name = "rol_id")
private RolEntity rol;

@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<DireccionEntity> direcciones;
```

La clase completa queda así:

```java
package com.example.tienda.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "usuarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UsuarioEntity {

    @Id @GeneratedValue(generator = "UUID")
    private UUID id;

    private String nombres;
    private String apellidos;
    private String username;
    private String password;
    private String email;

    @Column(name = "fecha_nacimiento")
    private Date fechaNacimiento;

    private String dni;

    @Column(name = "fecha_creacion")
    private Date fechaCreacion = new Date();

    @Column(name = "fecha_actualizacion")
    private Date fechaActualizacion;

    @ManyToOne
    @JoinColumn(name = "rol_id")
    private RolEntity rol;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DireccionEntity> direcciones;
}
```

| Anotación | Función |
|---|---|
| `@ManyToOne` | Muchos usuarios pueden tener el mismo rol |
| `@JoinColumn(name = "rol_id")` | Crea la FK `rol_id` en la tabla `usuarios` |
| `@OneToMany(mappedBy = "usuario")` | Un usuario tiene muchas direcciones. `mappedBy` apunta al campo dueño en `DireccionEntity` |
| `CascadeType.ALL` | Operaciones en Usuario se propagan a sus Direcciones |
| `FetchType.LAZY` | Las direcciones no se cargan hasta acceder a `getDirecciones()` |

**Dueño de la relación:** `DireccionEntity` — tiene la FK `usuario_id` en su tabla.

### Crear `RolRepository`

Dentro del paquete `repository`, crear la interfaz `RolRepository`:

```java
package com.example.tienda.repository;

import com.example.tienda.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<RolEntity, UUID> {
    Optional<RolEntity> findByNombre(String nombre);
}
```

### Crear `DireccionRepository`

Dentro del paquete `repository`, crear la interfaz `DireccionRepository`:

```java
package com.example.tienda.repository;

import com.example.tienda.entity.DireccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DireccionRepository extends JpaRepository<DireccionEntity, UUID> {
}
```

### Insertar roles en PostgreSQL

Levantar el proyecto primero para que Hibernate cree la tabla `roles`, luego ejecutar:

```sql
INSERT INTO roles (id, nombre) VALUES (gen_random_uuid(), 'ADMIN');
INSERT INTO roles (id, nombre) VALUES (gen_random_uuid(), 'CLIENT');
```

### Crear `DireccionResponse`

Dentro de `dto/response`, crear `DireccionResponse`:

```java
package com.example.tienda.dto.response;

import lombok.*;
import java.util.UUID;

@Getter @Setter
public class DireccionResponse {
    private UUID id;
    private String calle;
    private String ciudad;
    private String departamento;
    private String codigoPostal;
}
```

### Actualizar `UsuarioResponse`

Modificar `UsuarioResponse` agregando `id`, `rol` y `direcciones`:

```java
package com.example.tienda.dto.response;

import lombok.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class UsuarioResponse {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String username;
    private String email;
    private Date fechaNacimiento;
    private String dni;
    private String rol;
    private List<DireccionResponse> direcciones;
}
```

> `rol` es un `String` — solo exponemos el nombre, no el objeto completo.

### Actualizar `ModelMapperConfig`

Modificar `ModelMapperConfig` registrando el nuevo mapeo de `DireccionEntity`:

```java
@Bean
public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    modelMapper.createTypeMap(UsuarioEntity.class, UsuarioResponse.class);
    modelMapper.createTypeMap(DireccionEntity.class, DireccionResponse.class);
    return modelMapper;
}
```

### Refactorizar `UsuarioService` — asignar Rol y mapear relaciones

Modificar `UsuarioService` agregando `RolRepository` e inyectándolo por constructor. Agregar el método privado `toResponse` para manejar el mapeo de los campos de relación:

```java
package com.example.tienda.service;

import com.example.tienda.dto.request.UsuarioRequest;
import com.example.tienda.dto.response.*;
import com.example.tienda.entity.*;
import com.example.tienda.exception.*;
import com.example.tienda.feignClient.ReniecClient;
import com.example.tienda.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ModelMapper modelMapper;
    private final ReniecClient reniecClient;

    @Value("${api.token}")
    private String token;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          ModelMapper modelMapper,
                          ReniecClient reniecClient) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.modelMapper = modelMapper;
        this.reniecClient = reniecClient;
    }

    // Mapeo especial: extrae el nombre del rol y mapea la lista de direcciones
    private UsuarioResponse toResponse(UsuarioEntity entity) {
        UsuarioResponse response = modelMapper.map(entity, UsuarioResponse.class);
        if (entity.getRol() != null)
            response.setRol(entity.getRol().getNombre());
        response.setDirecciones(
                entity.getDirecciones() != null
                        ? entity.getDirecciones().stream()
                                .map(dir -> modelMapper.map(dir, DireccionResponse.class))
                                .collect(Collectors.toList())
                        : Collections.emptyList()
        );
        return response;
    }

    public UsuarioResponse saveUsuario(UsuarioRequest request) {
        // 1. Consultar RENIEC con el DNI
        ReniecResponse reniecData;
        try {
            reniecData = reniecClient.getData(request.getDni(), token);
        } catch (Exception e) {
            throw new ExternalServiceException("Error al consultar RENIEC: " + e.getMessage());
        }

        // 2. Generar username: primerNombre.primerApellido en minúsculas
        String primerNombre = reniecData.getFirstName().split("\\s+")[0].toLowerCase();
        String username = primerNombre + "." + reniecData.getFirstLastName().toLowerCase();

        // 3. Buscar rol CLIENT — se asigna por defecto al registrar
        RolEntity rolClient = rolRepository.findByNombre("CLIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENT no encontrado"));

        // 4. Mapear request → entity y completar con datos de RENIEC y Rol
        UsuarioEntity entity = new UsuarioEntity();
        modelMapper.map(request, entity);
        entity.setNombres(reniecData.getFirstName());
        entity.setApellidos(reniecData.getFirstLastName() + " " + reniecData.getSecondLastName());
        entity.setUsername(username);
        entity.setRol(rolClient);

        // 5. Guardar y retornar
        return toResponse(usuarioRepository.save(entity));
    }

    public UsuarioResponse findById(UUID id) {
        UsuarioEntity entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return toResponse(entity);
    }

    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponse> findByNombre(String nombre) {
        List<UsuarioEntity> entities = usuarioRepository.findByNombresContainingIgnoreCase(nombre);
        if (entities.isEmpty())
            throw new ResourceNotFoundException("No se encontraron usuarios con el nombre: " + nombre);
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
```

`this::toResponse` — referencia al método. Equivale a `entity -> this.toResponse(entity)`.

### 🧪 Prueba final

```
POST http://localhost:8080/api/v1/usuario/save
```

```json
{
    "password": "miPassword123",
    "email": "juan@email.com",
    "fechaNacimiento": "1995-05-15",
    "dni": "12345678"
}
```

Respuesta esperada:

```json
{
    "success": true,
    "message": "Usuario creado exitosamente",
    "data": {
        "id": "a3b4c5d6-...",
        "nombres": "JUAN CARLOS",
        "apellidos": "PÉREZ LÓPEZ",
        "username": "juan.pérez",
        "email": "juan@email.com",
        "fechaNacimiento": "1995-05-15T00:00:00.000+00:00",
        "dni": "12345678",
        "rol": "CLIENT",
        "direcciones": []
    }
}
```

---

## Estructura final del proyecto

```
com.example.tienda
├── controller       → UsuarioController
├── dto
│   ├── request      → UsuarioRequest
│   └── response     → ApiResponse · DireccionResponse · ReniecResponse · UsuarioResponse
├── entity           → DireccionEntity · RolEntity · UsuarioEntity
├── exception        → ExternalServiceException · GlobalExceptionHandler · ResourceNotFoundException
├── feignClient      → ReniecClient
├── mapper           → ModelMapperConfig
├── repository       → DireccionRepository · RolRepository · UsuarioRepository
├── service          → UsuarioService
└── TiendaApplication
```
