from flask import Flask, jsonify
from flask import request
import pyodbc

app = Flask(__name__)

conn = pyodbc.connect(
    "DRIVER={ODBC Driver 17 for SQL Server};"
    "SERVER=140.125.193.36,1433;"
    "DATABASE=PetDB;"
    "UID=UserAdmin2;"
    "PWD=groupH115"
)

#查詢所有飼料
@app.route('/foods', methods=['GET'])
def get_foods():
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM Food")

    rows = cursor.fetchall()

    result = []
    for row in rows:
        result.append({
            "FoodID": row[0],
            "Brabd": row[1],
            "Flavor": row[2],
            "UseFor": row[3]
        })

    return jsonify(result)

# 登入
@app.route('/register', methods=['POST'])
def register():
    data = request.json

    username = data['username']
    password = data['password']

    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO Users (Username, Password) VALUES (?, ?)",
        (username, password)
    )
    conn.commit()

    return jsonify({"status": "success"})



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)