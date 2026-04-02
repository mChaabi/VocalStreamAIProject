import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { StartComponent } from './components/start-component/start-component';
import { InterviewComponent } from './components/interview-component/interview-component';
import { ResultComponent } from './components/result-component/result-component';

 export const routes: Routes = [
  { path: '', redirectTo: 'start', pathMatch: 'full' },
  { path: 'start', component: StartComponent },
  { path: 'interview', component: InterviewComponent },
  { path: 'result', component: ResultComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
