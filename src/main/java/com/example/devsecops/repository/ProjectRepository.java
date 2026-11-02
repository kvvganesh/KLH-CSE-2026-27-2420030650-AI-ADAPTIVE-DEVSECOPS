package com.example.devsecops.repository;

import com.example.devsecops.model.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** In-memory store: keeps the demo dependency-free (no database needed). */
@Repository
public class ProjectRepository {

    private final Map<Long, Project> store = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public Project save(Project project) {
        if (project.getId() == null) {
            project.setId(sequence.incrementAndGet());
        }
        store.put(project.getId(), project);
        return project;
    }

    public List<Project> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}
