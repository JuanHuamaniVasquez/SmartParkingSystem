
-- DATOS INICIALES

-- PARQUEADEROS
INSERT INTO Parqueaderos (nombre, direccion) VALUES
    ('Parqueadero Central', 'Av. Independencia 123'),
    ('Parqueadero Norte',   'Av. Goyoneche 456'),
    ('Parqueadero Sur',     'Av. Ejercito 789');

-- ESPACIOS POR PARQUEADERO
-- Parqueadero 1 - Central
INSERT INTO Espacios (numero_espacio, disponible, parqueadero_id) VALUES
    ('A-1', TRUE, 1),
    ('A-2', TRUE, 1),
    ('A-3', TRUE, 1),
    ('A-4', TRUE, 1),
    ('A-5', TRUE, 1);

-- Parqueadero 2 - Norte
INSERT INTO Espacios (numero_espacio, disponible, parqueadero_id) VALUES
    ('B-1', TRUE, 2),
    ('B-2', TRUE, 2),
    ('B-3', TRUE, 2),
    ('B-4', TRUE, 2);

-- Parqueadero 3 - Sur
INSERT INTO Espacios (numero_espacio, disponible, parqueadero_id) VALUES
    ('C-1', TRUE, 3),
    ('C-2', TRUE, 3),
    ('C-3', TRUE, 3);

-- TARIFAS POR TIPO DE VEHÍCULO
INSERT INTO Tarifas (tipo_vehiculo, tarifa_por_hora) VALUES
    ('AUTO',      5.00),
    ('MOTO',      3.00),
    ('CAMIONETA', 7.50);

-- VEHÍCULOS
INSERT INTO Vehiculos (placa, tipo) VALUES
    ('ABC-123', 'AUTO'),
    ('XYZ-987', 'MOTO'),
    ('KLM-456', 'CAMIONETA');

-- CONSULTAS DE VERIFICACIÓN

-- Ver parqueaderos
SELECT * FROM Parqueaderos;

-- Ver espacios
SELECT espacio_id, numero_espacio, disponible, parqueadero_id
FROM Espacios
ORDER BY parqueadero_id, numero_espacio;

-- Ver tarifas
SELECT * FROM Tarifas;

-- Ver vehículos
SELECT * FROM Vehiculos;

-- Ver todo el movimiento
SELECT * FROM Transacciones;
SELECT * FROM Tickets;
SELECT * FROM Pagos;

INSERT INTO Transacciones (vehiculo_id, espacio_id, hora_entrada, hora_salida, tarifa_calculada, tarifa_id, estado)
VALUES
    (1, 1, '2025-11-20 08:00:00', '2025-11-20 09:00:00', 5.00, 1, 'CERRADA'),
    (1, 2, '2025-11-20 10:00:00', '2025-11-20 12:00:00', 10.00, 1, 'CERRADA'),
    (1, 3, '2025-11-21 12:00:00', '2025-11-21 15:00:00', 15.00, 1, 'CERRADA'),
    (1, 4, '2025-11-21 14:00:00', '2025-11-21 18:00:00', 20.00, 1, 'CERRADA'),
    (1, 5, '2025-11-22 08:00:00', '2025-11-22 09:00:00', 5.00, 1, 'CERRADA'),
    (1, 6, '2025-11-22 10:00:00', '2025-11-22 12:00:00', 10.00, 1, 'CERRADA'),
    (1, 7, '2025-11-23 12:00:00', '2025-11-23 15:00:00', 15.00, 1, 'CERRADA'),
    (1, 8, '2025-11-23 14:00:00', '2025-11-23 18:00:00', 20.00, 1, 'CERRADA'),
    (1, 9, '2025-11-24 08:00:00', '2025-11-24 09:00:00', 5.00, 1, 'CERRADA'),
    (1, 10, '2025-11-24 10:00:00', '2025-11-24 12:00:00', 10.00, 1, 'CERRADA'),
    (2, 11, '2025-11-20 08:00:00', '2025-11-20 09:00:00', 3.00, 2, 'CERRADA'),
    (2, 12, '2025-11-20 10:00:00', '2025-11-20 12:00:00', 6.00, 2, 'CERRADA'),
    (2, 1, '2025-11-21 12:00:00', '2025-11-21 15:00:00', 9.00, 2, 'CERRADA'),
    (2, 2, '2025-11-21 14:00:00', '2025-11-21 18:00:00', 12.00, 2, 'CERRADA'),
    (2, 3, '2025-11-22 08:00:00', '2025-11-22 09:00:00', 3.00, 2, 'CERRADA'),
    (2, 4, '2025-11-22 10:00:00', '2025-11-22 12:00:00', 6.00, 2, 'CERRADA'),
    (2, 5, '2025-11-23 12:00:00', '2025-11-23 15:00:00', 9.00, 2, 'CERRADA'),
    (2, 6, '2025-11-23 14:00:00', '2025-11-23 18:00:00', 12.00, 2, 'CERRADA'),
    (2, 7, '2025-11-24 08:00:00', '2025-11-24 09:00:00', 3.00, 2, 'CERRADA'),
    (2, 8, '2025-11-24 10:00:00', '2025-11-24 12:00:00', 6.00, 2, 'CERRADA'),
    (3, 9, '2025-11-20 08:00:00', '2025-11-20 09:00:00', 7.50, 3, 'CERRADA'),
    (3, 10, '2025-11-20 10:00:00', '2025-11-20 12:00:00', 15.00, 3, 'CERRADA'),
    (3, 11, '2025-11-21 12:00:00', '2025-11-21 15:00:00', 22.50, 3, 'CERRADA'),
    (3, 12, '2025-11-21 14:00:00', '2025-11-21 18:00:00', 30.00, 3, 'CERRADA'),
    (3, 1, '2025-11-22 08:00:00', '2025-11-22 09:00:00', 7.50, 3, 'CERRADA'),
    (3, 2, '2025-11-22 10:00:00', '2025-11-22 12:00:00', 15.00, 3, 'CERRADA'),
    (3, 3, '2025-11-23 12:00:00', '2025-11-23 15:00:00', 22.50, 3, 'CERRADA'),
    (3, 4, '2025-11-23 14:00:00', '2025-11-23 18:00:00', 30.00, 3, 'CERRADA'),
    (3, 5, '2025-11-24 08:00:00', '2025-11-24 09:00:00', 7.50, 3, 'CERRADA'),
    (3, 6, '2025-11-24 10:00:00', '2025-11-24 12:00:00', 15.00, 3, 'CERRADA');
