# 🎙️ AI Interview System

Plateforme intelligente de simulation d’entretiens utilisant l’IA, la reconnaissance vocale (STT) et la synthèse vocale (TTS), permettant une interaction en temps réel avec évaluation automatique des réponses.
---

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

