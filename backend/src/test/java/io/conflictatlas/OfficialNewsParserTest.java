package io.conflictatlas;

import static org.junit.jupiter.api.Assertions.*;

import io.conflictatlas.data.OfficialNewsParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OfficialNewsParserTest {
  String item(String title, String url) {
    return "<item><title>"
        + title
        + "</title><link>"
        + url
        + "</link><pubDate>Wed, 04 Nov 2026 12:00:00 +0000</pubDate></item>";
  }

  @Test
  void normalizesDatesFiltersHeadlinesAndRejectsUnsafeLinks() throws Exception {
    String xml =
        "<rss><channel>"
            + item("Peacekeeping update", "https://news.un.org/en/story/one")
            + item("Warming oceans", "https://news.un.org/en/story/two")
            + item("War update", "javascript:alert(1)")
            + item("War report", "https://untrusted.example/item")
            + "</channel></rss>";
    var rows = OfficialNewsParser.parse(xml.getBytes(StandardCharsets.UTF_8));
    assertEquals(1, rows.size());
    assertEquals("2026-11-04T12:00:00Z", rows.getFirst().publishedAt());
  }

  @Test
  void externalEntitiesAreDisabled() {
    String xml =
        "<!DOCTYPE rss [<!ENTITY xxe SYSTEM"
            + " 'file:///does-not-exist'>]><rss><channel>&xxe;</channel></rss>";
    assertThrows(
        Exception.class, () -> OfficialNewsParser.parse(xml.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void duplicateStoriesAreNotRepeated() throws Exception {
    String xml =
        "<rss><channel>"
            + item("War report", "https://news.un.org/en/story/one")
            + item("War report", "https://news.un.org/en/story/one")
            + "</channel></rss>";
    assertEquals(1, OfficialNewsParser.parse(xml.getBytes(StandardCharsets.UTF_8)).size());
  }
}
