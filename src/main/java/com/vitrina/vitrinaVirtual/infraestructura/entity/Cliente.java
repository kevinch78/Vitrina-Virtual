package com.vitrina.vitrinaVirtual.infraestructura.entity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String telefono;
    private String direccion;

    // ✅ SOLUCIÓN: Agregar fetch = LAZY y @Fetch(FetchMode.SELECT)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    @Fetch(FetchMode.SELECT)
    private Usuario usuario;

    // @OneToOne
    // @JoinColumn(name = "usuario_id", unique = true)
    // private Usuario usuario;
}


