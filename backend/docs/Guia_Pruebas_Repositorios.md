# Guía: pruebas de repositorios (corrección del Lab 4)

Lo que pidió el profe en la defensa:
1. Pruebas de insert y update (y similares).
2. Justificar o corregir el nombre "IntegrationTest".
3. Que David entienda y pueda explicar cada prueba.

Todo está en el backend, en `src/test/kotlin/cr/ac/una/glifo/repository/`.

---

## 1. Por qué cambiamos el nombre "IntegrationTest"

**Prueba unitaria:** prueba **una sola pieza aislada** (una clase o una función). Todo lo demás se simula, sin Spring y sin base de datos. Ejemplo nuestro: en la app, `ConfidenceScorerTest`.

**Prueba de integración:** prueba **varias capas reales trabajando juntas** de punta a punta. En un backend sería entrar por la API (controller), pasar por el service y llegar a la base, como hace Postman con el backend corriendo.

**Las nuestras no son ninguna de las dos:**
- No son unitarias, porque usan Spring y PostgreSQL de verdad.
- Tampoco son de integración completa, porque no pasan por controller ni service. Prueban **solo la capa de datos**: un repositorio contra la base.

Por eso se llaman por lo que prueban, igual que hace el profe con su `LoadInitData`:

| Antes | Ahora | Qué prueba |
|---|---|---|
| `JpaRepositoryAndEntityIntegrationTest` | `EntityRelationshipsTest` | Las relaciones entre entidades |
| — | `UserRepositoryTest`, `CourseRepositoryTest`, `NoteRepositoryTest` | El CRUD de cada repositorio |
| — | `LoadInitDataTest` | Que `import.sql` cargó los datos |

**Frase para la defensa:** *"Son pruebas de la capa de datos: cada una prueba un repositorio contra PostgreSQL real. No son unitarias porque usan la base, ni de integración porque no pasan por service ni controller. Por eso las nombramos por el repositorio que prueban."*

---

## 2. Cómo está armada cada prueba

```kotlin
@SpringBootTest          // levanta Spring para poder usar los repositorios reales
@ActiveProfiles("test")  // usa application-test.properties -> base glifo_test
@Transactional           // al terminar cada prueba se revierte todo (la base queda igual)
class UserRepositoryTest {
    @Autowired lateinit var userRepository: UserRepository   // Spring lo inyecta
```

Cada prueba tiene tres pasos:
1. **Preparar:** crear el objeto.
2. **Ejecutar:** `save`, `find` o `delete`.
3. **Verificar:** `assertEquals` / `assertTrue`. Si el valor no es el esperado, la prueba falla.

### `flushAndClear()`: el detalle importante
```kotlin
entityManager.flush()  // manda los INSERT/UPDATE pendientes a PostgreSQL
entityManager.clear()  // vacía la memoria (caché) de Hibernate
```
Sin esto, después de un `save` Hibernate puede devolver el objeto **desde su memoria** sin ir a la base, y la prueba "pasaría" sin haber probado PostgreSQL. Con `flush` + `clear`, el `find` siguiente **lee de verdad de la base**.

---

## 3. Qué hace cada prueba

### `LoadInitDataTest`: datos iniciales (estilo del profe)
| Prueba | Qué verifica |
|---|---|
| carga los 3 roles | `roleRepository.findAll()` trae 3 |
| carga las 3 cuentas demo | `userRepository.findAll()` trae 3 y existe el estudiante demo |
| carga el curso demo con sus 2 apuntes | `findByJoinCode("GLF-CALC1")` lo encuentra y tiene 2 apuntes |

### `UserRepositoryTest`
| Prueba | Operación | Cómo |
|---|---|---|
| insert guarda el usuario y le asigna id | **Insert** | `save`, luego el id es mayor que 0 y `existsById` |
| find por email trae el usuario guardado | **Find** | `findByEmail`, y el nombre coincide |
| find por email inexistente devuelve null | **Find** | Un correo que no existe da `null` |
| update cambia el nombre del usuario | **Update** | `findById`, cambiar `firstName`, `save`, y se lee el nuevo nombre |
| delete elimina el usuario | **Delete** | `deleteById`, y `findById` queda vacío |

### `CourseRepositoryTest`
| Prueba | Operación | Cómo |
|---|---|---|
| insert guarda el curso | **Insert** | `save` con el docente demo como dueño (N a 1) |
| find por código de unión | **Find** | `findByJoinCode("GLF-TEST1")` |
| find por docente | **Find** | `findByOwnerId` trae 2: el demo y el nuevo |
| update cambia el nombre | **Update** | `findById`, cambiar `name`, `save`, y se lee de nuevo |
| delete elimina el curso | **Delete** | `deleteById`, y `findById` queda vacío |

### `NoteRepositoryTest`
| Prueba | Operación | Cómo |
|---|---|---|
| insert guarda el apunte en estado DRAFT | **Insert** | `save`; el estado inicial es DRAFT |
| find por curso incluye el apunte nuevo | **Find** | `findByCourseId` trae 3: los 2 demo y el nuevo |
| update cambia el estado y guarda el contenido JSONB | **Update** | Estado DRAFT → READY y contenido JSON; se consulta con `content ->> 'summary'` en SQL |
| delete elimina el apunte | **Delete** | `deleteById`, y `findById` queda vacío |

### `EntityRelationshipsTest` (Brandon): relaciones
- **N a N:** usuario ↔ rol.
- **1 a N:** curso → temas, apunte → páginas.
- **N a 1:** matrícula → usuario y curso.
- Cascada y `orphanRemoval`.

---

## 4. Cómo correrlas
- **Todas:** configuración **Tests Backend** en el IDE, o `./gradlew test` en la carpeta del backend.
- **Una sola:** el ▶ verde al lado de la clase o del `fun` en IntelliJ o Android Studio.
- El profe recomendó **IntelliJ** para el backend: se abre la carpeta del backend como proyecto.
- Docker tiene que estar encendido (`docker compose up -d`).

Resultado actual: **45/45 pruebas** en verde.
