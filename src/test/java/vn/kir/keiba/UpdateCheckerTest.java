package vn.kir.keiba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateCheckerTest {
    @Test void comparesSemanticVersions() {
        assertEquals(1, ReleaseVersion.compare("v4.5.0", "4.4.1"));
        assertEquals(0, ReleaseVersion.compare("v4.4.1", "4.4.1"));
        assertEquals(-1, ReleaseVersion.compare("4.4.0", "v4.4.1"));
    }

    @Test void parsesLatestReleaseResponse() {
        String json = "{\"tag_name\":\"v4.5.0\",\"html_url\":\"https://github.com/rogteam/KirKeiba/releases/tag/v4.5.0\"}";
        assertEquals("v4.5.0", ReleaseVersion.tagFromJson(json));
        assertEquals("https://github.com/rogteam/KirKeiba/releases/tag/v4.5.0", ReleaseVersion.urlFromJson(json, "fallback"));
    }
}
