package com.example.colombina.services;

import com.example.colombina.DTOs.SolicitudDTO;
import com.example.colombina.DTOs.TramiteDTO;
import com.example.colombina.model.Solicitud;
import com.example.colombina.model.Tramite;
import com.example.colombina.model.Usuario;
import com.example.colombina.repositories.SolicitudRepository;
import com.example.colombina.repositories.TramiteRepository;
import com.example.colombina.repositories.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper; // Usaremos ModelMapper para la conversión a DTO

    public SolicitudDTO crearSolicitud(SolicitudDTO solicitudDTO, TramiteDTO tramiteDTO, String username) {
        log.info("Entra crear servicio");

        // Obtener el usuario solicitante
        Usuario solicitante = usuarioRepository.findByNombre(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el nombre: " + username));
        log.info("Usuario encontrado " + solicitante.getId());

        Long nextId = solicitudRepository.count();

        // Crear y guardar el nuevo Tramite
        Tramite nuevoTramite = modelMapper.map(tramiteDTO, Tramite.class); // Convertir el DTO a entidad
        nuevoTramite.setEstado(Tramite.EstadoTramite.PENDIENTE); // Asignar estado pendiente
        nuevoTramite.setNumeroRadicado("AR-" + (nextId + 50)); // Generar número de radicado
        nuevoTramite.setFechaRadicacion(new Date()); // Asignar la fecha actual
        nuevoTramite.setEtapa(2);
        nuevoTramite.setProgreso();
        System.out.println("Antes de guardar tramite: " + nuevoTramite.getNumeroRadicado());
        Tramite tramiteGuardado = tramiteRepository.save(nuevoTramite);
        System.out.println("Tramite guardado: " + tramiteGuardado.getNumeroRadicado());
        // Crear y guardar la nueva Solicitud
        Solicitud nuevaSolicitud = modelMapper.map(solicitudDTO, Solicitud.class); // Convertir el DTO a entidad
        nuevaSolicitud.setSolicitante(solicitante); // Asociar el usuario solicitante
        nuevaSolicitud.setTramite(tramiteGuardado); // Asociar el trámite creado

        Solicitud solicitudGuardada = solicitudRepository.save(nuevaSolicitud);

        log.info("Antes de return");
        // Convertir la solicitud guardada a DTO y devolverla
        return modelMapper.map(solicitudGuardada, SolicitudDTO.class);
    }

    public List<Solicitud> getSolicitudesPorSolicitante(String username) {
        Usuario solicitante = usuarioRepository.findByNombre(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el nombre: " + username));
        return solicitudRepository.findBySolicitanteWithTramite(solicitante);
    }

    public List<Solicitud> getSolicitudes() {
        return solicitudRepository.findAll();
    }
}
