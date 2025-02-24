package com.example.colombina.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Rol {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Column
    private String tipoRol; //ADMIN, ASUNTOSREG, SOLICITANTE, EXPORTACIONES, MERCADEO

    public Rol(String tipoRol) {
        this.tipoRol = tipoRol;
    }
}




