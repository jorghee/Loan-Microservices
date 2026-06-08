# <samp>1. Ejecución del proyecto<samp> :skull:

### Fase 1: Preparación del Entorno

Dado que modificamos el código fuente integrando Temporal, **no podemos usar las imágenes Docker precompiladas** (`roadmaplearner/loans:V1.0`) que vienen en `docker-compose.yml` para los microservicios, ya que no contienen los cambios de Temporal. 

Levantaremos la **infraestructura en Docker**, pero ejecutaremos los **microservicios nativamente** usando Maven para reflejar los cambios.

> [!WARNING]
> Los siguiente comandos utilizan el gestor de paquetes Pacman. Puedes utilizar APT o el gestor de paquetes que usa tu máquina Linux.

**1. Instalar dependencias base (si no las tienes):**
```bash
sudo pacman -Syu
sudo pacman -S docker docker-compose jdk17-openjdk maven curl
```

**2. Iniciar y habilitar el servicio de Docker:**
```bash
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

### Fase 2: Usar Temporal CLI en tu entorno local

1. **Instalar Temporal CLI en Sistemas Linux:
   ```bash
   curl -sSf https://temporal.download/cli.sh | sh
   ```

> [!IMPORTANT]
> El binario se descarga normalmente en la ruta `home/[user]/.temporalio/bin/`. Dicho binario debe ser visible en las variables de entorno de tu sistema operativo. Para ello, debes agregar la ruta al classpath de tu máquina. **Sin embargo**, para una rápida ejecución, puedes exportar directamente el binario ejecutando `export PATH="$PATH:/home/[user]/.temporalio/bin"` en tu terminal. Puedes luego revisas si encuentra el comando ejecutando `temporal --version` en la misma instancia de tu terminal.

2. **Iniciar el servidor local (ejecutar en una terminal independiente):**
   ```bash
   temporal server start-dev
   ```
   *Esto levantará el servidor en `127.0.0.1:7233` (donde ahora apuntan nuestros microservicios) y la Web UI en `http://localhost:8233`.*

### Fase 3: Arranque de Infraestructura y Microservicios

> [!IMPORTANT]
> El proyecto usa `jdk17-openjdk`, es decir, Java Development Kit 17. Las versiones recientes son propensas a errores de compatibilidad.
> Además, es necesario tener instalado Maven como el gestor de dependencias para proyectos Java.

**1. Iniciar la Infraestructura (Temporal + Observabilidad + DBs):**
Abre una terminal, ve a la directorio de tu proyecto y ejecuta:
```bash
cd docker-compose/default
# Levantamos todo EXCEPTO los microservicios Java
docker-compose up -d prometheus tempo grafana alloy minio gateway read write backend
```
*Espera unos 30 segundos para que el entorno de Observabilidad inicie.*

**2. Compilar y arrancar los Microservicios (En orden estricto):**
Abre **4 terminales separadas** (en la raíz del proyecto) y ejecuta los siguientes comandos. *Asegúrate de esperar a que cada uno indique que ha arrancado exitosamente antes de pasar al siguiente.*

*   **Terminal 1 (Config Server):**
    ```bash
    cd configserver
    ./mvnw spring-boot:run
    ```
*   **Terminal 2 (Eureka Server):**
    ```bash
    cd eurekaserver
    ./mvnw spring-boot:run
    ```
*   **Terminal 3 (Accounts - Puerto 8080):** *(Contiene el Worker secundario de Temporal)*
    ```bash
    cd accounts
    ./mvnw spring-boot:run
    ```
*   **Terminal 4 (Loans - Puerto 8090):** *(Contiene el Worker principal de Temporal)*
    ```bash
    cd loans
    ./mvnw spring-boot:run
    ```

> [!NOTE]
> Omitimos `gatewayserver` para las pruebas iniciales y evitar configurar Keycloak, llamaremos directamente a los puertos 8080 y 8090).


# <samp>2. Caso de prueba<samp> :see_no_evil:

## Ejecución de Casos de Prueba (Validación Temporal)

Para que el Workflow de préstamos funcione, el cliente debe existir en `Accounts`.

> [!IMPORTANT]
> Podemos usar Postman para realizar las peticiones HTTP o simplemente usar la herramienta de linea de comando `curl`

#### Paso Previo: Crear Cliente y Cuenta

```bash
curl -X POST http://localhost:8080/api/create \
-H "Content-Type: application/json" \
-d '{
  "name":"Jorge Luis Mamani Huarsaya",
  "email":"jorge.mamani@unsa.edu.pe",
  "mobileNumber":"9999535350"
}'
```
*   **Esperado:** HTTP 201 Created. `{"statusCode":"201","statusMsg":"Account created successfully"}`

#### Caso 1: Flujo Feliz (Auto-aprobación, monto < $50,000)
Vamos a solicitar un préstamo de $40,000. Al ser menor a $50k, el sistema lo aprobará automáticamente y realizará el desembolso.

```bash
curl -X POST "http://localhost:8090/api/applyDistributed" \
     -H "Content-Type: application/json" \
     -d '{"mobileNumber": "9999535350", "totalLoan": 40000}'
```
*   **Esperado Inmediato:** HTTP 202 Accepted. `{"statusCode":"202","statusMsg":"Loan origination workflow started successfully"}`. (El Workflow sigue en background).

#### Caso 2: Flujo con Aprobación Manual (Signal de Temporal, monto > $50,000)
Vamos a solicitar un préstamo de $60,000. El Workflow se pausará esperando un *Signal*.

**Paso A: Iniciar el proceso**
```bash
curl -X POST "http://localhost:8090/api/applyDistributed" \
     -H "Content-Type: application/json" \
     -d '{"mobileNumber": "9999535350", "totalLoan": 60000}'
```
*   **Esperado Inmediato:** HTTP 202. El proceso está pausado en el servidor Temporal (lo veremos en la UI luego).

**Paso B: Aprobar manualmente enviando el Signal**
```bash
curl -X POST "http://localhost:8090/api/approveManual/9999535350"
```
*   **Esperado:** HTTP 200 OK. El Workflow se reanuda, hace el desembolso y finaliza.


## Validación en la Interfaz Web de Temporal

**URL:** [http://localhost:8233](http://localhost:8233)

**Qué observar durante las pruebas:**
1.  **Workflows Tab:** Verás ejecuciones con el nombre `LoanOriginationWorkflow`. Si hiciste el Caso 2 (Paso A), verás que el estado (Status) es **"Running"**. Tras enviar el Signal (Paso B), pasará a **"Completed"**.
2.  **Detalle del Workflow (Haz clic en un ID):**
    *   **Event History:** Esta es la joya de Temporal. Verás eventos como `ActivityTaskScheduled`, `ActivityTaskStarted`, y `ActivityTaskCompleted`.
    *   Observa cómo se intercalan ejecuciones entre diferentes microservicios: verás completarse `evaluateCreditRisk` y luego `disburseFunds`.
    *   **Pending Activities:** Si detienes el microservicio `accounts` (Ctrl+C en su terminal) y lanzas un préstamo, la actividad `disburseFunds` aparecerá aquí. Verás el contador de "Reintentos" subiendo, esperando a que `accounts` vuelva a estar en línea. ¡Esto valida la tolerancia a fallos extrema!
    *   **Queries / Signals:** En la pestaña "Signals" verás registrado el momento exacto en que ingresó `approveManual`.

## Validación en la Plataforma de Observabilidad

> [!IMPORTANT]
> Las siguientes métricas corresponden al entorno local desplegado mediante Docker Compose. Su objetivo es validar que la recolección de logs, métricas y trazas distribuidas funciona correctamente entre los microservicios, la infraestructura de observabilidad y Temporal.

**1. Grafana (Logs, Métricas y Trazas)**
**URL:** [http://localhost:3000](http://localhost:3000)
*(No requiere contraseña por el `GF_AUTH_ANONYMOUS_ENABLED` configurado).*

**A. Verificando Logs con Loki:**
1. Ve al menú lateral (brújula), luego a **Explore**.
2. Arriba a la izquierda, selecciona la fuente de datos: **Loki**.
3. En el campo "Log browser", ingresa esta consulta (LogQL):
   `{compose_service="loans-ms"}` o busca por la palabra clave `roadMapLearner-correlation-id`.
4. **Verificación:** Deberías ver los logs que imprimen los controladores y Temporal, unificados en tiempo real.

**B. Verificando Trazas (Distributed Tracing) con Tempo:**
1. En el mismo **Explore**, cambia la fuente de datos a **Tempo**.
2. Pega el ID de correlación (Correlation ID) que te devuelve el header de cualquier petición de tus APIs, o simplemente ve a la pestaña "Search" y dale a "Run query" para ver las últimas trazas.
3. **Verificación:** Al abrir una traza, verás un diagrama de Gantt. Notarás cuánto tardó la petición HTTP y cuánto tiempo tomaron las transacciones de base de datos internas (gracias a OpenTelemetry).

**2. Prometheus (Métricas Crudas)**
**URL:** [http://localhost:9090](http://localhost:9090)
1. Ve a **Status** y luego a **Targets**.
2. **Verificación:** Comprueba que `accounts`, `loans`, `cards` (que configuraste en el archivo `prometheus.yml`) aparezcan con estado **UP**.
3. Puedes ir a la lupa y buscar `http_server_requests_seconds_count` para ver cuántas peticiones han recibido tus APIs de Spring Boot.
