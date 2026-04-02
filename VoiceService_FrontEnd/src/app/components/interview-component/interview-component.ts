import { Component, OnInit, OnDestroy, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { InterviewService } from '../../services/interview-service';
import { StateService } from '../../services/state-service';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-interview',
  imports: [CommonModule],
  templateUrl: './interview-component.html',
  styleUrls: ['./interview-component.scss']
})
export class InterviewComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('audioPlayer') audioPlayerRef!: ElementRef<HTMLAudioElement>;

  get state() { return this.stateService; }
  phase$!: Observable<string>;

  mediaRecorder!: MediaRecorder;
  audioChunks: Blob[] = [];
  stream!: MediaStream;

  private pendingAudioBuffer: ArrayBuffer | null = null;

  // ── Timer ──
  remainingSeconds = 0;
  totalSeconds = 0;
  timerStarted = false;        // ✅ false = timer en attente, true = timer en cours
  private timerInterval: any = null;

  constructor(
    private interviewService: InterviewService,
    private stateService: StateService,
    private router: Router
  ) {
    this.phase$ = this.stateService.phase$;
  }

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

  ngAfterViewInit() {
    if (this.pendingAudioBuffer) {
      this.playAudio(this.pendingAudioBuffer);
      this.pendingAudioBuffer = null;
    }
  }

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
      this.prepareRecording();  // ✅ prépare le micro mais n'active PAS encore le timer
    };
  }

  // ✅ Prépare le micro — affiche le bouton "Commencer à répondre" — timer en attente
  private async prepareRecording() {
    this.stateService.setPhase('RECORDING');
    this.timerStarted = false;   // timer visible mais pas démarré
    this.totalSeconds = this.getDurationForQuestion(this.stateService.questionNumber);
    this.remainingSeconds = this.totalSeconds;
    this.audioChunks = [];

    this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    this.mediaRecorder = new MediaRecorder(this.stream);
    this.mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) this.audioChunks.push(e.data);
    };
    this.mediaRecorder.onstop = () => this.sendAnswer();
    this.mediaRecorder.start();
  }

  // ✅ Le candidat clique "Commencer à répondre" → le timer démarre
  beginAnswer() {
    this.timerStarted = true;
    this.startTimer();
  }

  // ── Timer ────────────────────────────────────

  private getDurationForQuestion(questionNumber: number): number {
    if (questionNumber === 1)                          return 30;
    if (questionNumber >= 2 && questionNumber <= 4)   return 120;
    if (questionNumber >= 5 && questionNumber <= 7)   return 60;
    if (questionNumber === 8 || questionNumber === 9) return 120;
    if (questionNumber === 10)                         return 30;
    return 60;
  }

  getDurationLabel(questionNumber: number): string {
    const sec = this.getDurationForQuestion(questionNumber);
    if (sec === 30)  return '30 secondes';
    if (sec === 60)  return '1 minute';
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
        this.stopRecording(); // auto-stop quand temps écoulé
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
    if (!this.timerStarted)       return '#b2bec3'; // gris = en attente
    if (this.timerPercent > 50)   return '#2ecc71'; // vert
    if (this.timerPercent > 25)   return '#f39c12'; // orange
    return '#e74c3c';                                // rouge
  }

  get timerDisplay(): string {
    const m = Math.floor(this.remainingSeconds / 60);
    const s = this.remainingSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  // ─────────────────────────────────────────────

  stopRecording() {
    this.clearTimer();
    if (this.mediaRecorder?.state === 'recording') {
      this.mediaRecorder.stop();
      this.stream.getTracks().forEach(t => t.stop());
    }
  }

  private sendAnswer() {
    this.stateService.setPhase('PROCESSING');
    const audioBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
    this.interviewService.sendAnswer(audioBlob).subscribe({
      next: (audioBuffer: ArrayBuffer) => this.playAudio(audioBuffer),
      error: (err) => console.error('Erreur réponse:', err)
    });
  }

  finishInterview() {
    this.stopRecording();
    this.stateService.setPhase('PROCESSING');
    this.interviewService.evaluate().subscribe({
      next: (evaluation) => {
        this.stateService.evaluation = evaluation;
        this.stateService.setPhase('DONE');
        this.router.navigate(['/result']);
      }
    });
  }

  ngOnDestroy() {
    this.clearTimer();
    this.stream?.getTracks().forEach(t => t.stop());
  }
}
