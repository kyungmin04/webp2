package ce.mnu.siteuser;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.springframework.http.*;
import org.springframework.core.io.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.*;
import com.google.gson.*;

@Controller
@RequestMapping(path="/siteuser")
public class SiteUserController {
	@Autowired
	private SiteUserRepository userRepository;
	
	@GetMapping(value= {"", "/"})
	public String start(Model model) {
		return "start";
	}
	
	@GetMapping(path="/signup")
	public String singup(Model model) {
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
	
	/*
	@PostMapping(path="/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<String> signup(@RequestBody SiteUser user) {
		userRepository.save(user);
		return new ResponseEntity<>("회원가입이 완료되었습니다", HttpStatus.OK);
	}*/
	
	/*public String signup(@ModelAttribute SiteUser user, Model model) {
		userRepository.save(user);
		model.addAttribute("name", user.getName());
		return "signup_done";
	}*/
	
	@PostMapping(path="/find")
	public String findUser(@RequestParam(name="email") String email, HttpSession session, Model model, RedirectAttributes rd) {
		SiteUser user = userRepository.findByEmail(email);
		if(user != null) {
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
	
	/*@PostMapping(path="/login")
	public String loginUser(@RequestParam(name="email") String email, @RequestParam(name="passwd") String passwd, HttpSession session, RedirectAttributes rd) {
		SiteUser user = userRepository.findByEmail(email);
		if(user != null) {
			if(passwd.equals(user.getPasswd())) {
				session.setAttribute("email", email);
				return "login_done";
			}
		}
		rd.addFlashAttribute("reason", "wrong password");
		return "redirect:/error";
	}*/
	
	@GetMapping(path="/login")
	public String loginForm() {
		return "login";
	}
	
	@GetMapping("/check-login")
	@ResponseBody
	public ResponseEntity<String> checkLoginStatus(HttpSession session) {
		String email = (String) session.getAttribute("email");
	    if (email != null) {
	        // 이메일로 사용자 객체 조회
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
	
	@PostMapping(path="/bbs/add")
	public String addArticle(@ModelAttribute Article article, Model model) {
		articleRepository.save(article);
		model.addAttribute("article", article);
		return "saved";
	}
	
	@GetMapping(path="/bbs")
	public String getAllArticles(@RequestParam(name="pno", defaultValue="0") String pno, Model model, HttpSession session, RedirectAttributes rd) {
		String email = (String)session.getAttribute("email");
		if(email==null) {
			rd.addFlashAttribute("reason", "login required");
			return "redirect:/error";
		}
		Integer pageNo = 0;
		if(pno != null) {
			pageNo = Integer.valueOf(pno);
		}
		Integer pageSize = 2;
		Pageable paging = PageRequest.of(pageNo, pageSize, Sort.Direction.DESC, "num");
		Page<ArticleHeader> data = articleRepository.findArticleHeaders(paging);
		model.addAttribute("articles", data);
		return "articles";
	}

	
	@GetMapping(path="/read")
	public String readArticle(@RequestParam(name="num") String num, HttpSession session, Model model) {
		Long no = Long.valueOf(num);
		Article article = articleRepository.getReferenceById(no);
		model.addAttribute("article", article);
		return "article";
	}
	
	//파일 업로드
	@PostMapping(path="/upload")
	public String upload(@RequestParam MultipartFile file, Model model) throws IllegalStateException, IOException{
		if(!file.isEmpty()) {
			String newName = file.getOriginalFilename();
			newName = newName.replace(' ', '_');
			FileDto dto = new FileDto(newName, file.getContentType());
			File upfile = new File(dto.getFileName());
			file.transferTo(upfile);
			model.addAttribute("file", dto);
		}
		return "result";
	}
	
	@GetMapping(path="upload")
	public String visitUpload() {
		return "uploadForm";
	}
	
	@Value("${spring.servlet.multipart.location}")
	String base;
	
	//파일 다운로드
	@GetMapping(path="/download")
	public ResponseEntity<Resource> download(@ModelAttribute FileDto dto) throws IOException{
		Path path = Paths.get(base + "/" + dto.getFileName());
		String contentType = Files.probeContentType(path);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(ContentDisposition.builder("attachment").filename(dto.getFileName(), StandardCharsets.UTF_8).build());
		headers.add(HttpHeaders.CONTENT_TYPE, contentType);
		Resource rsc = new InputStreamResource(Files.newInputStream(path));
		return new ResponseEntity<>(rsc, headers, HttpStatus.OK);
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
		if(pid.equals("0")) {
			String[] data = {
				"사용자 등록", "/siteuser/sample?pid=1",
				"사용자 로그인", "/siteuser/sample?pid=2",
				"게시글", "/siteuser/sample?pid=3",
				"파일 업로드", "/siteuser/upload",
				"사용자 로그아웃", "/siteuser/logout"
			};
			jo.addProperty("header", "SiteUser 첫 페이지");
		
			JsonArray ja = new JsonArray();
			for(int k = 0; k < 5; k++) {
				JsonObject jObj = new JsonObject();
				jObj.addProperty(data[2*k], data[2*k + 1]);
				ja.add(jObj);
			}
			jo.add("menus", ja);
		} else if(pid.equals("1")) {
			jo.addProperty("header", "사용자 등록");
		} else if(pid.equals("2")) {
			jo.addProperty("header", "사용자 로그인");
		} else if(pid.equals("3")) {
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
		} else if(pid.equals("4")) {
			jo.addProperty("header", "사용자 로그인");
		}
		
		System.out.println(jo.toString());
		return jo.toString();
	}
}