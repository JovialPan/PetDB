package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WaterDeviceController {

    private final JdbcTemplate jdbcTemplate;

    public WaterDeviceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Arduino UNO R4 WiFi 飲水機上傳資料
    @PostMapping("/water/device/upload")
    public Map<String, Object> uploadWaterDevice(@RequestBody Map<String, Object> data) {

        Map<String, Object> response = new HashMap<>();

        try {
            String sql = """
                INSERT INTO WaterDeviceLog
                (PetName, WaterPercent, DrinkML, TotalML, TempC, CreatedAt)
                VALUES (?, ?, ?, ?, ?, GETDATE())
            """;

            jdbcTemplate.update(
                    sql,
                    data.get("PetName"),
                    data.get("WaterPercent"),
                    data.get("DrinkML"),
                    data.get("TotalML"),
                    data.get("TempC")
            );

            response.put("status", "success");
            response.put("message", "硬體資料上傳成功");

        } catch (Exception e) {
            e.printStackTrace();

            response.put("status", "fail");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // 查詢飲水機紀錄，之後 Android App 可以呼叫這支 API 顯示資料
    @GetMapping("/water/device/list")
    public List<Map<String, Object>> getWaterDeviceLogs() {

        String sql = """
            SELECT TOP 100
                LogID,
                PetName,
                WaterPercent,
                DrinkML,
                TotalML,
                TempC,
                CreatedAt
            FROM WaterDeviceLog
            ORDER BY CreatedAt DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }
}
