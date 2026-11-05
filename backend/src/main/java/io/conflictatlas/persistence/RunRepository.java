package io.conflictatlas.persistence;

import io.conflictatlas.domain.Models.*;
import java.util.List;
import java.util.Optional;

public interface RunRepository {
  SavedRun save(String owner, SimulationResult result);

  List<RunSummary> list(String owner);

  Optional<SavedRun> find(String owner, String id);

  boolean delete(String owner, String id);
}
