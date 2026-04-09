from flask import Flask, jsonify
from flask import request
import pyodbc

app = Flask(__name__)

conn = pyodbc.connect(
    "DRIVER={ODBC Driver 17 for SQL Server};"
    "SERVER=140.125.193.36;"
    "DATABASE=PetDB;"
    "UID=UserAdmin2;"
    "PWD=groupH115"
)


#註冊 
@app.route('/register', methods=['POST'])
def register():
    data = request.get_json(silent=True) or request.form
        
    username = data.get("username")
    password = data.get("password")
    email = data.get("email")

    if not username or not password or not email:
        return jsonify({
            "status": "fail",
            "message": f"缺少欄位 username={username}, password={password}, email={email}"
        })

    cursor = conn.cursor()

    cursor.execute(
        "SELECT * FROM Users WHERE email=? OR username=?",
        (email, username)
    )
    if cursor.fetchone():
        return jsonify({"status": "fail", "message": "帳號或Email已存在"})

    cursor.execute(
        "INSERT INTO Users (email, username, password) VALUES (?, ?, ?)",
        (email, username, password)
    )
    conn.commit()

    return jsonify({"status": "success"})

# 登入 
@app.route('/login', methods=['POST'])
def login():
    data = request.get_json(silent=True) or request.form

    email = data.get("email")
    password = data.get("password")

    if not email or not password:
        return jsonify({
            "status": "fail",
            "message": "帳號或密碼不能為空"
        })

    cursor = conn.cursor()

    cursor.execute(
        "SELECT UserID FROM Users WHERE Email=? AND Password=?",
        (email, password)
    )

    user = cursor.fetchone()
    
    if user:
        return jsonify({
            "status": "success",
            "user_id": user[0]
        })
    else:
        return jsonify({"status": "fail"})


#新增寵物
@app.route('/add_pet', methods=['POST'])
def add_pet():
    data = request.get_json(silent=True) or request.form

    user_id = data.get("UserID")
    pet_name = data.get("PetName")
    gender = data.get("Gender")
    species = data.get("Species")
    birthday = data.get("Birthday")  # YYYY-MM-DD
    weight = data.get("Weight")
    food_id = data.get("FoodID")
    activity = data.get("Activity")
    body_type = data.get("BodyType")
    is_sterilized = data.get("IsSterilized")

    if not user_id or not pet_name:
        return jsonify({"status": "fail", "message": "user_id 或 pet_name 缺失"})

    cursor = conn.cursor()

    cursor.execute("""
        INSERT INTO Pets 
        (UserID, PetName, Gender, Species, Birthday, Weight, FoodID, Activity, BodyType, IsSterilized)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        user_id,
        pet_name,
        gender,
        species,
        birthday,
        weight,
        food_id,
        activity,
        body_type,
        is_sterilized
    ))

    conn.commit()

    return jsonify({"status": "success", "message": "新增寵物成功"})

#選單取得飼料資料
# @app.route('/get_foods', methods=['GET'])
# def get_foods():
    
#     cursor = conn.cursor()
#     cursor.execute("SELECT Flavor FROM Food")

#     foods = []
#     for row in cursor.fetchall():
#         foods.append({
#             "Flavor": row[0]
#         })

#     return jsonify({"status": "success", "foods": foods})

# 取得寵物列表
# @app.route('/get_pets', methods=['POST'])
# def get_pets():
#     data = request.json
#     user_id = data.get("user_id")

#     cursor = conn.cursor()
#     cursor.execute(
#         "SELECT id, pet_name, type, age FROM Pets WHERE user_id=?",
#         (user_id,)
#     )

#     pets = []
#     for row in cursor.fetchall():
#         pets.append({
#             "id": row[0],
#             "pet_name": row[1],
#             "type": row[2],
#             "age": row[3]
#         })

#     return jsonify({"status": "success", "pets": pets})




if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)