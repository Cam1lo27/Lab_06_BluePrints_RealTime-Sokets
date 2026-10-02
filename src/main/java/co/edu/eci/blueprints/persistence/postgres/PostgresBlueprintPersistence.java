package co.edu.eci.blueprints.persistence.postgres;

import org.springframework.context.annotation.Profile;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistence;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Profile("postgres")
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repo;

    public PostgresBlueprintPersistence(BlueprintJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repo.existsByAuthorAndName(bp.getAuthor(), bp.getName())) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + ":" + bp.getName());
        }
        BlueprintEntity entity = new BlueprintEntity(bp.getAuthor(), bp.getName());
        for (Point p : bp.getPoints()) {
            entity.addPoint(new PointEntity(p.x(), p.y()));
        }
        repo.save(entity);
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        return toDomain(entity);
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> entities = repo.findByAuthor(author);
        if (entities.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return new HashSet<>(repo.findAll().stream().map(this::toDomain).collect(Collectors.toSet()));
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        entity.addPoint(new PointEntity(x, y));
        repo.save(entity);
    }

    private Blueprint toDomain(BlueprintEntity entity) {
        List<Point> points = entity.getPoints().stream()
                .map(pe -> new Point(pe.getX(), pe.getY()))
                .toList();
        return new Blueprint(entity.getAuthor(), entity.getName(), points);
    }
}
