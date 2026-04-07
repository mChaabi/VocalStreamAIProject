// ════════════════════════════════════════════════════════════════
// result-component.ts  — VERSION AVEC RAPPORT DE SURVEILLANCE
// ════════════════════════════════════════════════════════════════
//
// NOUVEAUTÉ :
//   - On affiche le rapport de surveillance (étape 4) en plus
//     du score et du feedback de l'interview
//
// ════════════════════════════════════════════════════════════════

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { StateService } from '../../services/state-service';
import { EvaluationResponse } from '../../models/evaluation-response';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-result',
  imports: [CommonModule],
  templateUrl: './result-component.html',
  styleUrls: ['./result-component.scss']
})
export class ResultComponent implements OnInit {

  evaluation: EvaluationResponse | null = null;

  // ── ÉTAPE 4 : Récupérer le rapport depuis StateService ──────────
  // StateService doit stocker surveillanceReport (voir note ci-dessous)
  surveillanceReport: any = null;

  constructor(public state: StateService, private router: Router) {}

  ngOnInit() {
    this.evaluation = this.state.evaluation;

    // ÉTAPE 4 : On récupère le rapport de surveillance
    // (à stocker dans StateService depuis finishInterview())
    this.surveillanceReport = this.state.surveillanceReport || null;
  }

  restart() {
    this.state.reset();
    this.router.navigate(['/start']);
  }

  // ── ÉTAPE 4 : Calcul du score de présence ───────────────────────
  //   Formule simple :
  //   score = 100 - (totalLookAwayDuration * 2) - (micPausedCount * 10)
  //   Minimum 0, Maximum 100
  get presenceScore(): number {
    if (!this.surveillanceReport) return 100;
    const penalty =
      (this.surveillanceReport.totalLookAwayDuration * 2) +
      (this.surveillanceReport.micPausedCount * 10);
    return Math.max(0, Math.round(100 - penalty));
  }
}
