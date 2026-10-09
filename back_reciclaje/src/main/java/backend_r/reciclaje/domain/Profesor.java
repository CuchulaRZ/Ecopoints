package backend_r.reciclaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PROFESOR")
public class Profesor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PROFESOR")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COLEGIO", nullable = false)
    private Colegio colegio;

    @Column(name = "ID_SALON", nullable = false)
    private Long salonId;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "APELLIDO", nullable = false, length = 100)
    private String apellido;

    @Column(name = "CORREO", nullable = false, length = 150)
    private String correo;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "ESTADO", nullable = false, length = 1)
    private String estado = "A";

    protected Profesor() {
    }

    public Profesor(
            Colegio colegio,
            String nombre,
            String apellido,
            String correo,
            String passwordHash,
            Long salonId) {
        this.colegio = colegio;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.salonId = salonId;
    }

    public Long getId() {
        return id;
    }

    public Colegio getColegio() {
        return colegio;
    }

    public Long getSalonId() {
        return salonId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return "A".equals(estado);
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
