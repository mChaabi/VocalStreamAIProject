# 🎙️ AI Interview System
Plateforme intelligente de simulation d’entretiens utilisant l’IA, la reconnaissance vocale (STT) et la synthèse vocale (TTS), permettant une interaction en temps réel avec évaluation automatique des réponses.
---

<h2 align="center">📸 Aperçu du Système</h2>

<div align="center">
  <img width="680" src="https://github.com/user-attachments/assets/d3187fe5-40f4-4626-bc75-8aa9f00e6333" alt="DebutEntretien" />
  <p><em>Figure 1.1 : Interface de bienvenue</em></p>
  
  <br>
  
  <img width="595" src="https://github.com/user-attachments/assets/1b76a830-f47a-4772-96b6-ac2128ce064d" alt="ChoixPoste" />
  <p><em>Figure 1.2 : Sélection du poste pour l'entretien (ex: Product Manager)</em></p>
</div>

<hr />

<div align="center">
  <img width="674" src="https://github.com/user-attachments/assets/eeac747a-4218-4833-985c-ce09f5326887" alt="PeriodeQuestion" />
  <p><em>Figure 2 : Génération de la question par l'IA</em></p>
</div>

<hr />

<div align="center">
  <img width="554" src="https://github.com/user-attachments/assets/e13b0eec-10f3-4612-ac26-19bb997744e3" alt="PeriodeRecording" />
  <p><em>Figure 3 : Capture vocale en temps réel</em></p>
</div>

<hr />

<div align="center">
  <img width="596" src="https://github.com/user-attachments/assets/4283d071-bb44-4d9d-9082-f56add5016c4" alt="PeriodeReponse" />
  <p><em>Figure 4 : Transcription textuelle de la réponse</em></p>
</div>

<hr />

<div align="center">
  <img width="658" src="https://github.com/user-attachments/assets/3cdbb1f1-44f4-4295-bfe3-0417890ea46b" alt="ResultatEntretien" />
  <p><em>Figure 5 : Analyse finale et évaluation automatique</em></p>
</div>


## ⚙️ Technologies utilisées

- **Frontend** : Angular (TypeScript, HTML, CSS, RxJS)
- **Backend** : Spring Boot (Java 21, REST API)
- **Intelligence Artificielle** : Spring AI + Groq (modèle `llama-3.3-70b-versatile`)
- **Voice Processing (Python)** :
  - Flask
  - gTTS (Text-To-Speech)
  - SpeechRecognition (Speech-To-Text)

---

## 🚀 Fonctionnement

- L’IA génère des questions d’entretien
- Les questions sont lues via TTS
- L’utilisateur répond oralement
- Les réponses sont converties en texte via STT
- Le backend analyse les réponses avec un modèle LLM
- Génération d’un score + feedback (points forts / améliorations)

---

## 🔌 APIs

- **Spring Boot**
  - `POST /start` → démarrer entretien
  - `POST /answer` → envoyer réponse
  - `POST /evaluate` → évaluation finale

- **Python Flask**
  - `GET /tts` → texte → audio
  - `POST /stt` → audio → texte

---

## ▶️ Lancement

```bash
# Voice Service
pip install flask gtts SpeechRecognition
python app.py

# Backend
mvn spring-boot:run

# Frontend
npm install
ng serve

🧠 Modèle IA
Intégré via Spring AI
Fournisseur : Groq
Modèle : LLaMA 3.3 70B
Utilisation :
Génération de questions
Analyse des réponses
Évaluation intelligente


📌 Résultat
Score sur 100
Feedback détaillé
Points forts
Axes d’amélioration

👨‍💻 Auteur
Mohamed Chaabi

