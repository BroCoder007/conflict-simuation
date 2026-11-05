package io.conflictatlas.persistence;

import io.conflictatlas.domain.Models.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcRunRepository implements RunRepository {
  private final JdbcTemplate db;
  private final JsonMapper json = JsonMapper.builder().build();

  public JdbcRunRepository(JdbcTemplate db) {
    this.db = db;
    db.execute(
        "CREATE TABLE IF NOT EXISTS runs(id VARCHAR(36) PRIMARY KEY,owner VARCHAR(64) NOT"
            + " NULL,created_at VARCHAR(40) NOT NULL,name VARCHAR(80) NOT NULL,payload TEXT NOT"
            + " NULL)");
    db.execute("CREATE INDEX IF NOT EXISTS runs_owner ON runs(owner,created_at)");
  }

  @Override
  public synchronized SavedRun save(String owner, SimulationResult result) {
    Integer count =
        db.queryForObject("SELECT COUNT(*) FROM runs WHERE owner=?", Integer.class, owner);
    if (count != null && count >= 50)
      throw new IllegalArgumentException(
          "Your workspace has 50 saved runs. Remove a run before creating another.");
    SavedRun run =
        new SavedRun(
            UUID.randomUUID().toString(), result.generatedAt(), result.request().name(), result);
    db.update(
        "INSERT INTO runs(id,owner,created_at,name,payload) VALUES(?,?,?,?,?)",
        run.id(),
        owner,
        run.createdAt(),
        run.name(),
        json.writeValueAsString(run));
    return run;
  }

  @Override
  public List<RunSummary> list(String owner) {
    return db.query(
        "SELECT payload FROM runs WHERE owner=? ORDER BY created_at DESC",
        (row, n) -> {
          SavedRun run = json.readValue(row.getString(1), SavedRun.class);
          var r = run.result().request();
          return new RunSummary(
              run.id(),
              run.createdAt(),
              run.name(),
              r.countryA(),
              r.countryB(),
              r.terrain().name(),
              r.days(),
              run.result().outcomes());
        },
        owner);
  }

  @Override
  public Optional<SavedRun> find(String owner, String id) {
    return db
        .query(
            "SELECT payload FROM runs WHERE owner=? AND id=?",
            (row, n) -> json.readValue(row.getString(1), SavedRun.class),
            owner,
            id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean delete(String owner, String id) {
    return db.update("DELETE FROM runs WHERE owner=? AND id=?", owner, id) > 0;
  }
}
