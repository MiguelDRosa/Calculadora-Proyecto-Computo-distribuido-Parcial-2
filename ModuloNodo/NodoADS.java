package ModuloNodo;

import ModuloComun.Mensaje;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class NodoADS {
    private int puerto;
    private String archivoHistorial;
    
    // Lista segura para guardar a todos los conectados (otras células u otros nodos)
    private List<ObjectOutputStream> conexionesActivas = new CopyOnWriteArrayList<>();
    
    // Set seguro para recordar los mensajes que ya vimos (PREVENCIÓN DE CICLOS)
    private Set<String> mensajesProcesados = ConcurrentHashMap.newKeySet();

    public NodoADS(int puerto) {
        this.puerto = puerto;
        this.archivoHistorial = "historial_nodo_" + puerto + ".txt";
    }

    public void iniciar() {
        System.out.println("==========================================");
        System.out.println("   NODO ADS INICIADO EN PUERTO: " + puerto);
        System.out.println("==========================================");
        guardarEnHistorial("NODO INICIADO - Escuchando en puerto " + puerto);

        // Hilo principal para aceptar conexiones entrantes
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(puerto)) {
                while (true) {
                    Socket cliente = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(cliente.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(cliente.getInputStream());
                    
                    conexionesActivas.add(out);
                    guardarEnHistorial("NUEVA CONEXIÓN RECIBIDA.");
                    System.out.println("[+] Nueva conexión establecida. Total conectados: " + conexionesActivas.size());
                    
                    // Iniciar un hilo para escuchar a esta nueva conexión
                    new Thread(() -> escucharConexion(in, out)).start();
                }
            } catch (IOException e) {
                System.err.println("Error en el Nodo: " + e.getMessage());
            }
        }).start();
    }

    // Método para conectar este nodo con otros nodos de la malla
    public void conectarConOtroNodo(String host, int puertoDestino) {
        try {
            Socket socket = new Socket(host, puertoDestino);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            conexionesActivas.add(out);
            guardarEnHistorial("CONECTADO COMO CLIENTE AL NODO EN PUERTO " + puertoDestino);
            System.out.println("[*] Conectado exitosamente a nodo en " + puertoDestino);
            
            new Thread(() -> escucharConexion(in, out)).start();
        } catch (Exception e) {
            System.out.println("[-] No se pudo conectar al puerto " + puertoDestino + ". Quiza no ha encendido.");
        }
    }

    // Método que escucha los mensajes entrantes
    private void escucharConexion(ObjectInputStream in, ObjectOutputStream outPropio) {
        try {
            while (true) {
                Mensaje msg = (Mensaje) in.readObject();
                
                // PREVENCIÓN DE CICLOS: ¿Ya vi este mensaje antes?
                if (mensajesProcesados.contains(msg.idMensaje)) {
                    guardarEnHistorial("CICLO EVITADO: Mensaje duplicado ignorado -> " + msg.idMensaje);
                    continue; // Lo ignora y no hace broadcast
                }

                // Si es nuevo, lo registro y hago broadcast
                mensajesProcesados.add(msg.idMensaje);
                guardarEnHistorial("BROADCAST: " + msg.toString());
                System.out.println("[BROADCAST] " + msg.codigoContenido + " de " + msg.idRemitente);
                
                difundirMensaje(msg, outPropio);
            }
        } catch (Exception e) {
            conexionesActivas.remove(outPropio);
            System.out.println("[-] Conexión perdida. Total conectados: " + conexionesActivas.size());
        }
    }

    // Enviar el mensaje a todos los conectados EXCEPTO al que nos lo acaba de enviar
    private void difundirMensaje(Mensaje msg, ObjectOutputStream outOrigen) {
        for (ObjectOutputStream out : conexionesActivas) {
            if (out != outOrigen) {
                try {
                    out.writeObject(msg);
                    out.flush();
                } catch (IOException e) {
                    conexionesActivas.remove(out);
                }
            }
        }
    }

    // Reutilizamos tu método exacto del primer parcial para los TXT
    private synchronized void guardarEnHistorial(String evento) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (FileWriter fw = new FileWriter(archivoHistorial, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + fechaHora + "] " + evento);
        } catch (IOException e) {
            System.err.println("Error al escribir el historial: " + e.getMessage());
        }
    }

    // Main para arrancar el nodo
    public static void main(String[] args) {
        int puertoLocal = 8001; // Puerto por defecto para que funcione con el botón "Run"
        
        if (args.length > 0) {
            puertoLocal = Integer.parseInt(args[0]);
        }
        
        NodoADS miNodo = new NodoADS(puertoLocal);
        miNodo.iniciar();

        // Conectar automáticamente a los otros nodos si se pasan por argumentos en la consola
        for (int i = 1; i < args.length; i++) {
            miNodo.conectarConOtroNodo("localhost", Integer.parseInt(args[i]));
        }
    }
}