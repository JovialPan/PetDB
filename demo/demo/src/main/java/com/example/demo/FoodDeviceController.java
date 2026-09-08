package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FoodDeviceController {

    private final JdbcTemplate jdbcTemplate;

    public FoodDeviceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    // 提供寵物推薦熱量給餵食機
    @GetMapping("/food/device/recommend")
    public Map<String, Object> getCalories(@RequestParam String PetName) {

        String sql = """
            SELECT PetName, RecommendCalories
            FROM Pets
            WHERE PetName = ?
        """;

        return jdbcTemplate.queryForMap(sql, PetName);
    }

    // Arduino UNO R4 WiFi 餵食機上傳資料
    @PostMapping("/food/device/upload")
    public Map<String, Object> uploadFoodDevice(@RequestBody Map<String, Object> data) {

        Map<String, Object> response = new HashMap<>();

        try {
            String sql = """
                INSERT INTO FoodDeviceLog
                (PetName, FoodGram, TotalGram, CreatedAt)
                VALUES (?, ?, ?, GETDATE())
            """;

            jdbcTemplate.update(
                    sql,
                    data.get("PetName"),
                    data.get("FoodGram"),
                    data.get("TotalGram")
            );

            response.put("status", "success");
            response.put("message", "餵食機資料上傳成功");

        } catch (Exception e) {
            e.printStackTrace();

            response.put("status", "fail");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // 查詢飲水機紀錄，之後 Android App 可以呼叫這支 API 顯示資料
    @GetMapping("/food/device/list")
    public List<Map<String, Object>> getFoodDeviceLogs() {

        String sql = """
            SELECT TOP 100
                PetName
                ,FoodGram
                ,TotalGram
                ,CreatedAt
            FROM FoodDeviceLog

        """;

        return jdbcTemplate.queryForList(sql);
    }
}