
-- VISTAS DE REPORTES

-- 1) Ocupación actual por parqueadero
-- Usa la tabla Espacios (aprovecha el índice idx_espacios_disponibles)
CREATE OR REPLACE VIEW vw_ocupacion_parqueaderos AS
SELECT
    p.parqueadero_id,
    p.nombre,
    COUNT(e.espacio_id) AS total_espacios,
    COUNT(*) FILTER (WHERE e.disponible = FALSE) AS espacios_ocupados,
    COUNT(*) FILTER (WHERE e.disponible = TRUE)  AS espacios_libres,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE e.disponible = FALSE)
        / NULLIF(COUNT(e.espacio_id), 0),
        2
    ) AS porcentaje_ocupacion
FROM Parqueaderos p
JOIN Espacios e ON e.parqueadero_id = p.parqueadero_id
GROUP BY p.parqueadero_id, p.nombre
ORDER BY p.parqueadero_id;


-- 2) Ingresos por día, parqueadero y método de pago
-- Usa Pagos + Transacciones + Espacios + Parqueaderos
-- Se apoya en idx_pagos_transaccion y en los índices de hora si filtras por rango
CREATE OR REPLACE VIEW vw_ingresos_por_dia AS
SELECT
    p.parqueadero_id,
    p.nombre AS parqueadero,
    pg.fecha_pago::date AS dia,
    pg.metodo_pago,
    SUM(pg.monto) AS total_dia
FROM Pagos pg
JOIN Transacciones t ON t.transaccion_id = pg.transaccion_id
JOIN Espacios e       ON e.espacio_id = t.espacio_id
JOIN Parqueaderos p   ON p.parqueadero_id = e.parqueadero_id
GROUP BY p.parqueadero_id, p.nombre, dia, pg.metodo_pago
ORDER BY dia, p.parqueadero_id, pg.metodo_pago;


-- 3) Tiempo promedio de estadía (en minutos) por parqueadero
-- Usa Transacciones + Espacios + Parqueaderos
-- Se apoya en idx_transacciones_hora_entrada / hora_salida
CREATE OR REPLACE VIEW vw_tiempo_promedio_estadia AS
SELECT
    p.parqueadero_id,
    p.nombre AS parqueadero,
    AVG(EXTRACT(EPOCH FROM (t.hora_salida - t.hora_entrada)) / 60.0) AS minutos_promedio
FROM Transacciones t
JOIN Espacios e     ON e.espacio_id = t.espacio_id
JOIN Parqueaderos p ON p.parqueadero_id = e.parqueadero_id
WHERE t.estado = 'CERRADA'
  AND t.hora_salida IS NOT NULL
GROUP BY p.parqueadero_id, p.nombre
ORDER BY p.parqueadero_id;


SELECT * FROM vw_ocupacion_parqueaderos;
SELECT * FROM vw_ingresos_por_dia;
SELECT * FROM vw_tiempo_promedio_estadia;
