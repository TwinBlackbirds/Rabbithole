// Rabbithole
// Author: Michael Amyotte (twinblackbirds)
// Date: 6/23/25
// Purpose: Rebuilt data collection bot for data analysis
// 

package tbb.apps.Rabbithole;

import java.time.Duration;
import java.util.Scanner;

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
    	Printer.startBox();
    	
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
    	cd.get("https://youtube.com");
    	sendState(State.NAVIGATING);
    	
    	Thread.sleep(5000);
    }
    
    // queries the page every second until the DOM reports readyState = complete
    private static void waitUntilPageLoaded() {
    	String pageName = cd.getTitle();
    	log.Write(LogLevel.INFO, String.format("Waiting for page '%s' to load", pageName));
    	new WebDriverWait(cd, Duration.ofSeconds(1)).until(
                webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    	log.Write(LogLevel.INFO, "Page loaded");
    }
    
    private static String loopUntilInput(String prompt, String confirmationFmt) {
    	// loop and wait for a valid input from the user (to initiate searching)
    	Scanner s = new Scanner(System.in);
    	String searchTerm = "";
    	try {
    		// read input
    		while (true) {
        		System.out.print(prompt);
        		String input = s.nextLine();
        		if (input == null || input.trim().equals("")) {
        			continue;
        		}
        		System.out.flush();
        		System.out.print(String.format(confirmationFmt, input));
        		String confirm = s.nextLine();
        		if (confirm.trim().toLowerCase().equals("y")) {
        			break;
        		}
        	}

    	}
    	finally { 
    		// make sure scanner gets closed even if we get an interrupt
    		s.close();
    		log.Write(LogLevel.DBG, "Scanner closed");
    	}
    	return searchTerm;
    }
    
    private static void sendState(State state) {
    	String cleanURL = ensureSchema(cd.getCurrentUrl(), false);
    	if (cleanURL.startsWith("www.")) {
    		cleanURL = cleanURL.replace("www.", "");
    	}
    	cleanURL = cleanURL.split("/")[0];
    	Printer.sh.update(state, cleanURL);
    }
    
    private static void jsClick(WebElement el) {
    	js.executeScript("arguments[0].click();", el);
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
    	new WebDriverWait(cd, Duration.ofSeconds(TIMEOUT_SEC)).until(
		    ExpectedConditions.elementToBeClickable(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(EXTRA_WAIT_MS);
    	} catch (Exception e) { }
	}
    
    private static void waitForElementVisible(String selector) {
    	new WebDriverWait(cd, Duration.ofSeconds(TIMEOUT_SEC)).until(
		    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(EXTRA_WAIT_MS);
    	} catch (Exception e) { }
	}
}

