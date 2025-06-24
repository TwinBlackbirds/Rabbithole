// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Example database object for SQLite database

package tbb.db.Schema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @Column(name = "id", nullable = false) 
    public String id; // youtube.com/c/?

    @Column(name = "name", nullable = false) 
    public String name;

    @Column(name = "subscriber_count", nullable = false)
    public int subscriberCount;

    @Column(name = "time_checked", nullable = false) 
    public LocalDateTime timeChecked;

    @Column(name = "times_encountered", nullable = false)
    public int timesEncountered = 0; // # of times we have seen this channel's videos already
    
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Video> videos = new HashSet<>();
    
    public Channel() { }

}
