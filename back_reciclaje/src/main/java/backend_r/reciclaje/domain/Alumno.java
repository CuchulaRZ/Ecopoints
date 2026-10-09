package backend_r.reciclaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ALUMNO")
public class Alumno {
    @Id
    @Column(name = "ID_ALUMNO")
    private Long id;

    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    @Column(name = "APELLIDO", nullable = false)
    private String apellido;

    @Column(name = "ID_SALON", nullable = false)
    private Long salonId;

    protected Alumno() {
    }

    public Alumno(Long id, String nombre, String apellido, Long salonId) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.salonId = salonId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public Long getId() {
        return id;
    }

    public Long getSalonId() {
        return salonId;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
