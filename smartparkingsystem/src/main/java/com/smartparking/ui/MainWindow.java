package com.smartparking.ui;

import com.smartparking.service.ParkingService;
import com.smartparking.service.ParkingService.EspacioDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

public class MainWindow extends JFrame {

    private final ParkingService parkingService;

    // Componentes entrada
    private JTextField txtPlaca;
    private JComboBox<String> cbTipoVehiculo;
    private JTextField txtParqueaderoId;

    // Mapa de espacios
    private JPanel panelMapa;
    private Integer espacioSeleccionadoId = null;

    // Componentes salida/pago
    private JTextField txtTransaccionIdSalida;
    private JComboBox<String> cbMetodoPago;

    // Log
    private JTextArea txtLog;
    private JScrollPane scrollLog;
    private JCheckBox chkMostrarLog;

    public MainWindow() {
        super("SmartParkingSystem");
        this.parkingService = new ParkingService();
        initComponents();
        setupLayout();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 550);
        setLocationRelativeTo(null); // centrar
    }

    private void initComponents() {
        // Campos entrada
        txtPlaca = new JTextField(10);
        cbTipoVehiculo = new JComboBox<>(new String[]{"AUTO", "MOTO", "CAMIONETA"});
        txtParqueaderoId = new JTextField(5);

        // Campos salida/pago
        txtTransaccionIdSalida = new JTextField(6);
        cbMetodoPago = new JComboBox<>(new String[]{"EFECTIVO", "TARJETA"});

        // Mapa de espacios
        panelMapa = new JPanel();
        panelMapa.setBorder(BorderFactory.createTitledBorder("Mapa de espacios"));
        panelMapa.setLayout(new GridLayout(0, 5, 5, 5)); // 5 columnas, filas dinámicas

        // Log
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);

        scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(null);

        chkMostrarLog = new JCheckBox("Mostrar log");
        chkMostrarLog.setSelected(true);
        chkMostrarLog.addActionListener(e -> toggleLogVisibility());
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // =======================
        // Panel de entrada
        // =======================
        JButton btnCargarMapa = new JButton("Cargar mapa");
        btnCargarMapa.addActionListener(this::onCargarMapa);

        JButton btnRegistrarEntrada = new JButton("Registrar entrada");
        btnRegistrarEntrada.addActionListener(this::onRegistrarEntrada);

        JButton btnReportes = new JButton("Ver reportes");
        btnReportes.addActionListener(ev -> {
            ReportsWindow rw = new ReportsWindow();
            rw.setVisible(true);
        });

        // 0 filas => filas dinámicas, 3 columnas fijas
        JPanel panelEntrada = new JPanel(new GridLayout(0, 3, 5, 5));
        panelEntrada.setBorder(BorderFactory.createTitledBorder("Registrar entrada"));

        // Fila 1: Placa + campo + Ver reportes
        panelEntrada.add(new JLabel("Placa:"));
        panelEntrada.add(txtPlaca);
        panelEntrada.add(btnReportes);

        // Fila 2: Tipo vehículo + combo + vacío
        panelEntrada.add(new JLabel("Tipo vehículo:"));
        panelEntrada.add(cbTipoVehiculo);
        panelEntrada.add(new JLabel(""));

        // Fila 3: Parqueadero ID + campo + Cargar mapa
        panelEntrada.add(new JLabel("Parqueadero ID:"));
        panelEntrada.add(txtParqueaderoId);
        panelEntrada.add(btnCargarMapa);

        // Fila 4: vacío + Registrar entrada + vacío
        panelEntrada.add(new JLabel(""));
        panelEntrada.add(btnRegistrarEntrada);
        panelEntrada.add(new JLabel(""));

        // =======================
        // Panel de salida/pago
        // =======================
        JButton btnRegistrarSalida = new JButton("Registrar salida");
        btnRegistrarSalida.addActionListener(this::onRegistrarSalida);

        JPanel panelSalida = new JPanel(new GridLayout(3, 2, 5, 5));
        panelSalida.setBorder(BorderFactory.createTitledBorder("Registrar salida"));

        panelSalida.add(new JLabel("Transacción ID:"));
        panelSalida.add(txtTransaccionIdSalida);

        panelSalida.add(new JLabel("Método de pago:"));
        panelSalida.add(cbMetodoPago);

        panelSalida.add(new JLabel(""));
        panelSalida.add(btnRegistrarSalida);

        // =======================
        // Centro: mapa + salida
        // =======================
        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.add(panelMapa, BorderLayout.CENTER);
        panelCentro.add(panelSalida, BorderLayout.SOUTH);

        // =======================
        // Panel de log colapsable
        // =======================
        JPanel panelLog = new JPanel(new BorderLayout(5, 5));
        panelLog.setBorder(BorderFactory.createTitledBorder("Log"));
        panelLog.add(chkMostrarLog, BorderLayout.NORTH);
        panelLog.add(scrollLog, BorderLayout.CENTER);

        // =======================
        // Añadir todo al frame
        // =======================
        add(panelEntrada, BorderLayout.NORTH);
        add(panelCentro, BorderLayout.CENTER);
        add(panelLog, BorderLayout.SOUTH);
    }

    // ==============================
    // Handlers
    // ==============================

    private void onCargarMapa(ActionEvent e) {
        String parqueaderoStr = txtParqueaderoId.getText().trim();
        if (parqueaderoStr.isEmpty()) {
            appendLog("Ingresa un ID de parqueadero para cargar el mapa.");
            return;
        }

        int parqueaderoId;
        try {
            parqueaderoId = Integer.parseInt(parqueaderoStr);
        } catch (NumberFormatException ex) {
            appendLog("Parqueadero ID debe ser numérico.");
            return;
        }

        try {
            List<EspacioDTO> espacios = parkingService.listarEspaciosPorParqueadero(parqueaderoId);
            panelMapa.removeAll();
            espacioSeleccionadoId = null;

            if (espacios.isEmpty()) {
                appendLog("No hay espacios registrados para el parqueadero " + parqueaderoId);
            }

            for (EspacioDTO esp : espacios) {
                // Texto del botón: número de espacio y, si está ocupado, id de transacción
                String textoBoton;
                if (!esp.disponible && esp.transaccionIdActiva != null) {
                    textoBoton = "<html>" + esp.numero + "<br/>T: " + esp.transaccionIdActiva + "</html>";
                } else {
                    textoBoton = esp.numero;
                }

                JButton btn = new JButton(textoBoton);
                btn.setOpaque(true);
                btn.setBorderPainted(true);

                if (esp.disponible) {
                    btn.setBackground(new Color(144, 238, 144)); // verde claro
                    btn.setEnabled(true);
                } else {
                    btn.setBackground(new Color(255, 160, 122)); // rojo/anaranjado
                    btn.setEnabled(false);
                    if (esp.transaccionIdActiva != null) {
                        btn.setToolTipText("Ocupado por transacción " + esp.transaccionIdActiva);
                    }
                }

                btn.addActionListener(ev -> {
                    espacioSeleccionadoId = esp.espacioId;
                    appendLog("Espacio seleccionado: " + esp.numero +
                              " (espacio_id=" + esp.espacioId +
                              (esp.transaccionIdActiva != null ? ", transacción=" + esp.transaccionIdActiva : "") +
                              ")");
                });

                panelMapa.add(btn);
            }

            panelMapa.revalidate();
            panelMapa.repaint();

        } catch (SQLException ex) {
            appendLog("Error al cargar espacios: " + ex.getMessage());
        }
    }

    private void onRegistrarEntrada(ActionEvent e) {
        String placa = txtPlaca.getText().trim();
        String tipo = (String) cbTipoVehiculo.getSelectedItem();

        if (placa.isEmpty() || tipo == null) {
            appendLog("Faltan datos para registrar la entrada (placa/tipo).");
            return;
        }
        if (espacioSeleccionadoId == null) {
            appendLog("Selecciona un espacio en el mapa antes de registrar la entrada.");
            return;
        }

        try {
            int transaccionId = parkingService.registrarEntradaEnEspacio(placa, tipo, espacioSeleccionadoId);
            appendLog("Entrada registrada. Transacción ID = " + transaccionId +
                      ", espacio_id = " + espacioSeleccionadoId);
            txtTransaccionIdSalida.setText(String.valueOf(transaccionId));

            // Actualizar mapa para reflejar que se ocupó el espacio
            onCargarMapa(null);

        } catch (SQLException ex) {
            appendLog("Error BD al registrar entrada: " + ex.getMessage());
        }
    }

    private void onRegistrarSalida(ActionEvent e) {
        String transaccionStr = txtTransaccionIdSalida.getText().trim();
        String metodoPago = (String) cbMetodoPago.getSelectedItem();

        if (transaccionStr.isEmpty()) {
            appendLog("Ingresa un ID de transacción para registrar la salida.");
            return;
        }
        if (metodoPago == null) {
            appendLog("Selecciona un método de pago.");
            return;
        }

        int transaccionId;
        try {
            transaccionId = Integer.parseInt(transaccionStr);
        } catch (NumberFormatException ex) {
            appendLog("Transacción ID debe ser numérico.");
            return;
        }

        try {
            Double tarifa = parkingService.registrarSalida(transaccionId, metodoPago);
            appendLog("Salida registrada. Transacción " + transaccionId +
                      " - Método: " + metodoPago +
                      " - Tarifa: " + tarifa);

            // Después de salir, refrescar el mapa
            onCargarMapa(null);

        } catch (SQLException ex) {
            appendLog("Error BD al registrar salida: " + ex.getMessage());
        }
    }

    // ==============================
    // Utilidad
    // ==============================

    private void toggleLogVisibility() {
        boolean visible = chkMostrarLog.isSelected();
        scrollLog.setVisible(visible);
        scrollLog.getParent().revalidate();
        scrollLog.getParent().repaint();
    }

    private void appendLog(String msg) {
        txtLog.append(msg + "\n");
    }
}
