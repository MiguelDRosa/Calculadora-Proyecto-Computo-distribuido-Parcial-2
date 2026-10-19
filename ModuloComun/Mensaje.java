package ModuloComun;

import java.io.Serializable;
import java.util.UUID;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    public String idMensaje;
    public String idSolicitudOriginal;
    public String codigoContenido; 
    public double num1;
    public double num2;
    public double resultado;
    public String idRemitente;

    
    public Mensaje(String codigoContenido, double num1, double num2, String idRemitente) {
        this.idMensaje = "MSG-" + UUID.randomUUID().toString().substring(0, 6);
        this.idSolicitudOriginal = "N/A"; 
        this.codigoContenido = codigoContenido;
        this.num1 = num1;
        this.num2 = num2;
        this.resultado = 0.0;
        this.idRemitente = idRemitente;
    }

   
    public Mensaje(String codigoContenido, String idSolicitudOriginal, double resultado, String idRemitente) {
        this.idMensaje = "MSG-" + UUID.randomUUID().toString().substring(0, 6);
        this.idSolicitudOriginal = idSolicitudOriginal;
        this.codigoContenido = codigoContenido;
        this.num1 = 0.0;
        this.num2 = 0.0;
        this.resultado = resultado;
        this.idRemitente = idRemitente;
    }

  
    @Override
    public String toString() {
        return "Mensaje[" + idMensaje + "] | Codigo: " + codigoContenido + 
               " | Orig: " + idSolicitudOriginal + " | Remitente: " + idRemitente;
    }
}