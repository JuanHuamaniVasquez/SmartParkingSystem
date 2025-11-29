# SmartParkingSystem

Proyecto para el curso de Bases de Datos, donde se implementa un sistema de gestión de parqueaderos con tarifas dinámicas, control de espacios y reportes básicos.

La aplicación está hecha en **Java + Swing** y se conecta a **PostgreSQL** usando JDBC.

---

## 1. Funcionalidades principales

A nivel funcional, el sistema permite:

- Registrar **entradas** de vehículos:
  - Seleccionando el **tipo de vehículo** (AUTO, MOTO, CAMIONETA).
  - Elegir manualmente el **espacio de estacionamiento** desde un mapa visual.
- Registrar **salidas**:
  - Se calcula automáticamente la tarifa según el tiempo de estadía y el tipo de vehículo.
  - Se registra el **método de pago** (EFECTIVO o TARJETA).
- Ver un **mapa de espacios**:
  - Espacios disponibles en verde.
  - Espacios ocupados en rojo, mostrando abajo el **ID de la transacción** que lo está usando.
- Generar **reportes** (ventana de reportes):
  - Ocupación por parqueadero.
  - Ingresos por día y método de pago.
  - Tiempo promedio de estadía por parqueadero.

A nivel de base de datos se usan:

- **Transacciones** JDBC (commit/rollback).
- **Triggers** en PostgreSQL para:
  - Actualizar disponibilidad de espacios en tiempo real.
  - Calcular tarifas automáticamente.
  - Asignar la tarifa correcta según tipo de vehículo.
  - Generar tickets al cerrar una transacción.
- **Índices** para consultas de ocupación, pagos y tiempos.

---

## 2. Tecnologías usadas

- Lenguaje: **Java** (17 o superior recomendado).
- GUI: **Swing**.
- Base de datos: **PostgreSQL**.
- Conexión: **JDBC**.
- Build: **Maven**.
- IDE recomendado: **VS Code** con extensiones de Java (aunque puede usarse cualquier IDE).

---

## 3. Estructura del proyecto

```text
smartparkingsystem/
├── pom.xml
├── README.md
├── sql/
│   ├── tablas.sql
│   ├── datos.sql
│   └── vistas.sql
└── src/
    └── main/
        └── java/
            └── com/
                └── smartparking/
                    ├── App.java
                    ├── db/
                    │   └── DatabaseConnection.java
                    ├── service/
                    │   └── ParkingService.java
                    └── ui/
                        ├── MainWindow.java
                        └── ReportsWindow.java
```
---

## 4. Configurar la conexión JDBC

Editar:

src/main/java/com/smartparking/db/DatabaseConnection.java
```java
private static final String URL = "jdbc:postgresql://localhost:5432/smartparking";
private static final String USER = "tu_usuario";
private static final String PASSWORD = "tu_password";
```

Ajustar host, puerto, base de datos y credenciales según tu entorno.


