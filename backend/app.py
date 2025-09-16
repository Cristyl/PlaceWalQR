from flask import Flask, jsonify, request
from db_utils import get_db_connection, sanitize_input, check_user, retrieve_default_place_image, check_password_hash, generate_password_hash
from datetime import datetime
import base64

app = Flask(__name__)

@app.route("/api/getPlacesByUser", methods=['GET'])
def getPlacesByUser():
    user_id = request.args.get("user_id")

    if user_id is None:
        return jsonify({"error": "Missing user_id parameter"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            SELECT places.name, COALESCE(has_visited.image, places.image) AS image
            FROM has_visited
            JOIN places ON has_visited.place_id = places.id
            WHERE has_visited.user_id = ?
            ORDER BY has_visited.date DESC
        """, (user_id,))
        places = cursor.fetchall()
    except Exception as e:
        return jsonify({"error": "Database error: " + str(e)}), 500
    finally:
        conn.close()

    result = []
    for row in places:
        image_blob = row["image"]
        image_base64 = None
        if image_blob:
            image_base64 = base64.b64encode(image_blob).decode("utf-8")
        result.append({
            "name": row["name"],
            "imageBase64": image_base64
        })

    # Non è un errore se la lista è vuota, restituisce semplicemente un array vuoto
    return jsonify(result), 200

@app.route('/api/collections/<user_id>', methods=['GET'])
def get_user_collections(user_id):
    """
    Ottiene tutte le collezioni con il progress dell'utente
    """
    if user_id is None:
        return jsonify({"error": "Missing user_id parameter"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Query semplice per tutte le collezioni
        cursor.execute("""
            SELECT id, name, image, is_unknown, points
            FROM collection
            ORDER BY is_unknown ASC, id ASC
        """)
        collections_data = cursor.fetchall()

    except Exception as e:
        return jsonify({"error": "Database error: " + str(e)}), 500
    finally:
        conn.close()

    result = []

    for row in collections_data:
        col_id = row["id"]
        name = row["name"]
        image_blob = row["image"]
        is_unknown = row["is_unknown"]
        points = row["points"]

        # Calcola total_places
        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM collection_places WHERE id_collection = ?", (col_id,))
            total_places = cursor.fetchone()[0]
            conn.close()
        except:
            total_places = 0

        # Calcola visited_places
        visited_places = 0
        if total_places > 0:
            try:
                conn = get_db_connection()
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT COUNT(DISTINCT cp.id_place)
                    FROM collection_places cp
                    JOIN has_visited hv ON cp.id_place = hv.place_id
                    WHERE cp.id_collection = ? AND hv.user_id = ?
                """, (col_id, user_id))
                visited_places = cursor.fetchone()[0] or 0
                conn.close()
            except:
                visited_places = 0

        # Converti immagine
        image_base64 = None
        if image_blob:
            try:
                image_base64 = base64.b64encode(image_blob).decode("utf-8")
            except:
                image_base64 = None

        # Logica collezioni segrete
        display_name = name
        display_image = image_base64

        if is_unknown and visited_places < total_places:
            display_name = "???"
            display_image = None

        result.append({
            "id": col_id,
            "name": display_name,
            "image": display_image,
            "isUnknown": bool(is_unknown),
            "points": points,
            "totalPlaces": total_places,
            "visitedPlaces": visited_places
        })

    return jsonify(result), 200


@app.route('/api/collection/<int:collection_id>/places/<user_id>', methods=['GET'])
def get_collection_places(collection_id, user_id):
    """
    Ottiene i luoghi di una collezione specifica con stato visitato
    """
    if user_id is None:
        return jsonify({"error": "Missing user_id parameter"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Info collezione
        cursor.execute("""
            SELECT id, name, image, is_unknown, points
            FROM collection
            WHERE id = ?
        """, (collection_id,))
        collection_data = cursor.fetchone()

        if not collection_data:
            return jsonify({"error": "Collection not found"}), 404

        # Luoghi della collezione
        cursor.execute("""
            SELECT p.id, p.name, p.image
            FROM places p
            JOIN collection_places cp ON p.id = cp.id_place
            WHERE cp.id_collection = ?
            ORDER BY p.name ASC
        """, (collection_id,))
        places_data = cursor.fetchall()

    except Exception as e:
        return jsonify({"error": "Database error: " + str(e)}), 500
    finally:
        conn.close()

    # Costruisci collezione
    col_image_base64 = None
    if collection_data["image"]:
        try:
            col_image_base64 = base64.b64encode(collection_data["image"]).decode("utf-8")
        except:
            col_image_base64 = None

    # Calcola totali
    total_places = len(places_data)
    visited_places = 0

    # Costruisci luoghi con stato visitato
    places_result = []

    for row in places_data:
        place_id = row["id"]
        place_name = row["name"]
        place_image_blob = row["image"]

        # Controlla se visitato
        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute("""
                SELECT COUNT(*) FROM has_visited
                WHERE place_id = ? AND user_id = ?
            """, (place_id, user_id))
            is_visited = cursor.fetchone()[0] > 0
            conn.close()

            if is_visited:
                visited_places += 1
        except:
            is_visited = False

        # Converti immagine luogo
        place_image_base64 = None
        if place_image_blob:
            try:
                place_image_base64 = base64.b64encode(place_image_blob).decode("utf-8")
            except:
                place_image_base64 = None

        # Logica luoghi segreti
        is_collection_secret = bool(collection_data["is_unknown"])
        is_collection_completed = visited_places >= total_places
        is_secret_place = is_collection_secret and not is_visited and not is_collection_completed

        places_result.append({
            "id": place_id,
            "name": place_name,
            "image": place_image_base64,
            "isVisited": is_visited,
            "isSecret": is_secret_place
        })

    # Response finale
    collection_result = {
        "id": collection_data["id"],
        "name": collection_data["name"],
        "image": col_image_base64,
        "isUnknown": bool(collection_data["is_unknown"]),
        "points": collection_data["points"],
        "totalPlaces": total_places,
        "visitedPlaces": visited_places
    }

    response = {
        "collection": collection_result,
        "places": places_result
    }

    return jsonify(response), 200


@app.route("/api/getLeaderboard", methods=['GET'])
def get_leaderboard():
    user_nickname = request.args.get("user_nickname")
    if user_nickname is None:
        return jsonify({"error": "Missing user_nickname parameter"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Prima ottieni tutti gli utenti con i loro punti dai luoghi
        cursor.execute("""
            SELECT
                u.id,
                u.nickname,
                COALESCE(SUM(p.points), 0) AS place_points
            FROM users u
            LEFT JOIN has_visited hv ON u.id = hv.user_id
            LEFT JOIN places p ON hv.place_id = p.id
            GROUP BY u.id, u.nickname
            ORDER BY u.nickname ASC
        """)

        users_data = cursor.fetchall()

        # Calcola punti totali per ogni utente (luoghi + collezioni)
        leaderboard_data = []

        for user in users_data:
            user_id = user["id"]
            nickname = user["nickname"]
            place_points = user["place_points"] or 0

            # Calcola punti dalle collezioni completate per questo utente
            cursor.execute("""
                SELECT
                    c.id,
                    c.points,
                    COUNT(DISTINCT cp.id_place) as total_places,
                    COUNT(DISTINCT CASE WHEN hv.user_id = ? THEN cp.id_place END) as visited_places
                FROM collection c
                LEFT JOIN collection_places cp ON c.id = cp.id_collection
                LEFT JOIN has_visited hv ON cp.id_place = hv.place_id
                GROUP BY c.id, c.points
            """, (user_id,))

            collections = cursor.fetchall()
            collection_points = 0

            for collection in collections:
                total_places = collection["total_places"] or 0
                visited_places = collection["visited_places"] or 0
                points = collection["points"] or 0

                # Se la collezione è completata, aggiungi i punti
                if total_places > 0 and visited_places >= total_places:
                    collection_points += points

            # Punti totali = luoghi + collezioni completate
            total_points = place_points + collection_points

            leaderboard_data.append({
                "nickname": nickname,
                "total_points": total_points
            })

        # Ordina per punti totali decrescenti
        leaderboard_data.sort(key=lambda x: x["total_points"], reverse=True)

    except Exception as e:
        return jsonify({"error": "Database error: " + str(e)}), 500
    finally:
        conn.close()

    # Costruisci risultato
    result = []
    user_position = None

    for position, user_data in enumerate(leaderboard_data, 1):
        nickname = user_data["nickname"]
        total_points = user_data["total_points"]

        entry = {
            "position": position,
            "nickname": nickname,
            "total_points": total_points
        }

        # Salva posizione utente
        if nickname == user_nickname:
            user_position = position

        # Aggiungi alla top 10
        if position <= 10:
            result.append(entry)

    # Aggiungi utente se fuori top 10
    if user_position and user_position > 10:
        user_data = next(user for user in leaderboard_data if user["nickname"] == user_nickname)
        user_entry = {
            "position": user_position,
            "nickname": user_nickname,
            "total_points": user_data["total_points"]
        }
        result.append(user_entry)

    return jsonify(result), 200

# simple login
@app.route("/api/login", methods=['POST'])
def login():
    data = request.get_json()

    if not data or not data.get('email') or not data.get('password'):
        return jsonify({"status": "ko", "error": "Malformed credentials"}), 400

    email = sanitize_input(data['email'])
    password = data['password']

    try:
        conn = get_db_connection()
        cursor = conn.execute(
            'SELECT * FROM USERS WHERE email = ?',
            (email, )
        )
        user_info = cursor.fetchone()
        conn.close()

        if(user_info and check_password_hash(user_info["password"], password)):
            return jsonify({
                "id": user_info["id"],
                "name": user_info["name"],
                "surname": user_info["surname"],
                "dob": user_info["date_of_birth"],
                "email": user_info["email"],
                "nickname": user_info["nickname"]
            }), 200
        else:
            return jsonify({"status": "ko", "error": "Invalid credentials"}), 401

    except Exception as e:
        return jsonify({"status": "ko", "error": str(e)}), 500


# google signin
@app.route("/api/googleAuth", methods=['POST'])
def google_auth():
    data = request.get_json()
    if(not data):
        return jsonify({"status": "ko", "error": "Malformed request"}), 400

    print(data)
    google_id = sanitize_input(data.get("google_id"))
    email = sanitize_input(data.get("email"))
    name = sanitize_input(data.get("name"))
    surname = sanitize_input(data.get("surname", ""))
    dob = ""

    password = generate_password_hash("password")

    if(not google_id or not email):
        return jsonify({"status": "ko", "error": "Missing data"}), 400

    try:
        conn = get_db_connection()
        resp_code = 200

        # checking if the user already exists inside the db
        existing_user = conn.execute('SELECT * FROM users WHERE "email" = ? AND "google_auth" = ?', (email, True)).fetchone()

        if(not existing_user):
            conn.execute('INSERT INTO users (email, password, name, surname, date_of_birth, nickname, google_auth) VALUES (?, ?, ?, ?, ?, ?, ?)',
                (email, password, name, surname, dob, name + surname, True))

            conn.commit()
            existing_user = conn.execute('SELECT * FROM users WHERE "email" = ? AND "google_auth" = ?', (email, True)).fetchone()

            resp_code = 201

        conn.close()
        return jsonify({
                    "id": existing_user["id"],
                    "name": existing_user["name"],
                    "surname": existing_user["surname"],
                    "dob": dob,
                    "email": existing_user["email"],
                    "nickname": existing_user["nickname"]
                }), resp_code

    except Exception as e:
        return jsonify({"status": "ko", "erorr": f"An error occurred  - {str(e)}"}), 500


# register a new user existing
@app.route("/api/register", methods=['POST'])
def register():
    data = request.get_json()

    if not data:
        return jsonify({"status": "ko", "error": "Malformed request"}), 400

    email = sanitize_input(data["email"])
    password = data["password"]
    name = sanitize_input(data["name"])
    surname = sanitize_input(data["surname"])
    nickname = sanitize_input(data["nickname"])
    dob = data["dob"]

    if(check_user(email, password)):
        return jsonify({"status": "ko"}), 401

    try:
        password_hash = generate_password_hash(password)

        conn = get_db_connection()

        # since it is not used the google authenitcation, password and other details are needed and google_auth flag is set to False
        conn.execute('INSERT INTO USERS (email, password, name, surname, date_of_birth, nickname, google_auth) VALUES (?, ?, ?, ?, ?, ?, ?)',
                     (email, password_hash, name, surname, dob, nickname, False))
        conn.commit()
        conn.close()

        return jsonify({"status": "ok"}), 201

    except Exception as e:
        return jsonify({"status": f"An error occurred - {str(e)}"}), 500


# insert the tuple (user_id, place_id) if the user does not have already visited the place
@app.route("/api/visitPlaceById", methods=['POST'])
def visitPlaceById():
    data = request.get_json(silent=True)

    if not data:
        return jsonify({"status": "ko - no data"}), 400

    user_email = data.get("user_id")
    place_id =  data.get("place_id")
    date = data.get("date_of_visit")
    lat = data.get("user_lat")
    long = data.get("user_long")

    # scostamento di max +-50m
    POSITION_THRESHOLD = 0.45089

    if not user_email or not place_id or not date:
        return jsonify({"status": "ko", "error": "Malformed request", "user_id": user_email, "place_id": place_id, "date": date}), 400

    try:
        conn = get_db_connection()

        # checking existence of user
        cursor = conn.execute(f'SELECT * FROM USERS WHERE "id" = ?', (user_email,))
        if not cursor.fetchall():
            conn.close()
            return jsonify({"status": "ko - no rows"}), 401

        # checking exitence of place and retrieving it
        cursor = conn.execute(f'SELECT * FROM PLACES WHERE "id" = ?', (place_id,))
        place = cursor.fetchone()
        if not place:
            conn.close()
            return jsonify({"status": "ko - No matching place"}), 404

        # checking correctness of position for user
        if((abs(place["longitude"] - long) >= POSITION_THRESHOLD) or (abs(place["latitude"] - lat) >= POSITION_THRESHOLD)):
            return jsonify({"status": "ko - Wrong position"}), 406

        place_name = place["name"]
        place_information = place["information"]

        # checking if user already saw that place
        cursor = conn.execute(f'SELECT * FROM HAS_VISITED WHERE "user_id" = ? AND "place_id" = ?', (user_email, place_id))
        elem = cursor.fetchone()
        souvenir = False

        if elem:
            conn.close()

            encoded_image = None
            souvenir = False
            if (not elem["image"]):
                blob_image = retrieve_default_place_image(place_id)

                if(blob_image):
                    souvenir = True
                    encoded_image = base64.b64encode(blob_image).decode("utf-8")

            else:
                encoded_image = base64.b64encode(elem["image"]).decode("utf-8")

            return jsonify({
                "status": "existing",
                "seen": True,
                "souvenir": souvenir,
                "name": place_name,
                "information": place_information,
                "image":  encoded_image
                }), 200

        encoded_image = retrieve_default_place_image(place_id)
        if(encoded_image):
            encoded_image = base64.b64encode(encoded_image).decode("utf-8")

        # inserting <user_id, place_id> tuple
        conn.execute("BEGIN TRANSACTION")
        try:
            conn.execute('INSERT INTO has_visited (user_id, place_id) VALUES (?, ?)', (user_email, place_id))
            conn.commit()
            conn.close()

        except sqlite3.IntegrityError:
            conn.rollback()
            existing_place = conn.execute('SELECT * FROM HAS_VISITED WHERE user_id=? AND place_id=?',
                                  (user_email, place_id)).fetchone()

        return jsonify({
            "status": "ok",
            "souvenir": souvenir,
            "name": place_name,
            "information": place_information,
            "image": encoded_image,
            "seen": False
            }), 201

    except Exception as e:
        return jsonify({"status": f"An error occurred - {str(e)}"}), 500


# save a souvenir photo taken by the user
@app.route("/api/saveSouvenir", methods=['POST'])
def saveSouvenir():
    data = request.get_json()

    if(not data):
        return jsonify({"status": "ko"}), 400

    user_id = data["user_id"]
    place_id = data["place_id"]
    image_string = data["image_string"]

    try:
        # checking existence of user
        conn = get_db_connection()

        result = conn.execute('SELECT * FROM USERS WHERE id = ?', (user_id,)).fetchone()
        if(not result):
            return jsonify({"status": "ko"}), 404

        # checking existence of place
        result = conn.execute('SELECT * FROM PLACES WHERE id = ?', (place_id,)).fetchone()
        if(not result):
            return jsonify({"status": "place not found"}), 404

        # checking existence of visit
        result = conn.execute('SELECT * FROM has_visited WHERE place_id = ? AND user_id = ?', (place_id, user_id)).fetchone()
        if(not result):
            return jsonify({"status": "visit not found"}), 404

        # inserting image inside has_visited table
        image_blob = base64.b64decode(image_string)
        result = conn.execute(
            'UPDATE has_visited SET image = ?, date = ? WHERE place_id = ? AND user_id = ?',
                (image_blob, datetime.now().strftime("%Y-%m-%d %H:%M:%S"), place_id, user_id)
            )
        conn.commit()
        conn.close()

        return jsonify({"status": "image saved"}), 201

    except Exception as e:
        return {"status": f"An error occurred - {str(e)}"}, 500


@app.route("/api/findUserByEmail", methods=["GET"])
def findUserByEmail():
    email=request.get_json().get("email")
    conn_user=get_db_connection()
    cursor=conn_user.execute(f'SELECT * FROM USERS WHERE ("email" == "{email}")')
    rows=cursor.fetchall()

    if(rows.__len__()==0):
        conn_user.close()
        return jsonify({"status": "user not found"}), 404

    result=rows.pop()
    conn_user.close()

    name=result["name"]
    surname=result["surname"]
    nickname=result["nickname"]
    id=result["id"]
    password=result["password"]
    dob=result["date_of_birth"]

    return jsonify({"stratus": "ok", "id": id, "name": name, "surname": surname, "nickname": nickname, "email": email, "password": password, "dob": dob}), 201

@app.route("/api/getPointsById/<int:user_id>", methods=["GET"])
def getPointsById(user_id):
    conn_visited=get_db_connection()
    cursor=conn_visited.execute(f'SELECT place_id FROM HAS_VISITED WHERE ("user_id" == {user_id})')
    rows=cursor.fetchall()

    if(rows.__len__()==0):
        conn_visited.close()
        return jsonify({"status":"places don't found"}), 404

    points=0
    while rows:
        place_id=rows.pop()["place_id"]
        cursor=conn_visited.execute(f'SELECT points FROM PLACES WHERE ("id" == {place_id})')
        point=int(cursor.fetchone()["points"])
        points+=point

    cursor=conn_visited.execute(f'SELECT count(user_id) FROM HAS_VISITED WHERE ("user_id"=={user_id})')
    count=int(cursor.fetchone()[0])

    cursor.execute("""
                SELECT
                    c.id,
                    c.points,
                    COUNT(DISTINCT cp.id_place) as total_places,
                    COUNT(DISTINCT CASE WHEN hv.user_id = ? THEN cp.id_place END) as visited_places
                FROM collection c
                LEFT JOIN collection_places cp ON c.id = cp.id_collection
                LEFT JOIN has_visited hv ON cp.id_place = hv.place_id
                GROUP BY c.id, c.points
            """, (user_id,))

    collections = cursor.fetchall()
    collection_points = 0

    for collection in collections:
        total_places = collection["total_places"] or 0
        visited_places = collection["visited_places"] or 0
        points_c = collection["points"] or 0

        # Se la collezione è completata, aggiungi i punti
        if total_places > 0 and visited_places >= total_places:
            collection_points += points_c
    points+=collection_points

    conn_visited.close()
    return jsonify({"status": "ok", "points": points, "count":count}), 200

@app.route("/api/findAllPlaces", methods=["GET"])
def findAllPlaces():
    conn_place=get_db_connection()
    cursor=conn_place.execute("SELECT * FROM PLACES")
    rows=cursor.fetchall()
    result=[]
    while rows:
        place=rows.pop()
        result.append({"id":place["id"], "name": place["name"], "latitude": float(place["latitude"]), "longitude":float(place["longitude"])})

    conn_place.close()
    return jsonify(result), 200

@app.route("/api/findVisitedPlaceById/<int:user_id>", methods=["GET"])
def findVisitedPlaceById(user_id):
    try:
        connection = get_db_connection()
        cursor = connection.execute("SELECT * FROM PLACES")
        rows = cursor.fetchall()
        result = []

        for place in rows:
            place_id = place["id"]

            cursor_visited = connection.execute(
                'SELECT * FROM HAS_VISITED WHERE user_id = ? AND place_id = ?',
                (user_id, place_id)
            )
            visited = cursor_visited.fetchone()

            image_data = None
            if visited is not None and visited["image"] is not None:
                try:
                    image_data = base64.b64encode(visited["image"]).decode('utf-8')
                except Exception as e:
                    print(f"Error converting BLOB to base64 for {visited['name']}: {e}")
                    image_data = None
            else:
                try:
                    image_data = base64.b64encode(place["image"]).decode('utf-8')
                except Exception as e:
                    print(f"Error converting BLOB to base64 for {place['name']}: {e}")
                    image_data = None

            result.append({
                "id": place["id"],
                "name": place["name"],
                "information": place["information"],
                "latitude": float(place["latitude"]),
                "longitude": float(place["longitude"]),
                "image": image_data,
                "visited": visited is not None
            })

        connection.close()
        return jsonify(result), 200

    except Exception as e:
        print(f"Error in findVisitedPlaceById: {e}")
        import traceback
        traceback.print_exc()  # Stampa lo stack trace completo
        return {"error": str(e)}, 500

@app.route("/api/findLastPlaceById/<int:user_id>", methods=["GET"])
def findLastPlaceById(user_id):
    conn_place=get_db_connection()
    cursor=conn_place.execute(f'SELECT * FROM HAS_VISITED WHERE ("user_id"={user_id}) ORDER BY date DESC')

    results = cursor.fetchall()
    if len(results) == 0:
        return jsonify({"status": "Not found"}), 404

    last_place=results[0]
    date=last_place["date"]
    place_id=last_place["place_id"]
    image_data = None

    cursor=conn_place.execute(f'SELECT * FROM PLACES WHERE ("id"={place_id})')
    place=cursor.fetchone()
    name=place["name"]
    information=place["information"]
    point=place["points"]

    if last_place["image"] is not None:
        try:
            image_data = base64.b64encode(last_place["image"]).decode('utf-8')
        except Exception as e:
            print(f"Error converting BLOB to base64 for {last_place['name']}: {e}")
            image_data = None
    else:
        try:
            image_data = base64.b64encode(place["image"]).decode('utf-8')
        except Exception as e:
            print(f"Error converting BLOB to base64 for {place['name']}: {e}")
            image_data = None

    conn_place.close()
    return jsonify({"status": "ok", "id": place_id, "name": name, "information": information, "point":point, "date": date, "image": image_data}), 200
