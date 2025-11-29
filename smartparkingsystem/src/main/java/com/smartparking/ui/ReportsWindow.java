package com.smartparking.ui;

import com.smartparking.service.ParkingService;
import com.smartparking.service.ParkingService.OcupacionDTO;
import com.smartparking.service.ParkingService.IngresoDTO;
import com.smartparking.service.ParkingService.EstadiaDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ReportsWindow extends JFrame {

    private final ParkingService parkingService;

    private JTable tblOcupacion;
    private JTable tblIngresos;
    private JTable tblEstadia;

    private DefaultTableModel modeloOcupacion;
    private DefaultTableModel modeloIngresos;
    private DefaultTableModel modeloEstadia;

    public ReportsWindow() {
        super("Reportes - SmartParkingSystem");
        this.parkingService = new ParkingService();

        initComponents();
        setupLayout();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        // Cargar datos iniciales
        cargarOcupacion();
        cargarIngresos();
        cargarEstadias();
    }

    private void initComponents() {
        // Modelo y tabla de Ocupación
        modeloOcupacion = new DefaultTableModel(
                new Object[]{"Parqueadero ID", "Nombre", "Total", "Ocupados", "Libres", "% Ocupación"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblOcupacion = new JTable(modeloOcupacion);

        // Modelo y tabla de Ingresos
        modeloIngresos = new DefaultTableModel(
                new Object[]{"Día", "Parqueadero ID", "Parqueadero", "Método pago", "Total día"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblIngresos = new JTable(modeloIngresos);

        // Modelo y tabla de Estadía
        modeloEstadia = new DefaultTableModel(
                new Object[]{"Parqueadero ID", "Parqueadero", "Minutos promedio"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblEstadia = new JTable(modeloEstadia);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();

        // --------- Tab Ocupación ---------
        JPanel panelOcupacion = new JPanel(new BorderLayout(5, 5));
        JButton btnActualizarOcupacion = new JButton("Actualizar");
        btnActualizarOcupacion.addActionListener(e -> cargarOcupacion());
        JPanel topOcupacion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topOcupacion.add(btnActualizarOcupacion);

        panelOcupacion.add(topOcupacion, BorderLayout.NORTH);
        panelOcupacion.add(new JScrollPane(tblOcupacion), BorderLayout.CENTER);

        tabs.addTab("Ocupación", panelOcupacion);

        // --------- Tab Ingresos ---------
        JPanel panelIngresos = new JPanel(new BorderLayout(5, 5));
        JButton btnActualizarIngresos = new JButton("Actualizar");
        btnActualizarIngresos.addActionListener(e -> cargarIngresos());
        JPanel topIngresos = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topIngresos.add(btnActualizarIngresos);

        panelIngresos.add(topIngresos, BorderLayout.NORTH);
        panelIngresos.add(new JScrollPane(tblIngresos), BorderLayout.CENTER);

        tabs.addTab("Ingresos", panelIngresos);

        // --------- Tab Tiempo de estadía ---------
        JPanel panelEstadia = new JPanel(new BorderLayout(5, 5));
        JButton btnActualizarEstadia = new JButton("Actualizar");
        btnActualizarEstadia.addActionListener(e -> cargarEstadias());
        JPanel topEstadia = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topEstadia.add(btnActualizarEstadia);

        panelEstadia.add(topEstadia, BorderLayout.NORTH);
        panelEstadia.add(new JScrollPane(tblEstadia), BorderLayout.CENTER);

        tabs.addTab("Estadía promedio", panelEstadia);

        add(tabs, BorderLayout.CENTER);
    }

    // Carga de datos para cada reporte
    private void cargarOcupacion() {
        try {
            List<OcupacionDTO> lista = parkingService.obtenerOcupacionActual();
            modeloOcupacion.setRowCount(0);

            for (OcupacionDTO o : lista) {
                modeloOcupacion.addRow(new Object[]{
                        o.parqueaderoId,
                        o.nombre,
                        o.totalEspacios,
                        o.espaciosOcupados,
                        o.espaciosLibres,
                        o.porcentajeOcupacion
                });
            }
        } catch (SQLException ex) {
            mostrarError("Error al obtener ocupación: " + ex.getMessage());
        }
    }

    private void cargarIngresos() {
        try {
            List<IngresoDTO> lista = parkingService.obtenerIngresosPorDia();
            modeloIngresos.setRowCount(0);

            for (IngresoDTO i : lista) {
                modeloIngresos.addRow(new Object[]{
                        i.dia,
                        i.parqueaderoId,
                        i.parqueadero,
                        i.metodoPago,
                        i.totalDia
                });
            }
        } catch (SQLException ex) {
            mostrarError("Error al obtener ingresos: " + ex.getMessage());
        }
    }

    private void cargarEstadias() {
        try {
            List<EstadiaDTO> lista = parkingService.obtenerTiemposPromedioEstadia();
            modeloEstadia.setRowCount(0);

            for (EstadiaDTO e : lista) {
                modeloEstadia.addRow(new Object[]{
                        e.parqueaderoId,
                        e.parqueadero,
                        e.minutosPromedio
                });
            }
        } catch (SQLException ex) {
            mostrarError("Error al obtener tiempos de estadía: " + ex.getMessage());
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
