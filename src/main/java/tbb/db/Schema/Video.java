package tbb.db.Schema;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
public class Video {
	
	@Id
	@Column(name = "id", nullable = false)
	public String id;
	
	@Column(name = "title", nullable = false)
	public String title;
	
	
	@Column(name = "times_encountered", nullable = false)
	public int timesEncountered;
	
	@ManyToOne
	@JoinColumn(name = "channel_id", nullable = false)
	public Channel channel;
	
	
	@ManyToMany
	@JoinTable(
			name = "video_sessions",
			joinColumns = @JoinColumn(name = "video_id"),
			inverseJoinColumns = @JoinColumn(name = "session_id") 
	)
	public Set<Session> sessions = new HashSet<>();
	
	public Video() { }
}
