import pyodbc

def get_connection():
    return pyodbc.connect(
        "DRIVER={ODBC Driver 17 for SQL Server};"
        "SERVER=localhost;"
        "DATABASE=petdb;"
        "UID=sa;"
        "PWD=1234;"
        "TrustServerCertificate=yes;"
    )

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from database import get_connection

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "API running with SQL Server"}

@app.get("/feeds")
def get_feeds():
    conn = None
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id, feed_name, brand, calories FROM feeds")
        rows = cursor.fetchall()

        return [
            {
                "id": row.id,
                "feed_name": row.feed_name,
                "brand": row.brand,
                "calories": row.calories
            }
            for row in rows
        ]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if conn:
            conn.close()

@app.get("/feeds/{feed_id}")
def get_feed(feed_id: int):
    conn = None
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id, feed_name, brand, calories FROM feeds WHERE id = ?",
            (feed_id,)
        )
        row = cursor.fetchone()

        if row is None:
            raise HTTPException(status_code=404, detail="Feed not found")

        return {
            "id": row.id,
            "feed_name": row.feed_name,
            "brand": row.brand,
            "calories": row.calories
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if conn:
            conn.close()
