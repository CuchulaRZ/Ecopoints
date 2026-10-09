package backend_r.reciclaje.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "COLEGIO")
public class Colegio {
    @Id
    @Column(name = "ID_COLEGIO")
    private Long id;

    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    protected Colegio() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
