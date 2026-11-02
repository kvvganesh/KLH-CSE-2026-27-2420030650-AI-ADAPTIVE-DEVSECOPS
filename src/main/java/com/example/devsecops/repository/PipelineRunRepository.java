package com.example.devsecops.repository;

import com.example.devsecops.model.PipelineRun;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class PipelineRunRepository {

    private final List<PipelineRun> runs = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    public long nextId() {
        return sequence.incrementAndGet();
    }

    public PipelineRun save(PipelineRun run) {
        runs.add(run);
        return run;
    }

    /** Newest first. */
    public List<PipelineRun> findAll() {
        List<PipelineRun> copy = new ArrayList<>(runs);
        Collections.reverse(copy);
        return copy;
    }

    public Optional<PipelineRun> findById(long id) {
        return runs.stream().filter(r -> r.id() == id).findFirst();
    }

    public List<PipelineRun> findByProject(Long projectId) {
        return findAll().stream().filter(r -> r.projectId().equals(projectId)).toList();
    }

    public Optional<PipelineRun> findLatest(Long projectId) {
        return findByProject(projectId).stream().findFirst();
    }

    public List<PipelineRun> findLatestPerProject() {
        Map<Long, PipelineRun> latest = new LinkedHashMap<>();
        findAll().forEach(r -> latest.putIfAbsent(r.projectId(), r));
        return new ArrayList<>(latest.values());
    }
}
