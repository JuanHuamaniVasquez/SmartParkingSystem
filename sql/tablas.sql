-- TABLAS

CREATE TABLE Tarifas (
    tarifa_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_vehiculo VARCHAR(20) NOT NULL,
    tarifa_por_hora NUMERIC(10,2) NOT NULL CHECK (tarifa_por_hora >= 0)
);

CREATE TABLE Vehiculos (
    vehiculo_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL
);

CREATE TABLE Parqueaderos (
    parqueadero_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200) NOT NULL
);

CREATE TABLE Espacios (
    espacio_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_espacio  VARCHAR(20) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    parqueadero_id  INT NOT NULL REFERENCES Parqueaderos(parqueadero_id),
    CONSTRAINT uq_espacio_por_parqueadero
        UNIQUE (parqueadero_id, numero_espacio)
);

CREATE TABLE Transacciones (
    transaccion_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehiculo_id INT NOT NULL REFERENCES Vehiculos(vehiculo_id),
    espacio_id INT NOT NULL REFERENCES Espacios(espacio_id),
    hora_entrada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hora_salida TIMESTAMP,
    tarifa_calculada NUMERIC(10,2),
    tarifa_id INT REFERENCES Tarifas(tarifa_id),
    estado VARCHAR(10) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA'))
);

CREATE TABLE Tickets (
    ticket_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaccion_id INT NOT NULL UNIQUE REFERENCES Transacciones(transaccion_id),
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    codigo VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE Pagos (
    pago_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaccion_id INT NOT NULL REFERENCES Transacciones(transaccion_id),
    monto NUMERIC(10,2) NOT NULL CHECK (monto >= 0), 
    metodo_pago VARCHAR(20) NOT NULL,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_metodo_pago CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA'))
);


-- ÍNDICES

-- Espacios disponibles por parqueadero
CREATE INDEX idx_espacios_disponibles ON Espacios (parqueadero_id, disponible);

-- Transacciones por estado (ABIERTA/CERRADA)
CREATE INDEX idx_transacciones_estado ON Transacciones (estado);

-- Para reportes por fecha de entrada/salida (tiempos promedio, ocupación, ingresos)
CREATE INDEX idx_transacciones_hora_entrada ON Transacciones (hora_entrada);

CREATE INDEX idx_transacciones_hora_salida ON Transacciones (hora_salida);

-- Para pagos por transacción (consultas rápidas)
CREATE INDEX idx_pagos_transaccion ON Pagos (transaccion_id);


-- FUNCIONES Y TRIGGERS

-- 1) Verificar que el espacio no tenga otra transacción ABIERTA
CREATE OR REPLACE FUNCTION verificar_espacio_disponible()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM Transacciones
        WHERE espacio_id = NEW.espacio_id
          AND estado = 'ABIERTA'
    ) THEN
        RAISE EXCEPTION 'El espacio % ya tiene una transacción ABIERTA', NEW.espacio_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verificar_espacio_disponible
BEFORE INSERT ON Transacciones
FOR EACH ROW
EXECUTE FUNCTION verificar_espacio_disponible();

-- 2) Actualizar disponibilidad del espacio en tiempo real
CREATE OR REPLACE FUNCTION actualizar_disponibilidad_espacio()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado = 'ABIERTA' THEN
            UPDATE Espacios
            SET disponible = FALSE
            WHERE espacio_id = NEW.espacio_id;
        END IF;

    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.estado <> NEW.estado THEN
            IF NEW.estado = 'ABIERTA' THEN
                UPDATE Espacios
                SET disponible = FALSE
                WHERE espacio_id = NEW.espacio_id;
            ELSIF NEW.estado = 'CERRADA' THEN
                UPDATE Espacios
                SET disponible = TRUE
                WHERE espacio_id = NEW.espacio_id;
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_actualizar_disponibilidad_espacio
AFTER INSERT OR UPDATE ON Transacciones
FOR EACH ROW
EXECUTE FUNCTION actualizar_disponibilidad_espacio();

-- 3) Función para calcular tarifa por tiempo
CREATE OR REPLACE FUNCTION calcular_tarifa(
    p_vehiculo_id INT,
    p_hora_entrada TIMESTAMP,
    p_hora_salida TIMESTAMP
) 
RETURNS NUMERIC(10,2) AS $$
DECLARE
    v_tipo_vehiculo VARCHAR(20);
    v_tarifa_por_hora NUMERIC(10,2);
    v_duracion_horas NUMERIC;
    v_monto NUMERIC(10,2);
BEGIN
    IF p_hora_salida IS NULL THEN
        RAISE EXCEPTION 'No se puede calcular la tarifa sin hora de salida';
    END IF;

    IF p_hora_salida < p_hora_entrada THEN
        RAISE EXCEPTION 'La hora de salida (%) no puede ser anterior a la hora de entrada (%)',
            p_hora_salida, p_hora_entrada;
    END IF;

    SELECT tipo
    INTO v_tipo_vehiculo
    FROM Vehiculos
    WHERE vehiculo_id = p_vehiculo_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Vehículo % no encontrado', p_vehiculo_id;
    END IF;

    SELECT tarifa_por_hora
    INTO v_tarifa_por_hora
    FROM Tarifas
    WHERE tipo_vehiculo = v_tipo_vehiculo
    ORDER BY tarifa_id
    LIMIT 1;

    IF v_tarifa_por_hora IS NULL THEN
        RAISE EXCEPTION 'No hay tarifa configurada para tipo de vehículo %', v_tipo_vehiculo;
    END IF;

    v_duracion_horas :=
        CEIL(EXTRACT(EPOCH FROM (p_hora_salida - p_hora_entrada)) / 3600.0);

    IF v_duracion_horas < 1 THEN
        v_duracion_horas := 1;
    END IF;

    v_monto := v_duracion_horas * v_tarifa_por_hora;

    RETURN v_monto;
END;
$$ LANGUAGE plpgsql;

-- 4) Trigger: setear tarifa_calculada al cerrar transacción
CREATE OR REPLACE FUNCTION set_tarifa_calculada_transaccion()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'CERRADA' THEN
        IF NEW.hora_salida IS NULL THEN
            NEW.hora_salida := CURRENT_TIMESTAMP;
        END IF;

        NEW.tarifa_calculada :=
            calcular_tarifa(
                NEW.vehiculo_id,
                NEW.hora_entrada,
                NEW.hora_salida
            );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_tarifa_calculada
BEFORE UPDATE ON Transacciones
FOR EACH ROW
EXECUTE FUNCTION set_tarifa_calculada_transaccion();

-- 5) Trigger: setear tarifa_id automáticamente según tipo de vehículo
CREATE OR REPLACE FUNCTION set_tarifa_id_transaccion()
RETURNS TRIGGER AS $$
DECLARE
    v_tarifa_id INT;
BEGIN
    IF NEW.tarifa_id IS NULL THEN
        SELECT t.tarifa_id
        INTO v_tarifa_id
        FROM Tarifas t
        JOIN Vehiculos v ON v.tipo = t.tipo_vehiculo
        WHERE v.vehiculo_id = NEW.vehiculo_id
        ORDER BY t.tarifa_id
        LIMIT 1;

        IF v_tarifa_id IS NULL THEN
            RAISE EXCEPTION 'No hay tarifa configurada para el tipo de vehículo del vehiculo_id %', NEW.vehiculo_id;
        END IF;

        NEW.tarifa_id := v_tarifa_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_tarifa_id
BEFORE INSERT ON Transacciones
FOR EACH ROW
EXECUTE FUNCTION set_tarifa_id_transaccion();

-- 6) Trigger: crear Ticket al cerrar la transacción
CREATE OR REPLACE FUNCTION crear_ticket_al_cerrar_transaccion()
RETURNS TRIGGER AS $$
DECLARE
    v_codigo VARCHAR(30);
BEGIN
    IF OLD.estado = 'ABIERTA' AND NEW.estado = 'CERRADA' THEN
        IF NOT EXISTS (
            SELECT 1 FROM Tickets
            WHERE transaccion_id = NEW.transaccion_id
        ) THEN
            v_codigo := 'T-' || NEW.transaccion_id::TEXT;

            INSERT INTO Tickets (transaccion_id, fecha_emision, codigo)
            VALUES (NEW.transaccion_id, CURRENT_TIMESTAMP, v_codigo);
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_crear_ticket_al_cerrar
AFTER UPDATE ON Transacciones
FOR EACH ROW
EXECUTE FUNCTION crear_ticket_al_cerrar_transaccion();
