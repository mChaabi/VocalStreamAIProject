import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { StateService } from '../../services/state-service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-start',
  imports:[CommonModule,FormsModule],
  templateUrl: './start-component.html',
  styleUrls: ['./start-component.scss']

})
export class StartComponent {

  poste = '';

  constructor(
    private state: StateService,
    private router: Router
  ) {}

  startInterview() {
    if (!this.poste.trim()) return;
    this.state.poste = this.poste;
    this.router.navigate(['/interview']);
  }
}
