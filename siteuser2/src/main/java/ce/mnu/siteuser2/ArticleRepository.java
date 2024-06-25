package ce.mnu.siteuser2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Query(value="SELECT num, title, author FROM article", nativeQuery=true)
    Page<ArticleHeader> findArticleHeaders(Pageable pageable);

    // Modify the method to use 'body' instead of 'content'
    List<Article> findByTitleContainingOrBodyContaining(String title, String body);
    
    Optional<Article> findByNum(Long num);
}