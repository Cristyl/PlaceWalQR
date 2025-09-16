# security libraries
from werkzeug.security import check_password_hash, generate_password_hash
import bleach

import sqlite3
import os


# Percorso assoluto del DB
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "placewalqr.db")

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)

    conn.execute('PRAGMA synchronous=NORMAL')
    conn.execute('PRAGMA cache_size=10000')
    conn.execute('PRAGMA temp_store=MEMORY')
    conn.execute('PRAGMA mmap_size=268435456')

    conn.row_factory = sqlite3.Row  # così le righe possono essere trattate come dizionari
    return conn

# sanitize_input
def sanitize_input(input_data):
    return bleach.clean(input_data)

# check if there exists a user with such email and password - internal use only
def check_user(email, password):
    try:
        conn = get_db_connection()
        cursor = conn.execute('SELECT * FROM USERS WHERE "email" = ?', (email,))
        user = cursor.fetchone()

        # introduced check on password hashing
        if(user and check_password_hash(user["password"], password)):
            conn.close()
            return True

        conn.close()
        return False
    except Exception as e:
        print(str(e))
        return False

# retrieve the default image of the place - internal use only
def retrieve_default_place_image(place_id):
    if(place_id != None and place_id != 0):
        conn = get_db_connection()
        cursor = conn.execute('SELECT * FROM places WHERE "id" = ?', (place_id, ))
        elem = cursor.fetchone()
        conn.close()

        return elem["image"]
