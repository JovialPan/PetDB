USE PetDB; 

CREATE TABLE Users ( 
    UserID INT PRIMARY KEY IDENTITY(1,1), 
    Email NVARCHAR(100) NOT NULL UNIQUE,
    Password NVARCHAR(255) NOT NULL, 
    UserName NVARCHAR(50),  
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
	Activity NVARCHAR(20)
	BodyType NVARCHAR(50),  
    IsSterilized BIT DEFAULT 0,                -- 是否結紮 (0:否, 1:是)
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

CREATE TABLE Food (
	FoodID INT PRIMARY KEY IDENTITY(1,1),
    Brand NVARCHAR(50),
    Flavor NVARCHAR(50),
    Function NVARCHAR(100),  
    AgeGroup NVARCHAR(50),   
    BodyType NVARCHAR(50),
    PetType NVARCHAR(50),  
	AACFO BIT,
	ThirdPartyTest BIT,
    Calories FLOAT
);

CREATE TABLE Snacks (
	SnackID INT PRIMARY KEY IDENTITY(1,1),
    Name NVARCHAR(50),
    Calories INT,
    Gram FLOAT,
);

CREATE TABLE HealthLogs (
	HealthLogID INT PRIMARY KEY IDENTITY(1,1),
    PetID INT NOT NULL, 
    Weight DECIMAL(5,2),
    Temperature DECIMAL(5,2),   
    RecordTime DATETIME2 DEFAULT SYSDATETIME(),
    FOREIGN KEY (PetID) REFERENCES Pets(PetID)
);
