from flask import Flask, jsonify
from db_utils import get_db_connection

app = Flask(__name__)

if __name__ == '__main__':
    app.run()
