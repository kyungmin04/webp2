package ce.mnu.siteuser2;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long articleId;  // Article과 연관관계
    private String content;
    private String author;
    private LocalDateTime createdDate;

    // getters and setters
    public Long getId() {return id;}
	public void setId(Long i) {id = i;}
	
	public Long getArticleId() {return articleId;}
	public void setArticleId(Long a) {articleId = a;}
	
	public String getContent() {return content;}
	public void setContent(String c) {content = c;}
	
	public String getAuthor() {return author;}
	public void setAuthor(String au) {author = au;}
	
	public LocalDateTime getCreatedDate() {return createdDate;}
	public void setCreatedDate(LocalDateTime createdDate) {this.createdDate = createdDate;}
}
