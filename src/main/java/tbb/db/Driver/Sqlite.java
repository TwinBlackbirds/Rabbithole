// Name: Michael Amyotte
// Date: 6/16/25
// Purpose: SQLite ORM example driver for JScraper template

package tbb.db.Driver;

// database table objects
import tbb.db.Schema.Channel;
import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;
import tbb.db.Schema.*;

import java.time.LocalDateTime;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;



public class Sqlite {
	private Logger log;
	private boolean dbg;
	private SessionFactory db; // do not expose to users, instead write methods such as the writeChannel one below
	
	public Sqlite(Logger log) {
		this(log, false);
	}
	
	public Sqlite(Logger log, boolean dbg) {
		this.log = log;
		this.dbg = dbg;
		
		Configuration config = new Configuration()
				   .configure(); // use hibernate.cfg.xml
		
		if (this.dbg) {
			System.out.println("Dialect = " + config.getProperty("hibernate.dialect"));	
		}
		
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

	public Number startSession() {
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
