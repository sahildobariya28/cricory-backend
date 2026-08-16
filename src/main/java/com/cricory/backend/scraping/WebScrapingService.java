package com.cricory.backend.scraping;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "cricory.scraping", name = "enabled", havingValue = "true")
public class WebScrapingService {

    private final ScrapingProperties properties;

    public WebScrapingService(ScrapingProperties properties) {
        this.properties = properties;
    }

    public ScrapeResult scrapeConfiguredPage() {
        properties.validateTarget();
        String requestedUrl = properties.getTargetUrl().toString();

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions().setHeadless(properties.isHeadless()));
             BrowserContext context = browser.newContext(
                     new Browser.NewContextOptions().setUserAgent(properties.getUserAgent()))) {

            Page page = context.newPage();
            page.setDefaultTimeout(properties.getNavigationTimeout().toMillis());
            page.navigate(requestedUrl, new Page.NavigateOptions()
                    .setTimeout(properties.getNavigationTimeout().toMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            String visibleText = page.locator("body").innerText();
            if (visibleText.length() > properties.getMaxTextLength()) {
                visibleText = visibleText.substring(0, properties.getMaxTextLength());
            }

            return new ScrapeResult(
                    requestedUrl,
                    page.url(),
                    page.title(),
                    visibleText,
                    Instant.now()
            );
        } catch (RuntimeException exception) {
            throw new ScrapingException("Unable to scrape configured page", exception);
        }
    }
}
