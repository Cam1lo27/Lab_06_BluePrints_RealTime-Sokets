package co.edu.eci.blueprints.persistence.postgres;

import jakarta.persistence.*;

@Entity
@Table(name = "points")
public class PointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int x;
    private int y;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private BlueprintEntity blueprint;

    protected PointEntity() { }

    public PointEntity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Long getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public BlueprintEntity getBlueprint() { return blueprint; }
    public void setBlueprint(BlueprintEntity blueprint) { this.blueprint = blueprint; }
}
