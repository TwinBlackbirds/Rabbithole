// Name: Michael Amyotte
// Date: 6/16/25
// Purpose: Sqlite driver for Rabbithole project

package tbb.db.Driver;

// database table objects
import tbb.db.Schema.Channel;
import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;
import tbb.db.Schema.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;



public class Sqlite {
	private Logger log;
	private SessionFactory db; // do not expose to users, instead write methods such as the writeChannel one below
	
	public Sqlite(Logger log) {
		this(log, false);
	}
	
	public Sqlite(Logger log, boolean deleteDB) {
		this.log = log;
		
		Configuration config = new Configuration()
				   .configure(); // use hibernate.cfg.xml
		
		// debug feature
		if (deleteDB) {
			try {
				log.Write(LogLevel.BYPASS, "DEBUG ALERT: Deleting database");
				Files.deleteIfExists(Paths.get("./database.sqlite"));
				
			} catch (IOException e) { 
				log.Write(LogLevel.BYPASS, "Could not delete database! " + e);
			}		
			
		}
		log.Write(LogLevel.DBG, "Dialect = " + config.getProperty("hibernate.dialect"));
		this.db = config.buildSessionFactory();
	}
	
	// example write method
	public void writeChannel(Channel c) throws Exception {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			s.persist(c);
			s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "WriteChannel operation failed! " + e);
		}
	}

	public long startSession() {
		// create a session object and return the ID
		tbb.db.Schema.Session s = new tbb.db.Schema.Session();
		s.timeStarted = LocalDateTime.now();
		try (Session _s = db.openSession()) {
			_s.beginTransaction();
			_s.persist(s);
			_s.getTransaction().commit();
			return s.id; // auto-generated
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not instantiate session!" + e);
			return 0;
		}
	}
	
	public void closeSession(long sessionID) {
		try (Session _s = db.openSession()) {
			// get old session and set the time terminated
			tbb.db.Schema.Session s = _s.find(tbb.db.Schema.Session.class, sessionID);
			if (s == null) {
				throw new Exception("SessionID not found");
			}
			s.timeTerminated = LocalDateTime.now();
			
			// push update to database
			_s.beginTransaction();
			_s.merge(s);
			_s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not close session!" + e);
		}
	}
}
