package com.example.colombina.services;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.colombina.model.Notificacion;
import com.example.colombina.model.Tramite;
import com.example.colombina.model.Usuario;
import com.example.colombina.repositories.NotificacionRepository;
import com.example.colombina.repositories.TramiteRepository;
import com.example.colombina.repositories.UsuarioRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@Service
public class NotificacionService {

    @Autowired
    private Resend resendClient;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final String emailFrom = "Colombina <colombina@santicm.com>";

    @Scheduled(cron = "0 0 10 * * ?")
    public void verificarExpiracionTramites() {
        List<Tramite> tramites = tramiteRepository.findAll();
        Date fechaActual = new Date();
        
        for (Tramite tramite : tramites) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(tramite.getFechaRadicacion());
            calendar.add(Calendar.DAY_OF_MONTH, 30); // Expiración 30 días después
            Date fechaExpiracion = calendar.getTime();

            Calendar calendarLimite = Calendar.getInstance();
            calendarLimite.setTime(fechaExpiracion);
            calendarLimite.add(Calendar.DAY_OF_MONTH, -30);
            Date fechaLimite = calendarLimite.getTime();

            if (fechaLimite.before(fechaActual)) {
                enviarNotificacionExpiracionTramite(tramite.getId());
            }
        }
    }

    public void enviarNotificacionEstadoTramite(Long tramiteId, String nuevoEstado) {
        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        String mensaje = "Su trámite con ID " + tramiteId + " ha cambiado de estado a: " + nuevoEstado + ".";

        // Enviar notificación en tiempo real a través de WebSocket
        messagingTemplate.convertAndSend("/topic/notificaciones/" + destinatario.getId(), mensaje);

        enviarCorreo(destinatario.getCorreoElectronico(), "Cambio de estado del trámite", mensaje);
        guardarNotificacion(tramiteId, "Cambio de estado del trámite", mensaje);
    }

    public void enviarNotificacionDocumentosFaltantes(Long tramiteId, List<String> documentosFaltantes) {
        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        String documentos = String.join(", ", documentosFaltantes);
        String mensaje = "Faltan los siguientes documentos para su trámite con ID " + tramiteId + ": " + documentos + ". Por favor, adjúntelos para continuar.";

        messagingTemplate.convertAndSend("/topic/notificaciones/" + destinatario.getId(), mensaje);

        enviarCorreo(destinatario.getCorreoElectronico(), "Documentos Faltantes", mensaje);
        guardarNotificacion(tramiteId, "Documentos Faltantes", mensaje);
    }

    public void enviarNotificacionExpiracionTramite(Long tramiteId) {
        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        String mensaje = "Su trámite con ID " + tramiteId + " está a punto de expirar. Renueve su solicitud a tiempo.";

        messagingTemplate.convertAndSend("/topic/notificaciones/" + destinatario.getId(), mensaje);

        enviarCorreo(destinatario.getCorreoElectronico(), "Expiración de Trámite", mensaje);
        guardarNotificacion(tramiteId, "Expiración de Trámite", mensaje);
    }

    public void enviarNotificacionDocumentoNoCumpleNormativas(Long tramiteId, String tipoDocumento) {
        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        String mensaje = "El documento de tipo '" + tipoDocumento + "' asociado a su trámite con ID " + tramiteId + " no cumple con las normativas requeridas. Por favor, revise y envíe un documento que cumpla con los requisitos.";

        messagingTemplate.convertAndSend("/topic/notificaciones/" + destinatario.getId(), mensaje);

        enviarCorreo(destinatario.getCorreoElectronico(), "Documento No Cumple Normativas", mensaje);
        guardarNotificacion(tramiteId, "Documento No Cumple Normativas", mensaje);
    }

    public void enviarNotificacionDocumentoVencido(Long tramiteId, String tipoDocumento) {
        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        String mensaje = "El documento de tipo '" + tipoDocumento + "' asociado a su trámite con ID " + tramiteId + " ha vencido. Por favor, adjunte un documento vigente para continuar con el proceso.";

        messagingTemplate.convertAndSend("/topic/notificaciones/" + destinatario.getId(), mensaje);

        enviarCorreo(destinatario.getCorreoElectronico(), "Documento Vencido", mensaje);
        guardarNotificacion(tramiteId, "Documento Vencido", mensaje);
    }

    public List<Notificacion> obtenerNotificacionesPorUsuario(Long usuarioId) {
        Usuario destinatario = usuarioRepository.findById(usuarioId).orElse(null);
        return notificacionRepository.findByDestinatario(destinatario);
    }

    public void marcarNotificacionComoLeida(Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId).orElse(null);
        if (notificacion != null) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
    }


    public void enviarCorreo(String destinatario, String asunto, String mensaje) {
        String firmaUrl = "http://localhost:8080/images/firma.png";
        String mensajeHtml = "<html>" +
                "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<h2 style='color: #0066CC;'>Estimado usuario,</h2>" +
                "<p style='font-size: 14px;'>" + mensaje + "</p>" +
                "<br>" +
                "<p style='font-size: 12px; color: #888;'>Gracias por su atención.</p>" +
                "<br><br>" +
                "<hr style='border: 0; height: 1px; background-color: #ddd;'/>" +
                "<p style='font-size: 12px; color: #333;'>Atentamente,</p>" +
                "<p style='font-size: 14px; font-weight: bold; color: #0066CC;'>Colombina</p>" +
                "<img src='" + firmaUrl + "' alt='Firma' style='width: 200px; margin-top: 20px;'/>" +
                "</body>" +
                "</html>";
    
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(emailFrom)
                .to(destinatario)
                .subject(asunto)
                .html(mensajeHtml)
                .build();
    
        try {
            CreateEmailResponse data = resendClient.emails().send(params);
            System.out.println("Email enviado: " + data.getId());
        } catch (ResendException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo: " + e.getMessage());
        }
    }
    

    // Método auxiliar para guardar la notificación en la base de datos
    public void guardarNotificacion(Long tramiteId, String titulo, String mensaje) {

        Usuario destinatario = usuarioRepository.findSolicitanteByTramiteId(tramiteId);
        Notificacion notificacion = new Notificacion();
        notificacion.setAsunto(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(new Date());
        notificacion.setDestinatario(destinatario);
        notificacion.setLeida(false);

        
        notificacionRepository.save(notificacion);
        System.out.println(
                "Notificación registrada en la base de datos para el usuario: " + destinatario.getCorreoElectronico());
    }

    public List<Notificacion> obtenerTodasLasNotificaciones() {
        return notificacionRepository.findAll();
    }

    public Notificacion obtenerNotificacionPorId(Long id) {
        return notificacionRepository.findById(id).orElse(null);
    }

    public Notificacion crearNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    public Notificacion actualizarNotificacion(Long id, Notificacion notificacion) {
        if (notificacionRepository.existsById(id)) {
            notificacion.setId(id);
            return notificacionRepository.save(notificacion);
        }
        return null;
    }

    public boolean eliminarNotificacion(Long id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void recuperarContrasena(String nombre) {
        List<String> admins = getAdminEmails();

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(emailFrom)
                .to(admins)
                .subject("Solicitud para recuperar contraseña")
                .html("<strong>Se solicita al administrador comunicarse con el usuario " + nombre
                        + " para enviarle la nueva contraseña</strong>")
                .build();

        try {
            CreateEmailResponse data = resendClient.emails().send(params);
            System.out.println("Correo enviado con éxito, ID de la notificación: " + data.getId());
        } catch (ResendException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo: " + e.getMessage());
        }
    }

    private List<String> getAdminEmails() {
        return usuarioRepository.findByRolTipoRol("ADMIN").stream().map(u -> u.getCorreoElectronico()).toList();
    }
}
