package com.smartparking.service;

import com.smartparking.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParkingService {

    // DTO para usar en el mapa de espacios en la UI
    public static class EspacioDTO {
        public int espacioId;
        public String numero;
        public boolean disponible;
        public Integer transaccionIdActiva; 

        public EspacioDTO(int espacioId, String numero, boolean disponible, Integer transaccionIdActiva) {
            this.espacioId = espacioId;
            this.numero = numero;
            this.disponible = disponible;
            this.transaccionIdActiva = transaccionIdActiva;
        }
    }

    // Reporte: ocupación por parqueadero
    public static class OcupacionDTO {
        public int parqueaderoId;
        public String nombre;
        public int totalEspacios;
        public int espaciosOcupados;
        public int espaciosLibres;
        public double porcentajeOcupacion;

        public OcupacionDTO(int parqueaderoId, String nombre,
                            int totalEspacios, int espaciosOcupados,
                            int espaciosLibres, double porcentajeOcupacion) {
            this.parqueaderoId = parqueaderoId;
            this.nombre = nombre;
            this.totalEspacios = totalEspacios;
            this.espaciosOcupados = espaciosOcupados;
            this.espaciosLibres = espaciosLibres;
            this.porcentajeOcupacion = porcentajeOcupacion;
        }
    }

    // Reporte: ingresos por día, parqueadero y método de pago
    public static class IngresoDTO {
        public java.time.LocalDate dia;
        public int parqueaderoId;
        public String parqueadero;
        public String metodoPago;
        public double totalDia;

        public IngresoDTO(java.time.LocalDate dia, int parqueaderoId,
                        String parqueadero, String metodoPago, double totalDia) {
            this.dia = dia;
            this.parqueaderoId = parqueaderoId;
            this.parqueadero = parqueadero;
            this.metodoPago = metodoPago;
            this.totalDia = totalDia;
        }
    }

    // Reporte: tiempo promedio de estadía por parqueadero
    public static class EstadiaDTO {
        public int parqueaderoId;
        public String parqueadero;
        public double minutosPromedio;

        public EstadiaDTO(int parqueaderoId, String parqueadero, double minutosPromedio) {
            this.parqueaderoId = parqueaderoId;
            this.parqueadero = parqueadero;
            this.minutosPromedio = minutosPromedio;
        }
    }

    // Helpers internos


    // Busca el vehículo por placa. Si no existe, lo crea y devuelve su ID.

    private int obtenerOVerificarVehiculo(Connection conn, String placa, String tipoVehiculo)
            throws SQLException {

        String selectSql = "SELECT vehiculo_id FROM Vehiculos WHERE placa = ?";

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("vehiculo_id");
                }
            }
        }

        String insertSql = "INSERT INTO Vehiculos (placa, tipo) " +
                           "VALUES (?, ?) " +
                           "RETURNING vehiculo_id";

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, placa);
            ps.setString(2, tipoVehiculo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("vehiculo_id");
                } else {
                    throw new SQLException("No se pudo insertar el vehículo");
                }
            }
        }
    }

   
    // Asigna automáticamente el primer espacio disponible de un parqueadero.

    private int asignarEspacioDisponible(Connection conn, int parqueaderoId) throws SQLException {
        String sql = "SELECT espacio_id " +
                     "FROM Espacios " +
                     "WHERE parqueadero_id = ? AND disponible = TRUE " +
                     "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parqueaderoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("espacio_id");
                } else {
                    throw new SQLException("No hay espacios disponibles en el parqueadero " + parqueaderoId);
                }
            }
        }
    }

    // Listado de espacios para el mapa

    public List<EspacioDTO> listarEspaciosPorParqueadero(int parqueaderoId) throws SQLException {
        String sql = "SELECT e.espacio_id, e.numero_espacio, e.disponible, tr.transaccion_id " +
                    "FROM Espacios e " +
                    "LEFT JOIN Transacciones tr " +
                    "  ON tr.espacio_id = e.espacio_id " +
                    " AND tr.estado = 'ABIERTA' " +
                    "WHERE e.parqueadero_id = ? " +
                    "ORDER BY e.espacio_id";

        List<EspacioDTO> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, parqueaderoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("espacio_id");
                    String num = rs.getString("numero_espacio");
                    boolean disp = rs.getBoolean("disponible");

                    Integer transaccionIdActiva = null;
                    Object obj = rs.getObject("transaccion_id");
                    if (obj != null) {
                        transaccionIdActiva = rs.getInt("transaccion_id");
                    }

                    lista.add(new EspacioDTO(id, num, disp, transaccionIdActiva));
                }
            }
        }

        return lista;
    }


    // Registro de entrada

    public int registrarEntradaAuto(String placa, String tipoVehiculo, int parqueaderoId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int vehiculoId = obtenerOVerificarVehiculo(conn, placa, tipoVehiculo);
                int espacioId = asignarEspacioDisponible(conn, parqueaderoId);

                String insertTransaccion =
                        "INSERT INTO Transacciones (vehiculo_id, espacio_id) " +
                        "VALUES (?, ?) " +
                        "RETURNING transaccion_id";

                int transaccionId;

                try (PreparedStatement ps = conn.prepareStatement(insertTransaccion)) {
                    ps.setInt(1, vehiculoId);
                    ps.setInt(2, espacioId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            transaccionId = rs.getInt("transaccion_id");
                        } else {
                            throw new SQLException("No se pudo crear la transacción de entrada");
                        }
                    }
                }

                conn.commit();
                return transaccionId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Versión "vieja" compatible: por si ya tienes código que llama registrarEntrada().
     * Internamente usa la entrada automática.
     */
    public int registrarEntrada(String placa, String tipoVehiculo, int parqueaderoId) throws SQLException {
        return registrarEntradaAuto(placa, tipoVehiculo, parqueaderoId);
    }


    // Entrada manual: el usuario elige el espacio (por mapa) y se registra ahí.

    public int registrarEntradaEnEspacio(String placa, String tipoVehiculo, int espacioId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int vehiculoId = obtenerOVerificarVehiculo(conn, placa, tipoVehiculo);

                String insertTransaccion =
                        "INSERT INTO Transacciones (vehiculo_id, espacio_id) " +
                        "VALUES (?, ?) " +
                        "RETURNING transaccion_id";

                int transaccionId;

                try (PreparedStatement ps = conn.prepareStatement(insertTransaccion)) {
                    ps.setInt(1, vehiculoId);
                    ps.setInt(2, espacioId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            transaccionId = rs.getInt("transaccion_id");
                        } else {
                            throw new SQLException("No se pudo crear la transacción de entrada");
                        }
                    }
                }

                conn.commit();
                return transaccionId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    // Registro de salida + pagos
    /**
     * Registra la salida de un vehículo, cierra la transacción y crea el pago
     * con el método indicado. Devuelve la tarifa calculada.
     */
    public Double registrarSalida(int transaccionId, String metodoPago) throws SQLException {
    String sqlUpdate = """
        UPDATE Transacciones
        SET estado = 'CERRADA',
            hora_salida = NOW()
        WHERE transaccion_id = ?
          AND estado = 'ABIERTA'
        RETURNING tarifa_calculada
        """;

    String sqlPago = """
        INSERT INTO Pagos (transaccion_id, monto, metodo_pago, fecha_pago)
        VALUES (?, ?, ?, NOW())
        """;

    try (Connection conn = DatabaseConnection.getConnection()) {
        conn.setAutoCommit(false);

        Double tarifa;

        // Intentar cerrar solo si está ABIERTA
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setInt(1, transaccionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    throw new SQLException(
                        "La transacción " + transaccionId + " no existe o ya está CERRADA."
                    );
                }
                tarifa = rs.getDouble("tarifa_calculada");
            }
        }

        // Registrar pago solo si realmente se cerró
        try (PreparedStatement psPago = conn.prepareStatement(sqlPago)) {
            psPago.setInt(1, transaccionId);
            psPago.setDouble(2, tarifa);
            psPago.setString(3, metodoPago);
            psPago.executeUpdate();
        }

        conn.commit();
        return tarifa;

    } catch (SQLException e) {
        throw e;
    }
}


    // Obtener tarifa calculada para una transacción (si ya se cerró)
    public Double obtenerTarifa(int transaccionId) throws SQLException {
        String sql = "SELECT tarifa_calculada FROM Transacciones WHERE transaccion_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transaccionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getObject("tarifa_calculada") == null) {
                        return null;
                    }
                    return rs.getDouble("tarifa_calculada");
                } else {
                    return null;
                }
            }
        }
    }


    // REPORTES

    // Ocupación actual por parqueadero
    public java.util.List<OcupacionDTO> obtenerOcupacionActual() throws SQLException {
        String sql = "SELECT parqueadero_id, nombre, total_espacios, " +
                    "       espacios_ocupados, espacios_libres, porcentaje_ocupacion " +
                    "FROM vw_ocupacion_parqueaderos";

        java.util.List<OcupacionDTO> lista = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("parqueadero_id");
                String nombre = rs.getString("nombre");
                int total = rs.getInt("total_espacios");
                int ocup = rs.getInt("espacios_ocupados");
                int libres = rs.getInt("espacios_libres");
                double porc = rs.getDouble("porcentaje_ocupacion");

                lista.add(new OcupacionDTO(id, nombre, total, ocup, libres, porc));
            }
        }

        return lista;
    }

    // Ingresos por día, parqueadero y método de pago
    public java.util.List<IngresoDTO> obtenerIngresosPorDia() throws SQLException {
        String sql = "SELECT parqueadero_id, parqueadero, dia, metodo_pago, total_dia " +
                    "FROM vw_ingresos_por_dia " +
                    "ORDER BY dia, parqueadero_id, metodo_pago";

        java.util.List<IngresoDTO> lista = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int parqueaderoId = rs.getInt("parqueadero_id");
                String parqueadero = rs.getString("parqueadero");
                java.sql.Date fechaSql = rs.getDate("dia");
                java.time.LocalDate dia = fechaSql.toLocalDate();
                String metodo = rs.getString("metodo_pago");
                double total = rs.getDouble("total_dia");

                lista.add(new IngresoDTO(dia, parqueaderoId, parqueadero, metodo, total));
            }
        }

        return lista;
    }

    // Tiempo promedio de estadía por parqueadero
    public java.util.List<EstadiaDTO> obtenerTiemposPromedioEstadia() throws SQLException {
        String sql = "SELECT parqueadero_id, parqueadero, minutos_promedio " +
                    "FROM vw_tiempo_promedio_estadia " +
                    "ORDER BY parqueadero_id";

        java.util.List<EstadiaDTO> lista = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int parqueaderoId = rs.getInt("parqueadero_id");
                String parqueadero = rs.getString("parqueadero");
                double minutos = rs.getDouble("minutos_promedio");

                lista.add(new EstadiaDTO(parqueaderoId, parqueadero, minutos));
            }
        }

        return lista;
    }

}
