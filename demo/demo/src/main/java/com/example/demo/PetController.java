package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/") 
@SuppressWarnings("all") 
public class PetController {

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
        String apiKey = "AIzaSyCuFEs8bg3sFmtUvNXQHwkpvao-TS0fYiQ"; 
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/google/gemini-1.5-flash:generateContent?key=" + apiKey;
            
            // 強化 Prompt：加入 bodyType 判斷
            String prompt = "使用者說：'" + input + "'。分析需求並回傳 JSON。規則：\n" +
                            "1. species: '貓' 或 '狗'。\n" +
                            "2. age: '幼貓'、'幼犬'、'全年齡'、'熟齡' 或 '高齡'。\n" +
                            "3. useFor: 挑食、體重管理等 Excel 標籤，沒提到回傳 '一般'。\n" +
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

        String apiKey = "AIzaSyDviUdMMqGJUbHeadWIjlT4iNH6sN5Br2s"; // 建議之後改成環境變數

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        try {
            String prompt = "你是一位專業獸醫助理，請用自然、實用、簡單易懂的方式回答使用者問題：\n"
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


    // ✅ 修正：使用 AssistantRequest
    @PostMapping("/api/assistant")
    public String assistant(@RequestBody AssistantRequest request) {
        return askExternalGemini(request.getQuestion());
    }
    
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
        @PostMapping("/api/daily/summary")
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
}