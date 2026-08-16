package com.cricory.backend.scraping;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Validated
@ConfigurationProperties(prefix = "cricory.scraping")
public class ScrapingProperties {

    private boolean enabled;

    private boolean remoteEnabled;

    @NotNull
    private URI targetUrl = URI.create("https://example.com");

    @NotEmpty
    private Set<String> allowedHosts = new LinkedHashSet<>(Set.of("example.com"));

    private boolean headless = true;

    @NotNull
    private Duration navigationTimeout = Duration.ofSeconds(30);

    @Min(1_000)
    @Max(1_000_000)
    private int maxTextLength = 20_000;

    @NotBlank
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";

    @NotNull
    private Duration liveCacheTtl = Duration.ofSeconds(20);

    @NotNull
    private Duration upcomingCacheTtl = Duration.ofMinutes(30);

    @NotNull
    private Duration resultsCacheTtl = Duration.ofMinutes(15);

    public void validateTarget() {
        String scheme = targetUrl.getScheme();
        String host = targetUrl.getHost();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalStateException("Scraping target must use HTTP or HTTPS");
        }
        if (host == null || allowedHosts.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .noneMatch(allowed -> host.equalsIgnoreCase(allowed) || host.toLowerCase(Locale.ROOT).endsWith("." + allowed))) {
            throw new IllegalStateException("Scraping target host is not in cricory.scraping.allowed-hosts");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRemoteEnabled() { return remoteEnabled; }
    public void setRemoteEnabled(boolean remoteEnabled) { this.remoteEnabled = remoteEnabled; }
    public URI getTargetUrl() { return targetUrl; }
    public void setTargetUrl(URI targetUrl) { this.targetUrl = targetUrl; }
    public Set<String> getAllowedHosts() { return allowedHosts; }
    public void setAllowedHosts(Set<String> allowedHosts) { this.allowedHosts = allowedHosts; }
    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public Duration getNavigationTimeout() { return navigationTimeout; }
    public void setNavigationTimeout(Duration navigationTimeout) { this.navigationTimeout = navigationTimeout; }
    public int getMaxTextLength() { return maxTextLength; }
    public void setMaxTextLength(int maxTextLength) { this.maxTextLength = maxTextLength; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Duration getLiveCacheTtl() { return liveCacheTtl; }
    public void setLiveCacheTtl(Duration liveCacheTtl) { this.liveCacheTtl = liveCacheTtl; }
    public Duration getUpcomingCacheTtl() { return upcomingCacheTtl; }
    public void setUpcomingCacheTtl(Duration upcomingCacheTtl) { this.upcomingCacheTtl = upcomingCacheTtl; }
    public Duration getResultsCacheTtl() { return resultsCacheTtl; }
    public void setResultsCacheTtl(Duration resultsCacheTtl) { this.resultsCacheTtl = resultsCacheTtl; }
}
