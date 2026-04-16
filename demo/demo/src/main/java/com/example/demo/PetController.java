package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/") 
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
    //@PostMapping("/update_password") 
    //public Map<String, Object> updatePassword(@RequestBody UserRequest data) {
        //String sql = "UPDATE Users SET Password = ? WHERE Email = ?";
       // int rows = jdbcTemplate.update(sql, data.password, data.email);
        //return rows > 0 ? Map.of("status", "success") : Map.of("status", "fail", "message", "找不到該 Email");
   // }

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
        String sql = "INSERT INTO Pets (UserID, PetName, Gender, Species, Birthday, Weight, FoodID, Activity, BodyType, IsSterilized) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
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

    @PostMapping("/recommend_foods") //推薦飼料
    public List<Map<String, Object>> recommendFoods(@RequestBody PetRequest pet) {
        // 1. 基礎 SQL 語法，把欄位改成你資料庫真正的名稱
        StringBuilder sql = new StringBuilder(
            "SELECT FoodID, Brand, Flavor, UseFor, AgeGroup, BodyType, Calories " +
            "FROM Food WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        // 2. 邏輯 A：物種過濾 (對應 Excel 裡的 PetType 欄位)
        if (pet.Species != null) {
            sql.append(" AND (PetType = ? OR PetType = '全種') ");
            params.add(pet.Species);
        }

        // 3. 邏輯 B：年齡層推薦 (對應 AgeGroup 欄位)
        if (pet.Birthday != null) {
            // 這裡可以根據前端傳來的生日判斷，或者前端直接傳 AgeGroup 過來
            // 範例：如果前端傳來的是 '幼年'
            sql.append(" AND AgeGroup = ? ");
            params.add("成年"); // 這邊之後可以寫動態判斷邏輯
        }

        // 4. 邏輯 C：AI 症狀篩選 (對應你剛確認的 UseFor 欄位)
        // 假設前端透過 AI 幫手介面傳送了關鍵字需求
        if (pet.Activity != null) { // 這裡暫用 Activity 欄位模擬前端傳來的需求字串
            sql.append(" AND (UseFor LIKE ? OR Flavor LIKE ?) ");
            String keyword = "%" + pet.Activity + "%";
            params.add(keyword);
            params.add(keyword);
        }

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }


    @GetMapping("/ai_recommend") //AI幫手
    public List<Map<String, Object>> aiRecommend(
        @RequestParam String petType,   // 寵物種類 (貓/狗)
        @RequestParam String keyword    // 使用者在 AI 幫手點選或輸入的關鍵字
    ) {
        
        String sql = "SELECT FoodID, Brand, Flavor, UseFor, AgeGroup, BodyType, Calories " +
                    "FROM Food " +
                    "WHERE (PetType = ? OR PetType = '全種') " + // 確保物種正確
                    "AND (UseFor LIKE ? OR Flavor LIKE ? OR Brand LIKE ?)"; // 多重關鍵字比對
        
        // 模糊搜尋設定
        String queryKeyword = "%" + keyword + "%";
        
        // 執行查詢並回傳結果清單
        return jdbcTemplate.queryForList(sql, petType, queryKeyword, queryKeyword, queryKeyword);
    }

    // 4. 取得寵物清單 (給前端顯示)
    @GetMapping("/get_pets/{userId}")
    public List<Map<String, Object>> getPets(@PathVariable int userId) {
        String sql = "SELECT * FROM Pets WHERE UserID = ?";
        return jdbcTemplate.queryForList(sql, userId);
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

    //新增健康紀錄 (體溫/體重)
    @PostMapping("/add_health_log")
    public Map<String, Object> addLog(@RequestBody Map<String, Object> log) {
        String sql = "INSERT INTO HealthLogs (PetID, Weight, Temperature) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, log.get("PetID"), log.get("Weight"), log.get("Temperature"));
        return Map.of("status", "success");
    }

    //就醫紀錄
    @PostMapping("/add_medical")
    public Map<String, Object> addMedical(@RequestBody Map<String, Object> data) {
        String sql = "INSERT INTO Medical (PetID, Date, Category, Description) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, data.get("PetID"), data.get("Date"), data.get("Category"), data.get("Description"));
        return Map.of("status", "success");
    }

    //行事曆
    @PostMapping("/add_event")
    public Map<String, Object> addEvent(@RequestBody Map<String, Object> data) {
        // 假設有一個 Events 表
        String sql = "INSERT INTO Events (UserID, EventDate, Title) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, data.get("UserID"), data.get("Date"), data.get("Title"));
        return Map.of("status", "success");
    }

    //圖表
    @GetMapping("/get_chart_data/{petId}")
    public List<Map<String, Object>> getChartData(@PathVariable int petId) {
        // 這裡根據你的 HealthLogs 或攝取紀錄表抓取最近一個月的體重或進食趨勢
        String sql = "SELECT CAST(RecordTime AS DATE) as Date, Weight FROM HealthLogs " +
                    "WHERE PetID = ? AND RecordTime >= DATEADD(day, -30, GETDATE()) " +
                    "ORDER BY RecordTime ASC";
        return jdbcTemplate.queryForList(sql, petId);
    }   
}