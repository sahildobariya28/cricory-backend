package com.cricory.backend.scraping;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScrapingPropertiesTest {

    @Test
    void acceptsConfiguredHostAndItsSubdomains() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.setAllowedHosts(Set.of("example.com"));
        properties.setTargetUrl(URI.create("https://scores.example.com/live"));

        assertDoesNotThrow(properties::validateTarget);
    }

    @Test
    void rejectsUnlistedHost() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.setAllowedHosts(Set.of("example.com"));
        properties.setTargetUrl(URI.create("https://untrusted.test/private"));

        assertThrows(IllegalStateException.class, properties::validateTarget);
    }

    @Test
    void rejectsNonHttpSchemes() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.setAllowedHosts(Set.of("example.com"));
        properties.setTargetUrl(URI.create("file:///etc/passwd"));

        assertThrows(IllegalStateException.class, properties::validateTarget);
    }
}
