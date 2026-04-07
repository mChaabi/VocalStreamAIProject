import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Phase } from '../models/phase';
import { EvaluationResponse } from '../models/evaluation-response';

@Injectable({ providedIn: 'root' })
export class StateService {

  currentQuestionText = '';
  private phaseSubject = new BehaviorSubject<Phase>('IDLE');
  phase$ = this.phaseSubject.asObservable();
  surveillanceReport: any;

  poste = '';
  questionNumber = 0;
  evaluation: EvaluationResponse | null = null;

  setPhase(phase: Phase) {
    this.phaseSubject.next(phase);
  }

  getPhase(): Phase {
    return this.phaseSubject.value;
  }

  get phase(): Phase {
    return this.phaseSubject.value;
  }

  reset() {
    this.poste = '';
    this.questionNumber = 0;
    this.evaluation = null;
    this.setPhase('IDLE');
  }
}
