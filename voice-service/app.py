from flask import Flask, request, jsonify, send_file, render_template_string
import speech_recognition as sr
from gtts import gTTS
import os

app = Flask(__name__)

# Dossier temporaire pour les fichiers audio
UPLOAD_FOLDER = "temp_audio"
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# --- 1. L'INTERFACE UNIQUE (HTML + JS) ---
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
        // Logique TTS : Redirige vers la route /tts qui génère l'audio
        function playAudio() {
            let msg = document.getElementById('textToSpeak').value;
            if(msg) window.location.href = "/tts?text=" + encodeURIComponent(msg);
        }

        // Logique STT : Capture le micro et envoie au serveur
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

# --- 2. ROUTE TEXT-TO-SPEECH ---
# --- 2. ROUTE TEXT-TO-SPEECH ---
@app.route('/tts', methods=['GET', 'POST'])
def text_to_speech():
    # 1. On cherche d'abord dans les paramètres d'URL (?text=...)
    text = request.args.get('text')
    
    # 2. Si pas trouvé, on cherche dans le JSON (pour Spring Boot)
    if not text and request.is_json:
        text = request.json.get('text')
    
    # 3. Valeur par défaut
    if not text:
        text = "Bonjour"

    file_path = os.path.join(UPLOAD_FOLDER, "output.mp3")
    tts = gTTS(text=text, lang='fr')
    tts.save(file_path)
    return send_file(file_path, mimetype="audio/mpeg")

# --- 3. ROUTE SPEECH-TO-TEXT ---
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

if __name__ == '__main__':
    app.run(debug=True)