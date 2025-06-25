// Rabbithole
// Author: Michael Amyotte (twinblackbirds)
// Date: 6/23/25
// Purpose: Rebuilt data collection bot for data analysis
// 

package tbb.apps.Rabbithole;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


import tbb.db.Driver.Sqlite;
import tbb.utils.Config.ConfigPayload;
import tbb.utils.Config.Configurator;
import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;
import tbb.utils.Printer.Printer;
import tbb.utils.Printer.State;

public class App 
{
	// configuration
	private static Logger log = new Logger(LogLevel.ERROR); // min log level
	private static final ConfigPayload config = new Configurator(log).getData();
	
	// consts
	private static final int MAX_RETRIES = 3; // if page fails to load (cd.get())
	private static final int TIMEOUT_SEC = 30; // time to wait for el to be present
	private static final int EXTRA_WAIT_MS = 1000; // extra time spent waiting after el is present
	
	// db
	private static Sqlite sql = new Sqlite(log, true); // debug mode enabled TODO: disable
	private static final long SessionID = sql.startSession();
	
	// selenium browser tools
	private static ChromeDriver cd;
	private static JavascriptExecutor js; // to execute JS in browser context
	
	// video id regex
	private static final Pattern pattern = Pattern.compile("(?<=\\?v\\=)[\\w-]+(?=[&/]?)", Pattern.CASE_INSENSITIVE);

	
    public static void main( String[] args )
    {
    	boolean headless = false;
    	if (config != null) {
    		headless = config.headless;
    	}
    	log.Write(LogLevel.BYPASS, "Session started with ID: " + SessionID);
    	log.Write(LogLevel.INFO, "Headless mode: " + (headless ? "enabled" : "disabled"));
    	
    	// set launch options
		log.Write(LogLevel.DBG, "Setting Chrome launch options");
    	ChromeOptions co = new ChromeOptions();
    	if (headless) { co.addArguments("headless"); }
    	
    	// point selenium to correct driver
    	log.Write(LogLevel.DBG, "Creating default ChromeDriverService");
    	ChromeDriverService cds = ChromeDriverService.createDefaultService();
    	
    	
    	// start driver
    	log.Write(LogLevel.INFO, "Starting Chrome browser");
    	cd = new ChromeDriver(cds, co);
    	js = (JavascriptExecutor) cd;
    	
    	// end-user feedback
    	Printer.startBox("Rabbithole");
    	
    	// String s = loopUntilInput();
    	try {
    		// only enable while loop once you are confident in the bot's abilities
//    		while (true) {
            	bot();		
//    		}
    	} catch (Exception e) {
    		log.Write(LogLevel.ERROR, "Bot failed! " + e);
    	} finally {
    		// close DB session 
    		log.Write(LogLevel.INFO, "Closing session");
    		sql.closeSession(SessionID);
    		log.Write(LogLevel.BYPASS, "Session " + SessionID + " closed");
    		
    		// close browser + all tabs
    		log.Write(LogLevel.INFO, "Closing Chrome browser");
            cd.quit();
            
            // dump logs
            log.close();

            System.out.println("Process terminated with return code 0");
    	}
    }
    
    private static void bot() throws Exception {
    	navigateTo("https://youtube.com");
    	// check if we are getting 'search to get started'
    	List<WebElement> els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	int attempts = 0;
    	while (els.size() > 0 && attempts < MAX_RETRIES) {
    		log.Write(LogLevel.WARN, "Getting served the 'search to get started' page");
    		// we are on the page
    		navigateTo("https://youtube.com/shorts/");
    		waitForElementClickable("#reel-video-renderer .video-stream");
    		WebElement vid = cd.findElement(By.cssSelector("#reel-video-renderer .video-stream"));
    		jsClick(vid);
    		Thread.sleep(3500); // 'watch' the short a little bit
    		navigateTo("https://youtube.com/");
    		els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	}
    	if (els.size() > 0) {
    		throw new Exception("Could not get passed the 'search to get started' page!");
    	}
    	log.Write(LogLevel.INFO, "Passed the 'search to get started' page!"); // we are on the home page right here
    	
    	/*
    	 * we can get started on collection now
    	 */
    	
    	String href = getFirstValidVideoID();
    	navigateTo(href);
    	ensureAdsSkipped();
    	ensure144p();
    	
    	
    	
    	
    	// end of bot
    	sendState(State.WAITING);
    	Thread.sleep(5000); // for ease of debugging
    }
    
    private static void ensureAdsSkipped() {
    	sendState(State.INTERACTING);
    	List<WebElement> ads = cd.findElements(By.cssSelector("div.video-ads > *"));
    	if (ads.size() > 0) { // there are ads to skip
    		List<WebElement> skipButton = cd.findElements(By.cssSelector("button.ytp-skip-ad-button"));
    		if (skipButton.size() > 0) { // in case the ad is unskippable
    			jsClick(skipButton.getFirst()); // should skip them all
    		}
    	}
    	sendState(State.WAITING);
    }
    
    private static List<WebElement> getVideos() {
    	if (cd.getCurrentUrl().contains("watch?v=")) {
    		return getSidebarVideos();
    	} else {
    		return getHomepageVideos();
    	}
    }
    private static List<WebElement> getSidebarVideos() {
    	sendState(State.SCANNING);
    	List<WebElement> videos = cd.findElements(By.cssSelector("a.ytd-compact-video-renderer"));
    	sendState(State.WAITING);
    	return videos;
    }
    
    private static List<WebElement> getHomepageVideos() {
    	sendState(State.SCANNING);
    	List<WebElement> videos = cd.findElements(By.cssSelector("a#video-title-link"));
    	sendState(State.WAITING);
    	return videos;
    }
    
    private static String getFirstValidVideoID() throws Exception {
    	sendState(State.SCANNING);
    	int idx = 0;
    	
    	List<WebElement> videos = getVideos();
    	String videoID = null;
    	String href = null;
    	// find the first video link with an href (should always be 0, but just in case)
    	// also make sure we haven't already seen the video
    	while (href == null || (videoID != null && sql.findSessionVideo(SessionID, videoID))) {
    		href = videos.get(idx).getAttribute("href");
    		Matcher m = pattern.matcher(href);
    		videoID = (m.find() ? m.group() : null);
    		idx++;
    	}
    	sendState(State.WAITING);
    	return videoID;
    }
    
    private static void ensure144p() throws Exception {
    	// turn down the video quality to reduce bandwidth usage
    	WebElement settingsBtn = cd.findElement(By.cssSelector("button[title=\"Settings\"]"));
    	jsClick(settingsBtn);
    	List<WebElement> qualitySelector = cd.findElements(By.cssSelector(".ytp-settings-menu div.ytp-panel-menu > *"));
    	qualitySelector.removeLast(); // 'auto'
    	WebElement lowest = qualitySelector.getLast(); // 144p
    	jsClick(lowest);    	
    }
    
    private static void grabChannel() {
    	// get channel of video we are watching
    }
    
    private static void navigateTo(String URL) {
    	sendState(State.NAVIGATING);
    	cd.get(URL);
    	waitUntilPageLoaded();
    }
    
    // wait for TIMEOUT_SEC seconds OR until the DOM reports readyState = complete
    private static void waitUntilPageLoaded() {
    	sendState(State.LOADING);
    	String pageName = cd.getTitle();
    	log.Write(LogLevel.INFO, String.format("Waiting for page '%s' to load", pageName));
    	new WebDriverWait(cd, Duration.ofSeconds(TIMEOUT_SEC)).until(
                webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    	log.Write(LogLevel.INFO, "Page loaded");
    	sendState(State.WAITING);
    }
    
    private static void sendState(State state) {
    	String cleanURL = ensureSchema(cd.getCurrentUrl(), false);
    	if (cleanURL.startsWith("data")) { // browser just started
    		Printer.sh.update(state, "N/A");
    		return;
    	}
    	if (cleanURL.startsWith("www.")) {
    		cleanURL = cleanURL.replace("www.", "");
    	}
    	cleanURL = cleanURL.split("/")[0];
    	Printer.sh.update(state, cleanURL);
    }
    
    private static void jsClick(WebElement el) {
    	js.executeScript("arguments[0].click();", el);
    }
    
    private static void scrollPage(int scrolls) {
    	sendState(State.LOADING);
    	for (int i = 0; i < scrolls; i++) {
    		js.executeScript(String.format("window.scrollBy(0, %d);", 1080*i), "");
    		try { Thread.sleep(1000); } catch (InterruptedException e) { }
    	}
    	js.executeScript("window.scrollTo(0, 0);",  "");
    	sendState(State.WAITING);
    }
    
    private static String ensureSchema(String url, boolean giveSchemaBack) {
    	if (url.startsWith("https://")) {
    		if (giveSchemaBack) {
    			return url;
    		}
    		return url.replace("https://", "");
    	} else {
    		if (giveSchemaBack) {
    			return "https://" + url;
    		}
    		return url;
    	}
    }
    
    private static void waitForElementClickable(String selector) {
    	sendState(State.LOADING);
    	new WebDriverWait(cd, Duration.ofSeconds(TIMEOUT_SEC)).until(
		    ExpectedConditions.elementToBeClickable(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(EXTRA_WAIT_MS);
    	} catch (Exception e) { }
    	sendState(State.WAITING);
	}
    
    private static void waitForElementVisible(String selector) {
    	sendState(State.LOADING);
    	new WebDriverWait(cd, Duration.ofSeconds(TIMEOUT_SEC)).until(
		    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(EXTRA_WAIT_MS);
    	} catch (Exception e) { }
    	sendState(State.WAITING);
	}
}

