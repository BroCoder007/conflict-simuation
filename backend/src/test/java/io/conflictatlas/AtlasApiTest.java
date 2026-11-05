package io.conflictatlas;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.conflictatlas.data.DataCatalog;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:atlas-tests;DB_CLOSE_DELAY=-1",
      "atlas.live-data=false"
    })
@AutoConfigureMockMvc
class AtlasApiTest {
  @Test
  void packagedHomeDoesNotReuseStaleHtml() throws Exception {
    http.perform(get("/").header("If-Modified-Since", "Tue, 01 Jan 2030 00:00:00 GMT"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "private, no-store"));
  }
  @Autowired MockMvc http;
  @Autowired DataCatalog data;
  final JsonMapper json = JsonMapper.builder().build();
  static final String BODY =
      """
      {"name":"Integration scenario","countryA":"IND","countryB":"PAK","terrain":"MOUNTAIN","days":30,"trials":100,"seed":42,"readinessA":70,"readinessB":70,"supplyA":70,"supplyB":70,"diplomacy":30,"climateStress":20}
      """;

  Cookie workspace() throws Exception {
    String header =
        http.perform(get("/api/runs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");
    assertNotNull(header);
    assertTrue(header.contains("HttpOnly"));
    assertTrue(header.contains("SameSite=Strict"));
    return new Cookie("atlas_workspace", header.split(";", 2)[0].split("=", 2)[1]);
  }

  @Test
  void observationsAreAvailableWithoutExternalNetwork() throws Exception {
    http.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
    http.perform(get("/api/catalog"))
        .andExpect(jsonPath("$.manifest.candidateVersion").value("26.01.26.09"));
    assertTrue(data.history().size() > 7000);
    assertTrue(data.candidate().size() > 100);
    assertTrue(data.country("IND").metrics().get("spending").year() >= 2024);
  }

  @Test
  void runPersistsOnlyInItsOwnerWorkspace() throws Exception {
    var owner = workspace();
    var other = workspace();
    var response =
        http.perform(
                post("/api/runs")
                    .header("Host", "localhost")
                    .header("Origin", "http://localhost")
                    .cookie(owner)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.modelVersion").value("aggregate-resource-v1"))
            .andReturn()
            .getResponse();
    String id = json.readTree(response.getContentAsString()).path("id").asText();
    http.perform(get("/api/runs/" + id).cookie(owner)).andExpect(status().isOk());
    http.perform(get("/api/runs/" + id).cookie(other)).andExpect(status().isNotFound());
    http.perform(
            delete("/api/runs/" + id)
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .cookie(other))
        .andExpect(status().isNotFound());
    http.perform(
            delete("/api/runs/" + id)
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .cookie(owner))
        .andExpect(status().isNoContent());
    http.perform(get("/api/runs/" + id).cookie(owner)).andExpect(status().isNotFound());
  }

  @Test
  void rejectCrossOriginAndMissingOriginWrites() throws Exception {
    http.perform(
            post("/api/runs")
                .header("Host", "localhost")
                .header("Origin", "https://untrusted.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());
    http.perform(
            post("/api/runs")
                .header("Host", "localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectOutOfRangeAndUnknownInputs() throws Exception {
    http.perform(
            post("/api/runs")
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"trials\":100", "\"trials\":9000")))
        .andExpect(status().isBadRequest());
    http.perform(
            post("/api/runs")
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"IND\"", "\"ZZZ\"")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refreshCanBeDisabledForOfflineDeployment() throws Exception {
    http.perform(
            post("/api/sources/refresh")
                .header("Host", "localhost")
                .header("Origin", "http://localhost"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void sourceSnapshotsAndReceiptsAreExposed() throws Exception {
    http.perform(get("/api/news"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stale").value(true));
    http.perform(get("/api/countries/IND"))
        .andExpect(jsonPath("$.country.metrics.personnel.year").exists());
  }
}
