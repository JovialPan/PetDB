CREATE DATABASE PetDB;

USE PetDB;
GO

CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Email NVARCHAR(100) NOT NULL,
    Password NVARCHAR(255) NOT NULL,
    UserName NVARCHAR(50)
);

CREATE TABLE Food (
    FoodID INT IDENTITY(1,1) PRIMARY KEY,
    Brand NVARCHAR(50),
    Flavor NVARCHAR(100),
    UseFor NVARCHAR(255),
    AgeGroup NVARCHAR(50),
    BodyType NVARCHAR(50),
    PetType NVARCHAR(20),
    AAFCO BIT,
    ThirdPartyTest BIT,
    Calories FLOAT
);

CREATE TABLE Pets (
    PetID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    PetName NVARCHAR(50) NOT NULL,
    Gender NVARCHAR(10),
    Species NVARCHAR(20),
    Birthday DATE,
    Weight DECIMAL(5,2),
    FoodID INT,
    Activity NVARCHAR(20),
    BodyType NVARCHAR(50),
    IsSterilized BIT,
    RecommendCalories float,
    RecommendWater float

    FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    FOREIGN KEY (FoodID)
        REFERENCES Food(FoodID)
);

CREATE TABLE Hospitals (
    HospitalID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(100) NOT NULL,
    Address NVARCHAR(200) NOT NULL,
    Phone NVARCHAR(20) NOT NULL
);

CREATE TABLE Medical (
    MedicalID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT,
    Date DATE,
    Category NVARCHAR(50),
    Description NVARCHAR(255),

    FOREIGN KEY (PetID)
        REFERENCES Pets(PetID)
);

CREATE TABLE DailyFood (
    DailyFoodID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT,
    Calories INT,
    RecordDate DATE,

    FOREIGN KEY (PetID)
        REFERENCES Pets(PetID)
);

CREATE TABLE DailyWater (
    DailyWaterID INT IDENTITY(1,1) PRIMARY KEY,
    PetID INT,
    WaterML INT,
    RecordDate DATE,

    FOREIGN KEY (PetID)
        REFERENCES Pets(PetID)
);

CREATE TABLE Events (
    EventID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT,
    PetID INT,
    EventDate DATE,
    EventTime NVARCHAR(15),
    Title NVARCHAR(255),

    FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    FOREIGN KEY (PetID)
        REFERENCES Pets(PetID)
);

CREATE TABLE Snacks (
    SnackID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(50),
    Calories INT,
    Gram FLOAT
);