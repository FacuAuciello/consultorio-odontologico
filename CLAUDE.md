# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es

Sistema de gestión de un consultorio odontológico (agenda de turnos, pacientes e historia clínica, con login). Backend en `backend/` (Java 17 + Spring Boot 4) y frontend en `frontend/` (React 19 + TypeScript + Vite). Se desarrolló para un cliente real y se entregó, pero el cliente finalmente no lo puso en uso: **no hay producción**, solo el modo demo con H2 y datos ficticios. El detalle de la arquitectura está en el `README.md`.

## Comandos

Backend (desde `backend/`, con el wrapper de Maven):

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` — H2 en memoria, datos ficticios, puerto 8080. Usuario `admin` y contraseña `demo1234` (solo existen en el perfil `demo`).
- `./mvnw test` — todos los tests. No necesitan MySQL ni variables de entorno (usan el perfil `demo`).
- `./mvnw test -Dtest=NombreDelTest` — un test o una clase.

Frontend (desde `frontend/`):

- `npm run dev` — servidor de Vite en el puerto 5173 (`strictPort`: si está ocupado, falla en vez de cambiar de puerto). El proxy reenvía `/api` al backend (`VITE_PROXY_TARGET`, por defecto `http://localhost:8080`).
- `npm test` — Vitest.
- `npm run build` — `tsc --noEmit && vite build`. **Un error de tipos en un archivo `.test.ts` también rompe el build**, así que hay que revisarlo aunque los tests pasen.

## Java 17 a propósito

El proyecto está en Java 17 y tiene que seguir compilando en 17 (se eligió para mostrar que se manejan distintas versiones; el otro proyecto, Conectar Docentes, usa Java 25). No usar APIs de Java 18 o posteriores: por ejemplo `Locale.of(...)` es de Java 19, por eso el código usa `Locale.forLanguageTag("es-AR")`. Los records, los text blocks y el `instanceof` con patrón sí están permitidos. Si el JDK por defecto no es 17, apuntar `JAVA_HOME` a un JDK 17 solo para ese comando (por ejemplo, el `openjdk@17` de Homebrew). Los tests pasan con Java 17 y con Java 25.

## Cosas que ya costaron tiempo

- **CORS y el puerto del front:** el backend solo acepta pedidos del origen `CORS_ORIGEN` (por defecto `http://localhost:5173`). Si el front corre en otro puerto, el login falla con `403 Invalid CORS request`. Hay que arrancar el backend con `CORS_ORIGEN=http://localhost:<puerto>`. El proxy de Vite no lo evita (el navegador igual manda el `Origin`), aunque el comentario de `vite.config.ts` diga lo contrario.
- **Los datos de demo son relativos a la semana actual** (`CargadorDatosDemo`). Por eso los tests de API usan lunes de 2031 y DNI propios (`ApiTestBase`) y los de repositorio usan fechas de 2032: así no dependen del día en que se corren ni de la demo.
- **Si se prueba a mano contra el backend en marcha**, los turnos o pacientes que se creen quedan en memoria hasta reiniciarlo. Reiniciarlo antes de sacar capturas, para que no aparezcan datos de pruebas.
- **Tests de Spring Boot 4:** no hace falta agregar dependencias. Se usa `@SpringBootTest` con `@AutoConfigureMockMvc` y el perfil `demo`. Ojo: en Boot 4 `AutoConfigureMockMvc` está en `org.springframework.boot.webmvc.test.autoconfigure`.

## Reglas de la agenda y concurrencia

- Las reglas (días laborales, horario, duraciones permitidas, grilla de 30 minutos y sin superposición) viven en `TurnoService` y se configuran en `application.properties` (`consultorio.agenda.*`). El front las lee de `GET /api/agenda/configuracion`.
- **Crear o mover un turno usa un cerrojo** (`ReentrantLock`) que envuelve toda la transacción de escritura (`TransactionTemplate`), y los métodos públicos llevan `Propagation.NOT_SUPPORTED` para que la transacción de solo lectura de la clase no se abra antes del cerrojo. No quitarlo ni mover la transacción afuera: `TurnoConcurrenciaTest` falla sin él. Protege mientras haya **un solo servidor**; con varios haría falta un bloqueo en la base de datos (no se pudo probar contra MySQL real).
- `Isolation.SERIALIZABLE` se probó y no alcanza en H2.

## Convenciones del código

- Nombres en español y descriptivos; interfaces de repositorio con prefijo `I`; los services no tienen interfaz.
- Comentarios en español, sin tildes, con formato `// texto` (con espacio), que explican **el porqué** y no lo que el código ya dice.
- La entidad nunca se expone: la API usa DTOs (`dto/request` con validaciones y `dto/response`) y mappers estáticos.
- Los errores salen siempre como `{ status, error, message }` desde `ManejadorGlobalExcepciones`; las excepciones de negocio heredan de `ConsultorioException`, que ya trae su código HTTP.
- Frontend: todas las llamadas pasan por `src/api/cliente.ts` (cookie de sesión, errores como `ApiError`, aviso al volver un 401). Las reglas de validación del front son para responder rápido; la que cuenta es la del servidor.

## Privacidad

- **Nunca** usar el nombre real del dentista ni datos reales de pacientes. El profesional es ficticio (`Dr. Demo (ficticio)` en `frontend/src/config/consultorio.ts` y `.env.example`).
- No subir `.env`. Las claves salen de variables de entorno y no tienen valor por defecto.
- Las capturas de `docs/` se rehacen con el backend en modo demo recién iniciado y el front con el nombre ficticio.

## Pendiente

- La demo del portfolio (front con datos simulados, publicado como sitio aparte).
- Borrado lógico de pacientes (hoy se borran para siempre junto con su historia clínica; cambia el modelo de datos).
