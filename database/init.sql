CREATE DATABASE PetDB;

USE PetDB; 

CREATE TABLE Users ( 
    UserID INT PRIMARY KEY IDENTITY(1,1), 
    Email NVARCHAR(100) NOT NULL UNIQUE,
    Password NVARCHAR(255) NOT NULL, 
    UserName NVARCHAR(50),  
);

CREATE TABLE Food (
	FoodID INT PRIMARY KEY IDENTITY(1,1),
    Brand NVARCHAR(50),
    Flavor NVARCHAR(50),
    UseFor NVARCHAR(100),  
    AgeGroup NVARCHAR(50),   
    BodyType NVARCHAR(50), 
    PetType NVARCHAR(20),
	AAFCO BIT,
	ThirdPartyTest BIT,
    Calories FLOAT
);

CREATE TABLE Pets (
    PetID INT PRIMARY KEY IDENTITY(1,1),
    UserID INT NOT NULL,                      
    PetName NVARCHAR(50) NOT NULL,          
    Gender NVARCHAR(10),        
    Species NVARCHAR(20),           
    Birthday DATE,                     
    Weight DECIMAL(5,2),     
	FoodID INT,
	Activity NVARCHAR(20),
	BodyType NVARCHAR(50),  
    IsSterilized BIT DEFAULT 0, -- 是否結紮 (0:否, 1:是)
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
	FOREIGN KEY (FoodID) REFERENCES Food(FoodID)
);

CREATE TABLE Costs (
    CostID INT PRIMARY KEY IDENTITY(1,1),
    UserID INT,
    PetID INT,
	Date DATE,
    Amount DECIMAL(10,2),
    Category NVARCHAR(50), 
    FOREIGN KEY (UserID) REFERENCES Users(UserID),
    FOREIGN KEY (PetID) REFERENCES Pets(PetID)
);

CREATE TABLE Medical (
    MedicalID INT PRIMARY KEY IDENTITY(1,1),
    PetID INT,
    Date DATE,
    Category NVARCHAR(50),       
    Description NVARCHAR(255),
    FOREIGN KEY (PetID) REFERENCES Pets(PetID)
);


CREATE TABLE Snacks (
	SnackID INT PRIMARY KEY IDENTITY(1,1),
    Name NVARCHAR(50),
    Calories INT,
    Gram FLOAT
);

CREATE TABLE DailyFood (
    id INT PRIMARY KEY IDENTITY(1,1),
    calories INT, 
    record_date DATE DEFAULT GETDATE()
    FOREIGN KEY (pet_id) REFERENCES Pets(PetID)
);

CREATE TABLE DailyWater (
    id INT PRIMARY KEY IDENTITY(1,1),
    water_ml INT,  
    record_date DATE DEFAULT GETDATE()
    FOREIGN KEY (pet_id) REFERENCES Pets(PetID)
);

CREATE TABLE DailySummary (
    id INT PRIMARY KEY IDENTITY(1,1),
    food_id INT,
    water_id INT,      
    FOREIGN KEY (food_id) REFERENCES DailyFood(id),
    FOREIGN KEY (water_id) REFERENCES DailyWater(id)
);

-- 建立行事曆事件表
CREATE TABLE Events (
    EventID INT PRIMARY KEY IDENTITY(1,1),
    UserID INT,
    EventDate DATE,      -- 存放影片中選擇的日期
    EventTime NVARCHAR(10), -- 存放影片中選擇的時間
    Title NVARCHAR(255), -- 存放影片中輸入的計事內容
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);