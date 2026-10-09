package backend_r.reciclaje.auth;

import backend_r.reciclaje.domain.Alumno;
import backend_r.reciclaje.domain.Colegio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "USUARIO", uniqueConstraints = {
        @UniqueConstraint(name = "UK_USUARIO_IDENTIFICADOR", columnNames = "IDENTIFICADOR"),
        @UniqueConstraint(name = "UK_USUARIO_ALUMNO", columnNames = "ID_ALUMNO")
})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    private Long id;

    @Column(name = "IDENTIFICADOR", nullable = false, length = 80)
    private String identificador;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 20)
    private Role rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ALUMNO")
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COLEGIO")
    private Colegio colegio;

    @Column(name = "ESTADO", nullable = false, length = 1)
    private String estado = "A";

    protected Usuario() {
    }

    public Usuario(String identificador, String passwordHash, Role rol, Alumno alumno, Colegio colegio) {
        this.identificador = identificador;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.alumno = alumno;
        this.colegio = colegio;
    }

    public Long getId() {
        return id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRol() {
        return rol;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public Colegio getColegio() {
        return colegio;
    }

    public boolean isActivo() {
        return "A".equals(estado);
    }
}
