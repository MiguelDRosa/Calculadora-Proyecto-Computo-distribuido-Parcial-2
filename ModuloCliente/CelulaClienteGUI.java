package ModuloCliente;

import ModuloComun.Mensaje;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CelulaClienteGUI extends JFrame {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JTextField display;

    private String idCliente = "GUI-" + UUID.randomUUID().toString().substring(0, 6);
    private String archivoHistorial = "historial_cliente_" + idCliente + ".txt";

    private ArrayList<JButton> listaBotones = new ArrayList<>();
    private Set<String> solicitudesPendientes = ConcurrentHashMap.newKeySet();

    private String num1 = "";
    private String num2 = "";
    private String operador = "";
    private boolean escribiendoNum2 = false;
    private boolean conectado = false;

    public CelulaClienteGUI(String host, int puerto) {
        crearInterfazGrafica();
        conectarAlNodo(host, puerto);
    }

    private void crearInterfazGrafica() {
        setTitle("Célula GUI: " + idCliente);
        setSize(320, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setResizable(false);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBackground(new Color(44, 62, 80));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));

        display = new JTextField();
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Consolas", Font.BOLD, 28));
        display.setBackground(new Color(236, 240, 241));
        display.setForeground(new Color(44, 62, 80));
        display.setPreferredSize(new Dimension(300, 60));
        panelPrincipal.add(display, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 4, 8, 8));
        grid.setBackground(new Color(44, 62, 80));

        String[] botones = {
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "C", "0", "=", "+"
        };

        for (String texto : botones) {
            JButton btn = new JButton(texto);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder());

            listaBotones.add(btn);

            if (texto.matches("[0-9]")) {
                btn.setBackground(new Color(52, 73, 94));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> agregarNumero(texto));
            } else if (texto.equals("C")) {
                btn.setBackground(new Color(231, 76, 60));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> limpiarCalculadora());
            } else if (texto.equals("=")) {
                btn.setBackground(new Color(46, 204, 113));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> enviarOperacion());
            } else {
                btn.setBackground(new Color(243, 156, 18));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> asignarOperador(texto));
            }
            grid.add(btn);
        }

        panelPrincipal.add(grid, BorderLayout.CENTER);
        add(panelPrincipal);
    }

    private void agregarNumero(String numero) {
        if (escribiendoNum2) {
            num2 += numero;
            display.setText(num1 + " " + operador + " " + num2);
        } else {
            num1 += numero;
            display.setText(num1);
        }
    }

    private void asignarOperador(String simbolo) {
        if (!num1.isEmpty() && num2.isEmpty()) {
            operador = simbolo;
            escribiendoNum2 = true;
            display.setText(num1 + " " + operador + " ");
        }
    }

    private void limpiarCalculadora() {
        num1 = ""; num2 = ""; operador = "";
        escribiendoNum2 = false;
        display.setText("");
        cambiarEstadoBotones(true);
    }

    private void conectarAlNodo(String host, int puerto) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(host, puerto);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                conectado = true;
                guardarHistorial("Conectado al Nodo ADS en puerto " + puerto);
                
                while (true) {
                    Mensaje msg = (Mensaje) in.readObject();

                    
                    if (msg.codigoContenido.endsWith("_RES") && solicitudesPendientes.contains(msg.idSolicitudOriginal)) {
                        solicitudesPendientes.remove(msg.idSolicitudOriginal);
                        
                        SwingUtilities.invokeLater(() -> {
                            String valorFinal = String.valueOf(msg.resultado);
                            if (valorFinal.endsWith(".0")) valorFinal = valorFinal.replace(".0", "");
                            display.setText(valorFinal);
                            num1 = valorFinal;
                            num2 = ""; operador = ""; escribiendoNum2 = false;
                            cambiarEstadoBotones(true);
                        });
                        guardarHistorial("Resultado validado de " + msg.idRemitente + " -> " + msg.resultado);
                    } 
                    else if (msg.codigoContenido.endsWith("_RES") && msg.idRemitente.startsWith("SRV-")) {
                        guardarHistorial("Duplicado ignorado del servidor lento: " + msg.idRemitente);
                    }
                }
            } catch (Exception e) {
                conectado = false;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Conexión perdida con el Nodo."));
            }
        }).start();
    }

    private void enviarOperacion() {
        if (num1.isEmpty() || num2.isEmpty() || operador.isEmpty() || !conectado) return;

        String reqCode = "";
        switch (operador) {
            case "+": reqCode = "SUM_REQ"; break;
            case "-": reqCode = "SUB_REQ"; break;
            case "*": reqCode = "MUL_REQ"; break;
            case "/": reqCode = "DIV_REQ"; break;
        }

        try {
            double n1 = Double.parseDouble(num1);
            double n2 = Double.parseDouble(num2);

            Mensaje peticion = new Mensaje(reqCode, n1, n2, idCliente);
            solicitudesPendientes.add(peticion.idMensaje); 

            out.writeObject(peticion);
            out.flush();
            guardarHistorial("Enviado broadcast: " + reqCode + " [" + n1 + " " + operador + " " + n2 + "]");

            display.setText("Calculando...");
            cambiarEstadoBotones(false);

        } catch (Exception e) {
            display.setText("Error");
            limpiarCalculadora();
        }
    }

    private void cambiarEstadoBotones(boolean estado) {
        for (JButton btn : listaBotones) {
            btn.setEnabled(estado);
        }
    }

    private synchronized void guardarHistorial(String registro) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        try (FileWriter fw = new FileWriter(archivoHistorial, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + fechaHora + "] " + registro);
        } catch (IOException e) { }
    }

    public static void main(String[] args) {
        int puerto = 8001;
        if (args.length > 0) puerto = Integer.parseInt(args[0]);
        
        int finalPuerto = puerto;
        SwingUtilities.invokeLater(() -> new CelulaClienteGUI("localhost", finalPuerto).setVisible(true));
    }
}