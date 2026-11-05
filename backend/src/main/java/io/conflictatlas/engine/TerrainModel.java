package io.conflictatlas.engine;

import io.conflictatlas.domain.Models.TerrainKind;

/** Strategy objects describe generic environmental friction, not an actual battlefield. */
public sealed interface TerrainModel
    permits TerrainModel.Open,
        TerrainModel.Mountain,
        TerrainModel.Urban,
        TerrainModel.Desert,
        TerrainModel.Coastal {
  double friction();

  double variability();

  String description();

  static TerrainModel of(TerrainKind kind) {
    return switch (kind) {
      case OPEN -> new Open();
      case MOUNTAIN -> new Mountain();
      case URBAN -> new Urban();
      case DESERT -> new Desert();
      case COASTAL -> new Coastal();
    };
  }

  record Open() implements TerrainModel {
    public double friction() {
      return 1.0;
    }

    public double variability() {
      return 0.12;
    }

    public String description() {
      return "Open terrain; baseline friction.";
    }
  }

  record Mountain() implements TerrainModel {
    public double friction() {
      return 1.65;
    }

    public double variability() {
      return 0.27;
    }

    public String description() {
      return "Rugged terrain increases shared supply pressure and uncertainty.";
    }
  }

  record Urban() implements TerrainModel {
    public double friction() {
      return 1.5;
    }

    public double variability() {
      return 0.30;
    }

    public String description() {
      return "Urban complexity increases shared friction and uncertainty.";
    }
  }

  record Desert() implements TerrainModel {
    public double friction() {
      return 1.4;
    }

    public double variability() {
      return 0.22;
    }

    public String description() {
      return "Arid conditions increase shared sustainment pressure.";
    }
  }

  record Coastal() implements TerrainModel {
    public double friction() {
      return 1.3;
    }

    public double variability() {
      return 0.24;
    }

    public String description() {
      return "Coastal geography adds shared environmental complexity.";
    }
  }
}
