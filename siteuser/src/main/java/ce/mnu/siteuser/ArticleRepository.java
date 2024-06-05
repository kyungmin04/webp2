package ce.mnu.siteuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
	@Query(value="SELECT num, title, author FROM article", nativeQuery=true)
	Page<ArticleHeader> findArticleHeaders(Pageable pageable);
}