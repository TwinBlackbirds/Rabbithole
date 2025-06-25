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
	
	
	/*
	 * 
	 * Channel Operations
	 * 
	 */
	
	public void writeChannel(Channel c) throws Exception {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			s.persist(c);
			s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "WriteChannel operation failed! " + e);
		}
	}
	
	public void updateChannel(Channel c) throws Exception {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			s.merge(c);
			s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "UpdateChannel operation failed! " + e);
		}
	}
		
	
	public Channel getChannel(String ID) {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			Channel result = s.find(Channel.class, ID);
			s.getTransaction().commit();
			return result;
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "findChannel operation failed! " + e);
		}
		return null;
	}
	
	public boolean findChannel(String ID) {
		try (Session s = db.openSession()){ // try-with-resources
			Channel result = s.find(Channel.class, ID);
			if (result != null) { return true; }
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "findChannel operation failed! " + e);
		}
		return false;
	}
	
	
	
	/*
	 * 
	 * Session Operations
	 * 
	 */
	
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
	
	private tbb.db.Schema.Session getSession(long sessionID) {
		try (Session _s = db.openSession()) {
			return _s.find(tbb.db.Schema.Session.class, sessionID);
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "Could not get session with ID " + sessionID + "! " + e);
		}
		return null;
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
	
	
	/*
	 * 
	 * Video Operations
	 * 
	 */
	public void writeVideo(Video v) {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			s.persist(v);
			s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "writeVideo operation failed! " + e);
		}
	}
	
	public Video getVideo(String ID) {
		try (Session s = db.openSession()){ // try-with-resources
			Video result = s.find(Video.class, ID);
			return result;
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "getVideo operation failed! " + e);
		}
		return null;
	}
	
	public boolean findVideo(String ID) {
		try (Session s = db.openSession()){ // try-with-resources
			Video result = s.find(Video.class, ID);
			if (result != null) { return true; }
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "findVideo operation failed! " + e);
		}
		return false;
	}
	
	public void updateVideo(Video v) {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			s.merge(v);
			s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "updateVideo operation failed! " + e);
		}
	}
	
	/*
	 * 
	 * Session x Video operations
	 * 
	 */
	public void addVideo(long sessionID, String videoID) throws Exception {
		
		try (Session _s = db.openSession()){ // try-with-resources
			
			String hql = "SELECT COUNT(v) FROM Session s JOIN s.videos v WHERE s.id = :sessionId AND v.id = :videoId";
			Long count = _s.createQuery(hql, Long.class)
			               .setParameter("sessionId", sessionID)
			               .setParameter("videoId", videoID)
			               .getResultCount();

			if (count > 0) {
			    // Relation already exists
				log.Write(LogLevel.INFO, String.format("Skipping adding video %s to session %d that was already watched", videoID, sessionID));
				return;
			}
			
			_s.beginTransaction();
			Video v = _s.find(Video.class, videoID);
			tbb.db.Schema.Session s = _s.find(tbb.db.Schema.Session.class, sessionID);
			if (v == null) {
				throw new Exception("Video with ID " + videoID + " not found!");
			}
			s.videos.add(v);
			// make new record
			_s.getTransaction().commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "addVideo operation failed! " + e);
		}
	}
	
	public boolean findSessionVideo(long sessionID, String videoID) throws Exception {
		try (Session _s = db.openSession()) { // try-with-resources
			
			String hql = "SELECT COUNT(v) FROM Session s JOIN s.videos v WHERE s.id = :sessionId AND v.id = :videoId";
			Long count = _s.createQuery(hql, Long.class)
			               .setParameter("sessionId", sessionID)
			               .setParameter("videoId", videoID)
			               .getResultCount();

			if (count > 0) {
				return true;
			}
		}
		return false;
	}
}
