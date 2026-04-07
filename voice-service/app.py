from flask import Flask, request, jsonify, send_file, render_template_string
import speech_recognition as sr
from gtts import gTTS
import os

app = Flask(__name__)

UPLOAD_FOLDER = "temp_audio"
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# ════════════════════════════════════════════════════════════════
# ÉTAPE 4 — Stockage du rapport de surveillance en mémoire
#   Dans une vraie app : utiliser PostgreSQL avec un modèle
#   Ici : simple dictionnaire Python pour comprendre le concept
# ════════════════════════════════════════════════════════════════
surveillance_data = {}

HTML_CODE = '''
<!DOCTYPE html>
<html>
<head>
    <title>Voice Service Complet</title>
    <style>
        body { font-family: sans-serif; text-align: center; padding: 50px; }
        .box { border: 1px solid #ccc; padding: 20px; margin: 20px auto; width: 300px; border-radius: 10px; }
        button { cursor: pointer; padding: 10px; margin: 5px; }
    </style>
</head>
<body>
    <h1>Système Vocal Flask</h1>
    <div class="box">
        <h3>1. Parler (TTS)</h3>
        <input type="text" id="textToSpeak" placeholder="Tapez un message...">
        <button onclick="playAudio()">Écouter</button>
    </div>
    <div class="box">
        <h3>2. Écouter (STT)</h3>
        <button id="startBtn">🎤 Commencer</button>
        <button id="stopBtn" disabled>⏹️ Arrêter</button>
        <p id="sttResult">Résultat : ...</p>
    </div>
    <script>
        function playAudio() {
            let msg = document.getElementById('textToSpeak').value;
            if(msg) window.location.href = "/tts?text=" + encodeURIComponent(msg);
        }
        let mediaRecorder;
        let audioChunks = [];
        document.getElementById('startBtn').onclick = async () => {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRecorder = new MediaRecorder(stream);
            mediaRecorder.start();
            audioChunks = [];
            document.getElementById('startBtn').disabled = true;
            document.getElementById('stopBtn').disabled = false;
            mediaRecorder.ondataavailable = e => audioChunks.push(e.data);
            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
                const formData = new FormData();
                formData.append('audio', audioBlob, 'test.wav');
                document.getElementById('sttResult').innerText = "Analyse en cours...";
                const response = await fetch('/stt', { method: 'POST', body: formData });
                const data = await response.json();
                document.getElementById('sttResult').innerText = "Résultat : " + (data.text || data.error);
            };
        };
        document.getElementById('stopBtn').onclick = () => {
            mediaRecorder.stop();
            document.getElementById('startBtn').disabled = false;
            document.getElementById('stopBtn').disabled = true;
        };
    </script>
</body>
</html>
'''

@app.route('/')
def home():
    return render_template_string(HTML_CODE)


# ── Route TTS (inchangée) ────────────────────────────────────────
@app.route('/tts', methods=['GET', 'POST'])
def text_to_speech():
    text = request.args.get('text')
    if not text and request.is_json:
        text = request.json.get('text')
    if not text:
        text = "Bonjour"
    file_path = os.path.join(UPLOAD_FOLDER, "output.mp3")
    tts = gTTS(text=text, lang='fr')
    tts.save(file_path)
    return send_file(file_path, mimetype="audio/mpeg")


# ── Route STT (inchangée) ────────────────────────────────────────
@app.route('/stt', methods=['POST'])
def speech_to_text():
    if 'audio' not in request.files:
        return jsonify({"error": "Pas d'audio"}), 400
    file = request.files['audio']
    recognizer = sr.Recognizer()
    try:
        with sr.AudioFile(file) as source:
            audio_data = recognizer.record(source)
            text = recognizer.recognize_google(audio_data, language="fr-FR")
            return jsonify({"text": text})
    except Exception as e:
        return jsonify({"error": "Erreur de lecture ou micro silencieux."})


# ════════════════════════════════════════════════════════════════
# ÉTAPE 4 — Route /api/surveillance  (NOUVELLE)
#
#   Angular envoie ce JSON à la fin de l'interview :
#   {
#     "candidat_id": "user_123",        ← optionnel, pour identifier
#     "totalLookAways": 3,
#     "totalLookAwayDuration": 12.5,
#     "micPausedCount": 1
#   }
#
#   Flask stocke les données et retourne le score de présence
# ════════════════════════════════════════════════════════════════
@app.route('/api/surveillance', methods=['POST'])
def save_surveillance():
    # Récupère le JSON envoyé par Angular
    data = request.get_json()

    if not data:
        return jsonify({"error": "Données manquantes"}), 400

    # Extraction des valeurs
    candidat_id              = data.get('candidat_id', 'anonyme')
    total_look_aways         = data.get('totalLookAways', 0)
    total_look_away_duration = data.get('totalLookAwayDuration', 0)
    mic_paused_count         = data.get('micPausedCount', 0)

    # ── Calcul du score de présence ──────────────────────────────
    # Même formule que dans result-component.ts pour cohérence
    penalty = (total_look_away_duration * 2) + (mic_paused_count * 10)
    presence_score = max(0, round(100 - penalty))

    # ── Stockage en mémoire (remplacer par PostgreSQL en prod) ───
    surveillance_data[candidat_id] = {
        "totalLookAways": total_look_aways,
        "totalLookAwayDuration": total_look_away_duration,
        "micPausedCount": mic_paused_count,
        "presenceScore": presence_score
    }

    print(f"📷 Rapport reçu pour {candidat_id} : score présence = {presence_score}/100")

    # ── Retourne la confirmation à Angular ───────────────────────
    return jsonify({
        "message": "Rapport enregistré",
        "candidat_id": candidat_id,
        "presenceScore": presence_score
    })


# ════════════════════════════════════════════════════════════════
# ÉTAPE 4 — Route /api/surveillance/<candidat_id>  (BONUS)
#   Permet de récupérer le rapport d'un candidat spécifique
# ════════════════════════════════════════════════════════════════
@app.route('/api/surveillance/<candidat_id>', methods=['GET'])
def get_surveillance(candidat_id):
    report = surveillance_data.get(candidat_id)
    if not report:
        return jsonify({"error": "Rapport non trouvé"}), 404
    return jsonify(report)


if __name__ == '__main__':
    app.run(debug=True)