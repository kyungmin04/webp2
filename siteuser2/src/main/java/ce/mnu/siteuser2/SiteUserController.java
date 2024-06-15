package ce.mnu.siteuser2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.google.gson.*;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping(path="/siteuser")
public class SiteUserController {
	@Autowired
    private SiteUserRepository userRepository;

    private List<String> uploadedFiles = new ArrayList<>();

    @GetMapping(value= {"", "/"})
    public String start(Model model) {
        return "start";
    }

    @GetMapping(path="/signup")
    public String signup(Model model) {
        model.addAttribute("siteuser", new SiteUser());
        return "signup_input";
    }

    @PostMapping(path="/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> signup(@RequestBody SiteUser user) {
        try {
            userRepository.save(user);
            return ResponseEntity.ok("회원가입이 완료되었습니다.");
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("이미 존재하는 계정입니다."); // HTTP 409 Conflict
        }
    }

    @PostMapping(path="/find")
    public String findUser(@RequestParam(name="email") String email, HttpSession session, Model model, RedirectAttributes rd) {
        SiteUser user = userRepository.findByEmail(email);
        if (user != null) {
            model.addAttribute("user", user);
            return "find_done";
        }
        rd.addFlashAttribute("reason", "wrong email");
        return "redirect:/error";
    }

    @GetMapping(path="/find")
    public String find() {
        return "find_user";
    }

    @PostMapping(path="/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> loginUser(@RequestBody Map<String, String> loginData, HttpSession session) {
        String email = loginData.get("email");
        String passwd = loginData.get("passwd");

        SiteUser user = userRepository.findByEmail(email);
        if (user != null && passwd.equals(user.getPasswd())) {
            session.setAttribute("email", email);
            return new ResponseEntity<>("로그인 성공", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("이메일 또는 비밀번호가 잘못되었습니다", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/check-login")
    @ResponseBody
    public ResponseEntity<String> checkLoginStatus(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email != null) {
            SiteUser user = userRepository.findByEmail(email);
            if (user != null) {
                String name = user.getName();
                return new ResponseEntity<>(name, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>("로그인되지 않음", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping(path="/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "";
    }

    @Autowired
    private ArticleRepository articleRepository;

    @GetMapping(path="/bbs/write")
    public String bbsForm(Model model) {
        model.addAttribute("article", new Article());
        return "new_article";
    }

    @PostMapping(path="/bbs/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> addArticle(@RequestBody Article article, HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return new ResponseEntity<>("로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
        }

        articleRepository.save(article);
        return new ResponseEntity<>("게시글이 저장되었습니다", HttpStatus.OK);
    }

    @GetMapping(path="/bbs")
    public ResponseEntity<List<ArticleHeader>> getAllArticles(@RequestParam(name="pno", defaultValue="0") String pno, HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        Integer pageNo = Integer.valueOf(pno);
        Integer pageSize = 2;
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.Direction.DESC, "num");
        Page<ArticleHeader> data = articleRepository.findArticleHeaders(paging);
        
        return new ResponseEntity<>(data.getContent(), HttpStatus.OK);
    }

    @GetMapping(path="/read")
    public ResponseEntity<Article> readArticle(@RequestParam(name="num") String num, HttpSession session) {
        Long no = Long.valueOf(num);
        Article article = articleRepository.getReferenceById(no);
        
        return new ResponseEntity<>(article, HttpStatus.OK);
    }

    @Value("${spring.servlet.multipart.location}")
    String base;

    // 파일 업로드 처리
    @PostMapping(path="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<String> upload(@RequestParam MultipartFile file, HttpSession session) throws IllegalStateException, IOException {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return new ResponseEntity<>("로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
        }

        if (!file.isEmpty()) {
            String newName = file.getOriginalFilename();
            newName = newName.replace(' ', '_');
            FileDto dto = new FileDto(newName, file.getContentType());
            File upfile = new File(base + "/" + dto.getFileName());
            file.transferTo(upfile);
            uploadedFiles.add(newName); // Add the file to the uploaded files list
            return new ResponseEntity<>("파일이 업로드되었습니다", HttpStatus.OK);
        }
        return new ResponseEntity<>("파일이 비어있습니다", HttpStatus.BAD_REQUEST);
    }

    // 파일 다운로드
    @GetMapping(path="/download")
    public ResponseEntity<Resource> download(@RequestParam String fileName, HttpSession session) throws IOException {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Path path = Paths.get(base + "/" + fileName);
        if (Files.notExists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String contentType = Files.probeContentType(path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName, StandardCharsets.UTF_8).build());
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        Resource rsc = new InputStreamResource(Files.newInputStream(path));
        return new ResponseEntity<>(rsc, headers, HttpStatus.OK);
    }

    // 업로드된 파일 목록 조회
    @GetMapping(path="/uploaded-files")
    @ResponseBody
    public ResponseEntity<List<String>> getUploadedFiles(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(uploadedFiles, HttpStatus.OK);
    }

    @GetMapping(path="/sample")
    public String sample(@RequestParam(defaultValue="0") String pid, Model model) {
        model.addAttribute("pid", pid);
        return "sample";
    }

    @ResponseBody
    @GetMapping(path="/json-data")
    public String jsonData(@RequestParam(defaultValue="0") String pid) {
        JsonObject jo = new JsonObject();
        if (pid.equals("0")) {
            String[] data = {
                "사용자 등록", "/siteuser/sample?pid=1",
                "사용자 로그인", "/siteuser/sample?pid=2",
                "게시글", "/siteuser/sample?pid=3",
                "파일 업로드", "/siteuser/sample?pid=4",
                "사용자 로그아웃", "/siteuser/logout"
            };
            jo.addProperty("header", "SiteUser 첫 페이지");
        
            JsonArray ja = new JsonArray();
            for (int k = 0; k < 5; k++) {
                JsonObject jObj = new JsonObject();
                jObj.addProperty(data[2*k], data[2*k + 1]);
                ja.add(jObj);
            }
            jo.add("menus", ja);
        } else if (pid.equals("1")) {
            jo.addProperty("header", "사용자 등록");
        } else if (pid.equals("2")) {
            jo.addProperty("header", "사용자 로그인");
        } else if (pid.equals("3")) {
            jo.addProperty("header", "게시글 목록");
            Page<ArticleHeader> data = articleRepository.findArticleHeaders(PageRequest.of(0, 10));
            JsonArray articleArray = new JsonArray();
            for (ArticleHeader article : data) {
                JsonObject articleObj = new JsonObject();
                articleObj.addProperty("num", article.getNum());
                articleObj.addProperty("title", article.getTitle());
                articleObj.addProperty("author", article.getAuthor());
                articleArray.add(articleObj);
            }
            jo.add("articles", articleArray);
        } else if (pid.equals("4")) {
            jo.addProperty("header", "파일 업로드");
        } else if (pid.equals("5")) {
            jo.addProperty("header", "글 작성");
        }
        
        System.out.println(jo.toString());
        return jo.toString();
    }
}
