package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/")
@SuppressWarnings("all")
public class PetController {
    @PostMapping(
        value = "/api/assistant",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"
    )
    public ResponseEntity<String> askAssistant(@RequestBody Map<String, Object> body) {

        try {
            String question = String.valueOf(body.getOrDefault("question", "")).trim();

            if (question.isEmpty()) {
                return ResponseEntity.badRequest().body("缺少 question 欄位");
            }

            // 呼叫 Gemini，取得 AI 回答
            String answer = callGeminiAssistant(question);

            // 把 Gemini 的回答回傳給 Android 前端
            return ResponseEntity.ok(answer);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("AI 回答失敗：" + e.getMessage());
        }
    }

    private String callGeminiAssistant(String question) throws Exception {

        // 測試階段可以先直接放 API Key
        // 正式版本不要直接寫死在程式碼裡
        String apiKey = "api";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt =
                "你是一位寵物照護 AI 助手，請用繁體中文回答。\n" +
                "請根據使用者的問題，提供清楚、溫和、實用的寵物照護建議。\n" +
                "如果問題可能涉及疾病、呼吸困難、中毒、持續嘔吐、抽搐、流血、精神不佳等狀況，請提醒使用者盡快帶寵物就醫。\n" +
                "不要假裝自己是獸醫，也不要做絕對診斷。\n" +
                "回答請控制在 3 到 6 句，適合顯示在手機聊天畫面。\n\n" +
                "使用者問題：" + question;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024
                )
        );

        String jsonBody = new Gson().toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Gemini API 錯誤：" + response.statusCode() + "\n" + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        JsonArray candidates = root.getAsJsonArray("candidates");

        if (candidates == null || candidates.size() == 0) {
            return "抱歉，我目前沒有取得 AI 回覆，請稍後再試。";
        }

        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        JsonObject content = firstCandidate.getAsJsonObject("content");
        JsonArray parts = content.getAsJsonArray("parts");

        if (parts == null || parts.size() == 0) {
            return "抱歉，AI 回覆內容為空。";
        }

        JsonObject firstPart = parts.get(0).getAsJsonObject();

        if (!firstPart.has("text")) {
            return "抱歉，AI 回覆格式不正確。";
        }

        return firstPart.get("text").getAsString().trim();
    }
    

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //註冊
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody UserRequest data) {
        Map<String, Object> res = new HashMap<>();
        if (data.username == null || data.password == null || data.email == null) {
            res.put("status", "fail");
            res.put("message", "缺少欄位");
            return res;
        }
        String checkSql = "SELECT COUNT(*) FROM Users WHERE email = ? OR username = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, data.email, data.username);
        if (count != null && count > 0) {
            res.put("status", "fail");
            res.put("message", "帳號或Email已存在");
            return res;
        }
        String insertSql = "INSERT INTO Users (email, username, password) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertSql, data.email, data.username, data.password);
        res.put("status", "success");
        return res;
    }
    //登入
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody UserRequest data) {
        Map<String, Object> res = new HashMap<>();
        String sql = "SELECT UserID FROM Users WHERE Email = ? AND Password = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, data.email, data.password);
        if (!users.isEmpty()) {
            res.put("status", "success");
            res.put("user_id", users.get(0).get("UserID"));
        } else {
            res.put("status", "fail");
        }
        return res;
    }

    //更新密碼
    @PostMapping("/update_password")
    public Map<String, Object> updatePassword(@RequestBody UserRequest data) {
        String sql = "UPDATE Users SET Password = ? WHERE Email = ?";
        int rows = jdbcTemplate.update(sql, data.password, data.email);
        return rows > 0 ? Map.of("status", "success") : Map.of("status", "fail", "message", "找不到該 Email");
    }

    // 3. 新增寵物 (加入計算邏輯)
    @PostMapping("/add")
    public Map<String, Object> addPet(@RequestBody PetRequest data) {
        Map<String, Object> res = new HashMap<>();

        // 1. 基礎檢查
        if (data.UserID == null || data.PetName == null) {
            res.put("status", "fail");
            res.put("message", "UserID 或 寵物姓名 缺失");
            return res;
        }
        // 2. 限制 Species 只能是貓或狗
        if (!"貓".equals(data.Species) && !"狗".equals(data.Species)) {
            res.put("status", "fail");
            res.put("message", "物種格式錯誤"); // 這裡簡化訊息即可
            return res;
        }
        // 3. 處理 BodyType：只有狗有大中小，貓則設為 null

        String finalBodyType = data.BodyType;
        if ("貓".equals(data.Species)) {
            finalBodyType = null; // 貓咪不需要體態選單，強制設為空值
        } else {
            // 如果前端選單是 [小型, 中型, 大型]，這裡做一個基本的保護
            if (finalBodyType == null || finalBodyType.isEmpty()) {
                finalBodyType = "中型";
            }
        }
        // 4. 性別檢查 (如果需要更嚴謹)
        if (!"公".equals(data.Gender) && !"母".equals(data.Gender)) {
            // 也可以視情況決定是否要擋掉，或預設一個值
        }
        // 5. 執行 SQL 插入
        String sql = "INSERT INTO Pets (UserID, PetName, Species, Birthday, Gender, IsSterilized, Weight) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,

            data.UserID, data.PetName, data.Gender, data.Species,

            data.Birthday, data.Weight, data.FoodID, data.Activity,

            finalBodyType, data.IsSterilized ? 1 : 0);



        // 5. 計算建議量 (貓狗係數不同)

        double weight = (data.Weight != null) ? data.Weight : 0;
        double rer = 70 * Math.pow(weight, 0.75);
        double factor = 1.0;

        // 加上 pet 字眼區隔
        boolean petIsSterilized = (data.IsSterilized != null && data.IsSterilized);

        if ("狗".equals(data.Species)) {
            factor = petIsSterilized ? 1.6 : 1.8;
            if ("大型".equals(finalBodyType)) factor += 0.1; // 體型加成範例
        } else {
            factor = (data.IsSterilized) ? 1.2 : 1.4;
        }

        res.put("status", "success");
        res.put("suggested_calories", Math.round(rer * factor));
        res.put("suggested_water_ml", Math.round(weight * 50));
        return res;
    }

    @PostMapping("/recommend_foods")
    public List<Map<String, Object>> recommendFoods(@RequestBody PetRequest pet) {
        // 確保 pet 不是 null 避免警告
        if (pet == null) return new java.util.ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM Food WHERE 1=1 ");
        List<Object> params = new java.util.ArrayList<>();

        // 這裡使用 pet.getSpecies() 或 pet.Species 視你的 PetRequest 定義而定
        // 假設你的 PetRequest 變數是大寫開頭且為 public
        if (pet.Species != null) {
            sql.append(" AND (PetType = ? OR PetType = '全適用') ");
            params.add(pet.Species);
        }

        if (pet.Birthday != null) {
            sql.append(" AND (AgeGroup LIKE ? OR AgeGroup = '全年齡') ");
            params.add("%成年%");
        }
        if (pet.Activity != null) {

            sql.append(" AND (UseFor LIKE ? OR Flavor LIKE ?) ");
            String keyword = "%" + pet.Activity + "%";
            params.add(keyword);
            params.add(keyword);
        }

        // 將 StringBuilder 轉為 String，並將 List 轉為 Object 陣列
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @GetMapping("/ai_recommend")
    public Map<String, Object> aiRecommend(@RequestParam String userInput) {
        // 1. 取得 AI 解析標籤
        Map<String, String> aiTags = callGeminiToExtractTags(userInput);
        System.out.println("=== aiTags: " + aiTags);

        String species = aiTags.getOrDefault("species", "狗");

        String age = aiTags.getOrDefault("age", "全年齡");
        String useFor = aiTags.getOrDefault("useFor", "一般");
        String bodyType = aiTags.getOrDefault("bodyType", "全適用");

        // 2. 構建「基礎」SQL (WHERE 1=1 是動態拼接的神技)
        StringBuilder sql = new StringBuilder("SELECT * FROM Food WHERE (PetType = ? OR PetType = '全適用') ");
        List<Object> params = new ArrayList<>();
        params.add(species);

        // 3. 動態添加：體態 (BodyType)
        sql.append(" AND (BodyType = ? OR BodyType = '全適用') ");
        params.add(bodyType);

        // 4. 動態添加：年齡層 (只有非「全年齡」才過濾，讓成犬也能被全年齡搜到)
        if (!age.equals("全年齡")) {
            sql.append(" AND (AgeGroup LIKE ? OR AgeGroup = '全年齡') ");
            params.add("%" + age + "%");
        }

        // 5. 動態添加：需求 (UseFor)
        if (!useFor.equals("一般")) {
            sql.append(" AND UseFor LIKE ?");
            params.add("%" + useFor + "%");
        }

        // 6. 執行查詢
        List<Map<String, Object>> foods = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // 7. 回傳結果
        Map<String, Object> response = new HashMap<>();
        response.put("recommended_foods", foods);
        response.put("ai_analysis", "AI 分析 - 種類：" + species + "，年齡：" + age +
                                    "，體態：" + bodyType + "，需求：" + useFor);

        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callGeminiToExtractTags(String input) {

        String apiKey = "api";
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            // 強化 Prompt：加入 bodyType 判斷
            String prompt = "使用者說：'" + input + "'。分析需求並回傳 JSON。規則：\n" +
                        "1. species: '貓' 或 '狗'。\n" +
                        "2. age: '幼貓'、'幼犬'、'全年齡'、'熟齡' 或 '高齡'。\n" +
                        "3. useFor: 從以下關鍵字選一個最相關的，沒提到回傳 '一般'：\n" +
                        "   挑食、毛髮、皮膚、體重管理、消化、腸胃、免疫、關節、心臟、腎臟、泌尿、大腦發育、肌肉\n" +
                        "4. bodyType: 提到'胖'且是狗回傳 '胖犬'；是貓回傳 '胖貓'。否則回傳 '全適用'。\n" +
                        "回傳純 JSON 範例：{\"species\":\"狗\",\"age\":\"全年齡\",\"useFor\":\"一般\",\"bodyType\":\"胖犬\"}";
            String payload = "{\"contents\": [{\"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println("Gemini Raw Response: " + body);

            if (body != null && body.contains("candidates")) {
                JsonObject jsonResponse = JsonParser.parseString(body).getAsJsonObject();
                String aiText = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject()
                                .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                                .get("text").getAsString().replace("```json", "").replace("```", "").trim();
                return new Gson().fromJson(aiText, Map.class);
            } else {
                throw new Exception("API Error");
            }
        } catch (Exception e) {
            // --- 萬能防呆 Catch 區塊 ---
            Map<String, String> fallback = new HashMap<>();
            String species = (input.contains("貓") || input.contains("喵")) ? "貓" : "狗";
            fallback.put("species", species);
            // 年齡判斷
            if (input.contains("小") || input.contains("幼")) {
                fallback.put("age", species.equals("貓") ? "幼貓" : "幼犬");
            } else if (input.contains("老") || input.contains("大")) {
                fallback.put("age", "高齡");
            } else {
                fallback.put("age", "全年齡");
            }
            // 體態判斷 (關鍵！)
            if (input.contains("胖") || input.contains("重")) {
                fallback.put("bodyType", species.equals("貓") ? "胖貓" : "胖犬");
            } else {
                fallback.put("bodyType", "全適用");
            }
            fallback.put("useFor", input.contains("挑食") ? "挑食" : "一般");
            return fallback;
        }
    }    
    // 在類別內定義這個方法，紅線就會消失
        private String askExternalGemini(String question) {

        String apiKey = "api"; // 建議之後改成環境變數

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;        
        try{
            String prompt = "你是一位專業獸醫助理，請用自然、實用、簡單易懂的方式回答使用者問題，並一律使用繁體中文(台灣用語)回答，禁止使用簡體中文：\n"
                    + question;

            String payload =
                    "{ \"contents\": [{ \"parts\": [{\"text\": \"" +
                            prompt.replace("\"", "\\\"").replace("\n", "\\n") +
                            "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            System.out.println("=== Gemini response: " + body);

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            return json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text")
                    .getAsString();

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 暫時無法回應，請稍後再試";
        }
    }    
    private String askExternalGeminiWithImage(String question, MultipartFile image) throws IOException {

    String base64Image = Base64.getEncoder().encodeToString(image.getBytes());

    String mimeType = image.getContentType();
    if (mimeType == null || mimeType.isEmpty()) {
        mimeType = "image/jpeg";
    }

    System.out.println("mimeType = " + mimeType);
    System.out.println("base64 length = " + base64Image.length());

        String apiKey = "api"; // 建議之後改成環境變數

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;        

    Map<String, Object> textPart = new HashMap<>();
    textPart.put("text", question);

    Map<String, Object> inlineData = new HashMap<>();
    inlineData.put("mime_type", mimeType);
    inlineData.put("data", base64Image);

    Map<String, Object> imagePart = new HashMap<>();
    imagePart.put("inline_data", inlineData);

    Map<String, Object> content = new HashMap<>();
    content.put("parts", List.of(textPart, imagePart));

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("contents", List.of(content));

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

    Map body = response.getBody();

    System.out.println("Gemini image response body: " + body);

    try {
        List candidates = (List) body.get("candidates");
        Map firstCandidate = (Map) candidates.get(0);

        Map contentMap = (Map) firstCandidate.get("content");
        List parts = (List) contentMap.get("parts");
        Map firstPart = (Map) parts.get(0);

        return firstPart.get("text").toString();

    } catch (Exception e) {
        e.printStackTrace();
        return "Gemini 回傳格式解析失敗：" + body;
    }
}

    // 5. 獲取飼料選單 (對應紫色區塊)
    @GetMapping("/get_foods")
    public List<Map<String, Object>> getFoods() {
        return jdbcTemplate.queryForList("SELECT FoodID, Brand, Flavor, Calories FROM Food");
    }

    // 1. 取得所有曾經輸入過的零食清單 (一進入頁面就呼叫)
    @GetMapping("/get_snacks")
    public List<Map<String, Object>> getSnacks() {
        // 從 Snacks 表撈出所有紀錄，最新的排在最前面
        String sql = "SELECT SnackID, Name, Calories FROM Snacks ORDER BY SnackID DESC";
        return jdbcTemplate.queryForList(sql);
    }

    // 2. 處理零食點擊或手動新增
    @PostMapping("/add_snack_record")
    public Map<String, Object> addSnackRecord(@RequestBody Map<String, Object> data) {
        Map<String, Object> res = new HashMap<>();
        String name = (String) data.get("Name");
        // 確保熱量轉為 Double 處理
        Double calories = Double.valueOf(data.get("Calories").toString());

        // 【核心邏輯】：檢查這筆零食是否已經存在於 Snacks 表
        String checkSql = "SELECT COUNT(*) FROM Snacks WHERE Name = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, name);

        if (count == null || count == 0) {
            // 如果是「新的零食」，就存入資料庫，下次使用者進來就會直接看到它
            String insertSql = "INSERT INTO Snacks (Name, Calories) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, name, calories);
            res.put("message", "已存入新零食紀錄");
        } else {
            res.put("message", "使用現有零食紀錄");
        }

        // 這裡通常還會有一段存入「今日攝取紀錄」的 SQL，例如：
        // String logSql = "INSERT INTO DailyFoodLogs (PetID, FoodName, Calories, Date) VALUES (?, ?, ?, GETDATE())";
        // jdbcTemplate.update(logSql, data.get("PetID"), name, calories);

        res.put("status", "success");
        return res;
    }

    //就醫紀錄
    @PostMapping("/add_medical")
    public Map<String, Object> addMedical(@RequestBody Map<String, Object> data) {
        String sql = "INSERT INTO Medical (PetID, Date, Category, Description) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, data.get("PetID"), data.get("Date"), data.get("Category"), data.get("Description"));
        return Map.of("status", "success");
    }

    //行事曆
    @PostMapping("/events/add")
    public Map<String, Object> addCalendarEvent(@RequestBody Map<String, Object> data) {
        // 影片欄位：日期(Date)、時間(Time)、標題/內容(Title)
        String sql = "INSERT INTO Events (UserID, PetID, EventDate, EventTime, Title) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,

            data.get("UserID"),
            data.get("PetID"),
            data.get("EventDate"),
            data.get("EventTime"),
            data.get("Title")
        );
        return Map.of("status", "success", "message", "行程已加入行事曆");
    }

    // 4. 取得行事曆列表：顯示在影片中的日曆下方
    @GetMapping("/events/{userId}")
    public List<Map<String, Object>> getEvents(@PathVariable int userId) {
        String sql = "SELECT * FROM Events WHERE UserID = ? ORDER BY EventDate ASC, EventTime ASC";
        return jdbcTemplate.queryForList(sql, userId);
    }


    //飲食圖表  
    @GetMapping("/get_week_food/{petId}")
    public List<Map<String, Object>> getWeekFood(@PathVariable int petId) {

        String sql = """
            SELECT
                CAST(RecordDate AS DATE) as Date,
                SUM(Calories) as total_calories
            FROM DailyFood
            WHERE PetId = ?
            AND RecordDate >= DATEADD(day, -7, GETDATE())
            GROUP BY CAST(RecordDate AS DATE)
            ORDER BY Date ASC
        """;

        return jdbcTemplate.queryForList(sql, petId);
    }
    //飲水圖表
    @GetMapping("/get_week_water/{petId}")
    public List<Map<String, Object>> getWeekWater(@PathVariable int petId) {

        String sql = """
            SELECT
                CAST(RecordDate AS DATE) as Date,
                SUM(WaterML) as total_water
            FROM DailyWater
            WHERE PetId = ?
            AND RecordDate >= DATEADD(day, -7, GETDATE())
            GROUP BY CAST(RecordDate AS DATE)
            ORDER BY Date ASC
        """;

        return jdbcTemplate.queryForList(sql, petId);
    }


@PostMapping(value = "/api/assistant/image")
public String assistantWithImage(
        @RequestParam("question") String question,
        @RequestParam("image") MultipartFile image
) {
    try {
        System.out.println("=== assistant image called, question: " + question);
        System.out.println("image name: " + image.getOriginalFilename());
        System.out.println("image type: " + image.getContentType());
        System.out.println("image size: " + image.getSize());

        return askExternalGeminiWithImage(question, image);

    } catch (HttpClientErrorException e) {
        e.printStackTrace();

        return "Gemini HTTP 錯誤："
                + e.getStatusCode()
                + "\n\nGemini 回傳內容：\n"
                + e.getResponseBodyAsString();

    } catch (Exception e) {
        e.printStackTrace();

        return "後端錯誤："
                + e.getClass().getName()
                + " - "
                + e.getMessage();
    }
}

    @CrossOrigin(origins = "*")    
    @PostMapping("/api/daily/food")
        public Map<String, Object> addFood(@RequestBody Map<String, Object> req) {
            String sql = "INSERT INTO DailyFood (pet_id, calories) VALUES (?, ?)";
            jdbcTemplate.update(sql, req.get("pet_id"), req.get("calories"));
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "進食資料已存入 Azure SQL");
            return response;
        }

        // 2. 新增今日飲水量 (由硬體水盆或 App 呼叫)
        @PostMapping("/api/daily/water")
        public Map<String, Object> addWater(@RequestBody Map<String, Object> req) {
            String sql = "INSERT INTO DailyWater (pet_id, water_ml) VALUES (?, ?)";
            jdbcTemplate.update(sql, req.get("pet_id"), req.get("water_ml"));          
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "飲水資料已存入 Azure SQL");
            return response;
        }

        // 3. 更新每日總結 (通常由 App 計算達標率後呼叫)
        @PostMapping("/summary")
        public Map<String, Object> addSummary(@RequestBody Map<String, Object> req) {
            String sql = "INSERT INTO DailySummary (pet_id, food_id, water_id, is_goal_achieved) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql,
                req.get("pet_id"),
                req.get("food_id"),
                req.get("water_id"),
                req.get("is_goal_achieved")
            );
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return response;
        }
    public class AssistantController {
    // 純文字 AI 問答 API
    @PostMapping(
            value = "/assistant",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> askAssistant(@RequestBody Map<String, Object> body) {

        String question = String.valueOf(body.getOrDefault("question", "")).trim();

        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body("缺少 question 欄位");
        }

        // 先測試是否成功連線
        return ResponseEntity.ok("後端已收到你的問題：" + question);
    }

    // 圖片 / 檔案 AI 問答 API
    @PostMapping(
            value = "/assistant/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> askAssistantWithImage(
            @RequestParam("question") String question,
            @RequestParam("image") MultipartFile image
    ) {

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("缺少 question 欄位");
        }

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("缺少 image 檔案");
        }

        return ResponseEntity.ok(
                "後端已收到附檔問題：\n" +
                "問題：" + question + "\n" +
                "檔名：" + image.getOriginalFilename() + "\n" +
                "大小：" + image.getSize() + " bytes"
        );
    }
}    
}