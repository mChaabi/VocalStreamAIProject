import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { StateService } from '../../services/state-service';
import { EvaluationResponse } from '../../models/evaluation-response';

@Component({
  selector: 'app-result',
  templateUrl: './result-component.html',
  styleUrls: ['./result-component.scss']
})
export class ResultComponent {

  evaluation: EvaluationResponse | null = null;

  constructor(public state: StateService, private router: Router) {}

  ngOnInit() {
    this.evaluation = this.state.evaluation;
  }

  restart() {
    this.state.reset();
    this.router.navigate(['/start']);
  }
}
