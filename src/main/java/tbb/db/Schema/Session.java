package tbb.db.Schema;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;


@Entity
@Table(name = "sessions")
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long id;
	
	@Column(name = "time_started", nullable = false)
	public LocalDateTime timeStarted;
	
	@Column(name = "time_terminated", nullable = true)
	public LocalDateTime timeTerminated;
	
	@ManyToMany(mappedBy = "sessions")
	public Set<Video> videos = new HashSet<>();
	
	public Session() { }
}
