// ════════════════════════════════════════════════════════════════
// interview-component.ts  — VERSION AVEC CAMÉRA + SURVEILLANCE
// ════════════════════════════════════════════════════════════════
//
// NOUVEAUTÉS PAR RAPPORT À L'ANCIENNE VERSION :
//   - @ViewChild('videoElement') → accès à la balise <video> du template
//   - startCamera()             → active le flux vidéo de la webcam
//   - startFaceDetection()      → boucle de détection toutes les 500ms
//   - handleLookAway()          → logique d'alerte si regard absent
//   - stopCamera()              → arrête proprement la caméra
//   - surveillanceReport        → objet avec les stats envoyées à Flask
//
// ════════════════════════════════════════════════════════════════

import {
  Component, OnInit, OnDestroy, AfterViewInit,
  ElementRef, ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { InterviewService } from '../../services/interview-service';
import { StateService } from '../../services/state-service';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

// ── ÉTAPE 2 : Import de face-api.js ──────────────────────────────
// Pour installer : npm install face-api.js
// Télécharger les modèles sur : https://github.com/justadudewhohacks/face-api.js/tree/master/weights
// Les mettre dans : src/assets/models/
import * as faceapi from 'face-api.js';

@Component({
  selector: 'app-interview',
  imports: [CommonModule],
  templateUrl: './interview-component.html',
  styleUrls: ['./interview-component.scss']
})
export class InterviewComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── Références aux éléments HTML ────────────────────────────────
  @ViewChild('audioPlayer') audioPlayerRef!: ElementRef<HTMLAudioElement>;

  // ÉTAPE 1 : Référence à la balise <video> du template
  @ViewChild('videoElement') videoRef!: ElementRef<HTMLVideoElement>;

  // ── État général ─────────────────────────────────────────────────
  get state() { return this.stateService; }
  phase$!: Observable<string>;

  // ── Micro / enregistrement ───────────────────────────────────────
  mediaRecorder!: MediaRecorder;
  audioChunks: Blob[] = [];
  stream!: MediaStream;          // flux AUDIO du micro
  private pendingAudioBuffer: ArrayBuffer | null = null;

  readonly MAX_LOOK_AWAY_THRESHOLD = 10; // 20 secondes max avant disqualification
  isDisqualified = false; // Pour bloquer l'interface

  // ── Timer de réponse ─────────────────────────────────────────────
  remainingSeconds = 0;
  totalSeconds = 0;
  timerStarted = false;
  private timerInterval: any = null;

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 1 — Variables pour la caméra
  // ════════════════════════════════════════════════════════════════
  cameraStream!: MediaStream;    // flux VIDEO séparé du micro
  cameraReady = false;           // true quand la caméra est allumée

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 2 — Variables pour la détection du visage
  // ════════════════════════════════════════════════════════════════
  faceDetectionInterval: any = null;   // setInterval de détection
  faceModelsLoaded = false;            // true quand face-api est prêt
  faceDetected = true;                // true si un visage est détecté en ce moment

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 3 — Variables pour les alertes
  // ════════════════════════════════════════════════════════════════
  lookAwaySeconds = 0;           // compteur : combien de secondes sans regard
  showWarning = false;           // affiche le message d'avertissement (> 3s)
  micPaused = false;             // micro mis en pause (> 8s)

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 4 — Rapport de surveillance à envoyer à Flask
  // ════════════════════════════════════════════════════════════════
  surveillanceReport = {
    totalLookAways: 0,           // nombre total de fois qu'il a détourné le regard
    totalLookAwayDuration: 0,    // durée totale (en secondes) hors caméra
    micPausedCount: 0            // nombre de fois où le micro a été mis en pause
  };

  constructor(
    private interviewService: InterviewService,
    private stateService: StateService,
    private router: Router
  ) {
    this.phase$ = this.stateService.phase$;
  }

  // ══════════════════════════════════════════════════════════════
  // ngOnInit — Démarre l'interview comme avant
  // ══════════════════════════════════════════════════════════════
  ngOnInit() {
    this.stateService.setPhase('PROCESSING');
    this.interviewService.startInterview(this.stateService.poste).subscribe({
      next: (audioBuffer: ArrayBuffer) => {
        if (this.audioPlayerRef) {
          this.playAudio(audioBuffer);
        } else {
          this.pendingAudioBuffer = audioBuffer;
        }
      },
      error: (err) => console.error('Erreur démarrage:', err)
    });
  }

  // ══════════════════════════════════════════════════════════════
  // ngAfterViewInit — Lance la caméra dès que le template est prêt
  // ══════════════════════════════════════════════════════════════
  async ngAfterViewInit() {
    // Lecture audio en attente
    if (this.pendingAudioBuffer) {
      this.playAudio(this.pendingAudioBuffer);
      this.pendingAudioBuffer = null;
    }

    // ── ÉTAPE 1 : Démarrer la caméra ──
    await this.startCamera();

    // ── ÉTAPE 2 : Charger les modèles face-api.js ──
    await this.loadFaceModels();
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 1 — startCamera()
  //   Demande l'accès à la webcam et connecte le flux au <video>
  // ════════════════════════════════════════════════════════════════
  private async startCamera() {
    try {
      // On demande SEULEMENT la vidéo ici (le micro est géré séparément)
      this.cameraStream = await navigator.mediaDevices.getUserMedia({ video: true });

      // On connecte le flux à la balise <video> du template
      const video = this.videoRef.nativeElement;
      video.srcObject = this.cameraStream;
      await video.play();

      this.cameraReady = true;
      console.log('✅ Caméra activée');
    } catch (err) {
      console.error('❌ Erreur caméra :', err);
    }
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 1 — stopCamera()
  //   Arrête proprement le flux vidéo
  // ════════════════════════════════════════════════════════════════
  private stopCamera() {
    if (this.cameraStream) {
      this.cameraStream.getTracks().forEach(track => track.stop());
    }
    this.cameraReady = false;
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 2 — loadFaceModels()
  //   Charge les modèles IA de face-api.js depuis /assets/models/
  //   Ces fichiers doivent être téléchargés une seule fois
  // ════════════════════════════════════════════════════════════════
  private async loadFaceModels() {
    const MODEL_URL = '/assets/models';

    // On charge seulement TinyFaceDetector (léger, rapide, suffisant)
    await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL);

    this.faceModelsLoaded = true;
    console.log('✅ Modèles face-api chargés');
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 2 — startFaceDetection()
  //   Lance une boucle qui analyse la vidéo toutes les 500ms
  //   pour savoir si un visage est visible
  // ════════════════════════════════════════════════════════════════
  private startFaceDetection() {
    if (!this.faceModelsLoaded) return;

    this.faceDetectionInterval = setInterval(async () => {
      const video = this.videoRef.nativeElement;

      // Détecte UN visage dans la frame vidéo actuelle
      const detection = await faceapi.detectSingleFace(
        video,
        new faceapi.TinyFaceDetectorOptions()
      );

      // detection === undefined → pas de visage détecté
      this.faceDetected = detection !== undefined;

      // ── ÉTAPE 3 : Gérer les alertes ──
      this.handleLookAway();

    }, 500); // toutes les 500ms
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 2 — stopFaceDetection()
  //   Arrête la boucle de détection
  // ════════════════════════════════════════════════════════════════
  private stopFaceDetection() {
    if (this.faceDetectionInterval) {
      clearInterval(this.faceDetectionInterval);
      this.faceDetectionInterval = null;
    }
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 3 — handleLookAway()
  //   Appelée toutes les 500ms par la boucle de détection
  //   Logique d'alerte progressive selon le temps hors caméra
  // ════════════════════════════════════════════════════════════════
 private handleLookAway() {
  // On ne surveille QUE si la phase est 'RECORDING'
  // Si l'IA parle ou si on analyse, on ne disqualifie pas.
  if (this.stateService.getPhase() !== 'RECORDING') {
    this.lookAwaySeconds = 0; // Reset par sécurité
    this.showWarning = false;
    return;
  }
  if (!this.faceDetected) {
    // Le candidat ne regarde pas la caméra
    // On incrémente le compteur consécutif (+0.5s car la vérification a lieu toutes les 500ms)
    this.lookAwaySeconds += 0.5;

    // On continue de mettre à jour la durée totale pour le rapport de surveillance
    this.surveillanceReport.totalLookAwayDuration += 0.5;

    console.log(`Compteur consécutif hors caméra : ${this.lookAwaySeconds}s`);

    // --- LOGIQUE D'ALERTE PROGRESSIVE ---

    // Avertissement visuel à l'écran (ex: après 3 secondes consécutives)
    if (this.lookAwaySeconds >= 3 && !this.showWarning) {
      this.showWarning = true;
      this.surveillanceReport.totalLookAways++;
      console.warn("⚠️ Avertissement : Le candidat ne regarde pas l'écran.");
    }

    // Mise en pause du microphone (ex: après 6 secondes consécutives)
    // Nous baissons un peu ce seuil car le seuil critique est plus bas maintenant.
    if (this.lookAwaySeconds >= 6 && !this.micPaused) {
      this.micPaused = true;
      this.pauseMicrophone();
      console.warn("🔇 Microphone mis en pause — regard absent prolongé.");
    }

    // --- SEUIL CRITIQUE : DISQUALIFICATION IMMÉDIATE ---

    // Si le temps consécutif dépasse le seuil critique (ex: 10 secondes)
    if (this.lookAwaySeconds >= this.MAX_LOOK_AWAY_THRESHOLD && !this.isDisqualified) {
      // Nous appelons la méthode qui arrête tout et affiche l'overlay
      this.abortInterviewDueToCheating();
    }

  } else {
    // Le candidat regarde à nouveau la caméra
    // Nous REMETTONS À ZÉRO le compteur consécutif !!! C'est la clé du changement.
    if (this.lookAwaySeconds > 0) {
      this.lookAwaySeconds = 0;
      this.showWarning = false;

      // Si le micro était en pause, nous le réactivons
      if (this.micPaused) {
        this.micPaused = false;
        this.resumeMicrophone();
        console.log("🎤 Microphone réactivé — candidat regarde à nouveau.");
      }
    }
  }
}

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 3 — Pause / reprise du micro
  //   On met en pause les tracks audio du stream
  // ════════════════════════════════════════════════════════════════
  private pauseMicrophone() {
    if (this.stream) {
      this.stream.getAudioTracks().forEach(track => track.enabled = false);
    }
  }

  private resumeMicrophone() {
    if (this.stream) {
      this.stream.getAudioTracks().forEach(track => track.enabled = true);
    }
  }

  // ════════════════════════════════════════════════════════════════
  // Lecture audio (inchangé par rapport à l'ancienne version)
  // ════════════════════════════════════════════════════════════════
  private playAudio(buffer: ArrayBuffer) {
    this.stateService.setPhase('LISTENING');
    this.stateService.questionNumber++;

    const blob = new Blob([buffer], { type: 'audio/mpeg' });
    const url = URL.createObjectURL(blob);
    const audio = this.audioPlayerRef.nativeElement;
    audio.src = url;
    audio.play();

    audio.onended = () => {
      URL.revokeObjectURL(url);
      this.prepareRecording();
    };
  }

  // ══════════════════════════════════════════════════════════════
  // prepareRecording — Prépare le micro ET démarre la détection
  // ══════════════════════════════════════════════════════════════
  private async prepareRecording() {
    this.stateService.setPhase('RECORDING');
    this.timerStarted = false;
    this.totalSeconds = this.getDurationForQuestion(this.stateService.questionNumber);
    this.remainingSeconds = this.totalSeconds;
    this.audioChunks = [];

    // Micro uniquement (audio: true, PAS video — la caméra est déjà active)
    this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    this.mediaRecorder = new MediaRecorder(this.stream);
    this.mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) this.audioChunks.push(e.data);
    };
    this.mediaRecorder.onstop = () => this.sendAnswer();
    this.mediaRecorder.start();

    // ── ÉTAPE 2 : Démarrer la surveillance du regard ──
    this.startFaceDetection();
  }

  // ══════════════════════════════════════════════════════════════
  // beginAnswer — Le candidat clique "Commencer à répondre"
  // ══════════════════════════════════════════════════════════════
  beginAnswer() {
    this.timerStarted = true;
    this.startTimer();
  }

  // ══════════════════════════════════════════════════════════════
  // stopRecording — Arrête le micro ET la détection
  // ══════════════════════════════════════════════════════════════
  stopRecording() {
    this.clearTimer();

    // ── ÉTAPE 2 : Arrêter la détection du visage ──
    this.stopFaceDetection();

    // Réinitialiser les alertes
    this.showWarning = false;
    this.micPaused = false;
    this.lookAwaySeconds = 0;

    if (this.mediaRecorder?.state === 'recording') {
      this.mediaRecorder.stop();
      this.stream.getTracks().forEach(t => t.stop());
    }
  }

  // ══════════════════════════════════════════════════════════════
  // sendAnswer — Envoie la réponse audio (inchangé)
  // ══════════════════════════════════════════════════════════════
  private sendAnswer() {
    this.stateService.setPhase('PROCESSING');
    const audioBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
    this.interviewService.sendAnswer(audioBlob).subscribe({
      next: (audioBuffer: ArrayBuffer) => this.playAudio(audioBuffer),
      error: (err) => console.error('Erreur réponse:', err)
    });
  }

  // ════════════════════════════════════════════════════════════════
  // ÉTAPE 4 — finishInterview()
  //   Envoie le rapport de surveillance à Flask EN PLUS de l'évaluation
  // ════════════════════════════════════════════════════════════════
  finishInterview() {
    this.stopRecording();
    this.stopCamera();           // ← NOUVEAU : on éteint la caméra
    this.stateService.setPhase('PROCESSING');

    // ── ÉTAPE 4 : Envoyer rapport + demander évaluation ──
    this.interviewService.evaluate().subscribe({
      next: (evaluation) => {
        this.stateService.evaluation = evaluation;
        this.stateService.setPhase('DONE');
        this.router.navigate(['/result']);
      }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // Timer (inchangé)
  // ══════════════════════════════════════════════════════════════
  private getDurationForQuestion(questionNumber: number): number {
    if (questionNumber === 1) return 30;
    if (questionNumber >= 2 && questionNumber <= 4) return 120;
    if (questionNumber >= 5 && questionNumber <= 7) return 60;
    if (questionNumber === 8 || questionNumber === 9) return 120;
    if (questionNumber === 10) return 30;
    return 60;
  }

  getDurationLabel(questionNumber: number): string {
    const sec = this.getDurationForQuestion(questionNumber);
    if (sec === 30) return '30 secondes';
    if (sec === 60) return '1 minute';
    if (sec === 120) return '2 minutes';
    return `${sec} sec`;
  }

  private startTimer() {
    this.clearTimer();
    this.timerInterval = setInterval(() => {
      if (this.remainingSeconds > 0) {
        this.remainingSeconds--;
      } else {
        this.clearTimer();
        this.stopRecording();
      }
    }, 1000);
  }

  private clearTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  get timerPercent(): number {
    if (this.totalSeconds === 0) return 100;
    return (this.remainingSeconds / this.totalSeconds) * 100;
  }

  get timerColor(): string {
    if (!this.timerStarted) return '#b2bec3';
    if (this.timerPercent > 50) return '#2ecc71';
    if (this.timerPercent > 25) return '#f39c12';
    return '#e74c3c';
  }

  get timerDisplay(): string {
    const m = Math.floor(this.remainingSeconds / 60);
    const s = this.remainingSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }


  // AJOUTE cette nouvelle méthode ici :
  private abortInterviewDueToCheating() {
    this.isDisqualified = true;
    this.stopRecording();
    this.stopFaceDetection();
    this.stopCamera();

    console.error("🛑 Interview annulée : Tentative de triche détectée.");

    this.interviewService.sendCheatingReport(this.surveillanceReport).subscribe({
      next: () => {
        this.stateService.setPhase('DONE');
        this.router.navigate(['/result']);
      },
      error: (err) => {
        console.error("Erreur lors de l'envoi du rapport de triche", err);
        this.router.navigate(['/']); // Retour accueil en cas d'erreur
      }
    });
  }

  // À ajouter dans la classe InterviewComponent
  goToHome() {
    // On arrête tout proprement avant de partir
    this.stopCamera();
    this.stopFaceDetection();
    this.clearTimer();

    // Redirection vers la page d'accueil (ou la route de ton choix)
    this.router.navigate(['/']);
  }

  // ══════════════════════════════════════════════════════════════
  // ngOnDestroy — Nettoyage complet
  // ══════════════════════════════════════════════════════════════
  ngOnDestroy() {
    this.clearTimer();
    this.stopFaceDetection();                              // ← NOUVEAU
    this.stopCamera();                                     // ← NOUVEAU
    this.stream?.getTracks().forEach(t => t.stop());
  }
}
