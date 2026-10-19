package ModuloServidor;

import ModuloComun.Mensaje;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CelulaServidor {
    private String tipoOperacion; // "+", "-", "*", "/"
    private String reqCode;       // Ej: SUM_REQ
    private String resCode;       // Ej: SUM_RES
    
    private String idServidor;
    private String archivoHistorial;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public CelulaServidor(String tipoOperacion, String host, int puertoNodo) {
        this.tipoOperacion = tipoOperacion;
        this.idServidor = "SRV-" + tipoOperacion + "-" + UUID.randomUUID().toString().substring(0, 5);
        this.archivoHistorial = "historial_srv_" + idServidor + ".txt";
        
        configurarCodigos();
        System.out.println("==========================================");
        System.out.println("   CÉLULA SERVIDOR [" + tipoOperacion + "] INICIADA");
        System.out.println("   ID: " + idServidor);
        System.out.println("==========================================");
        
        conectarAlNodo(host, puertoNodo);
    }

    // Configura los Códigos de Contenido exactos que pide el PDF
    private void configurarCodigos() {
        switch (tipoOperacion) {
            case "+": reqCode = "SUM_REQ"; resCode = "SUM_RES"; break;
            case "-": reqCode = "SUB_REQ"; resCode = "SUB_RES"; break;
            case "*": reqCode = "MUL_REQ"; resCode = "MUL_RES"; break;
            case "/": reqCode = "DIV_REQ"; resCode = "DIV_RES"; break;
            default:
                System.out.println("Operación no válida. Usa: +, -, *, /");
                System.exit(1);
        }
    }

    private void conectarAlNodo(String host, int puerto) {
        try {
            Socket socket = new Socket(host, puerto);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            guardarHistorial("Conectado exitosamente al Nodo en puerto " + puerto);
            System.out.println("[*] Conectado al Campo de Datos. Escuchando peticiones: " + reqCode);
            
            escucharCampoDeDatos();
        } catch (IOException e) {
            System.err.println("[-] No se pudo conectar al Nodo ADS en " + host + ":" + puerto);
            guardarHistorial("Error de conexión al nodo.");
        }
    }

    private void escucharCampoDeDatos() {
        try {
            while (true) {
                Mensaje msg = (Mensaje) in.readObject();
                
                // ¿El mensaje es una petición y es de mi especialidad?
                if (msg.codigoContenido.equals(reqCode)) {
                    guardarHistorial("PETICIÓN ATRAPADA: " + msg.idMensaje + " de " + msg.idRemitente);
                    System.out.println("\n[!] Resolviendo petición: " + msg.num1 + " " + tipoOperacion + " " + msg.num2);
                    
                    double resultado = calcular(msg.num1, msg.num2);
                    
                    // Crear mensaje de respuesta
                    Mensaje respuesta = new Mensaje(resCode, msg.idMensaje, resultado, idServidor);
                    
                    out.writeObject(respuesta);
                    out.flush();
                    
                    guardarHistorial("RESPUESTA ENVIADA: " + respuesta.resultado);
                    System.out.println("[+] Respuesta enviada al Campo de Datos.");
                }
            }
        } catch (Exception e) {
            System.err.println("[-] Desconectado del Campo de Datos.");
            guardarHistorial("Desconectado del nodo.");
        }
    }

    private double calcular(double n1, double n2) {
        switch (tipoOperacion) {
            case "+": return n1 + n2;
            case "-": return n1 - n2;
            case "*": return n1 * n2;
            case "/": 
                if (n2 == 0) {
                    System.out.println("    -> Error: División entre cero.");
                    return Double.NaN; // Representa error numérico
                }
                return n1 / n2;
        }
        return 0;
    }

    private synchronized void guardarHistorial(String evento) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        try (FileWriter fw = new FileWriter(archivoHistorial, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + fechaHora + "] " + evento);
        } catch (IOException e) {
            System.err.println("Error al escribir el historial: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String op = "+";
        int puerto = 8001;
        
        if (args.length >= 2) {
            op = args[0];
            puerto = Integer.parseInt(args[1]);
        } else {
            // Si lo ejecutas desde el botón "Run", te pregunta con una ventana
            String[] opciones = {"+", "-", "*", "/"};
            op = (String) javax.swing.JOptionPane.showInputDialog(
                    null, 
                    "Selecciona la operación de este servidor:", 
                    "Configurar Célula Servidor", 
                    javax.swing.JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    opciones, 
                    opciones[0]);
            
            // Si el usuario cancela la ventana, cerramos
            if (op == null) {
                System.exit(0);
            }
        }
        
        new CelulaServidor(op, "localhost", puerto);
    }
}