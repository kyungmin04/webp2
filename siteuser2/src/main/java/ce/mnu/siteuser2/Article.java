package ce.mnu.siteuser2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

@Entity
public class Article {
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long num;

    @Column(length = 20, nullable = false)
    private String author;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 2048)
    private String body;

    @Column(nullable = false)
    private int likes;

    @Column(nullable = false)
    private int dislikes;

    @ElementCollection
    private List<String> files = new ArrayList<>();
    
    @ElementCollection
    @MapKeyColumn(name = "user_id")
    @Column(name = "reaction")
    private Map<String, String> userReactions = new HashMap<>();
    
    public Long getNum() { return num; }
    public void setNum(Long n) { num = n; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String e) { author = e; }
    
    public String getTitle() { return title; }
    public void setTitle(String e) { title = e; }
    
    public String getBody() { return body; }
    public void setBody(String n) { body = n; }

    public int getLikes() { return likes; }
    public void setLikes(int l) { likes = l; }
    
    public int getDislikes() { return dislikes; }
    public void setDislikes(int d) { dislikes = d; }
    
    public List<String> getFiles() { return files; }
    public void setFiles(List<String> f) { files = f; }
    
    public Map<String, String> getUserReactions() { return userReactions; }
    public void setUserReactions(Map<String, String> ur) { userReactions = ur; }
}
