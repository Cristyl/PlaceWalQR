import sqlite3
import os

# Percorso assoluto del DB
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "placewalqr.db")

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row  # così le righe possono essere trattate come dizionari
    return conn
