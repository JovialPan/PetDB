package com.example.demo;

public class PetRequest {
    public Integer UserID;
    public String PetName;     // 姓名
    public String Gender;      // 性別
    public String Species;     // 物種
    public String Birthday;    // 生日
    public Double Weight;      // 體重
    public Integer FoodID;     // 選用飼料 (對應資料庫 ID)
    public String Activity;    // 活動量
    public String BodyType;    // 體態
    public Boolean IsSterilized; // 是否結紮 (0 或 1)
    public Double Temperature; // 體溫
    public String ImageUrl;    // 圖片路徑或 Base64
}