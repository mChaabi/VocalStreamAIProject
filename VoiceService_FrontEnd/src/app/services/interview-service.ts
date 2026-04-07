import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EvaluationResponse } from '../models/evaluation-response';

@Injectable({ providedIn: 'root' })
export class InterviewService {

  private API = 'http://localhost:8080/api/interview';

  constructor(private http: HttpClient) {}
sendCheatingReport(surveillanceReport: any): Observable<any> {
  return this.http.post(`${this.API}/cheating-report`, surveillanceReport);
}
  // POST /api/interview/start → reçoit bytes MP3
  startInterview(poste: string): Observable<ArrayBuffer> {
    return this.http.post(
      `${this.API}/start`,
      { poste },
      { responseType: 'arraybuffer' }
    );
  }

  // POST /api/interview/answer → envoie audio WAV, reçoit bytes MP3
  sendAnswer(audioBlob: Blob): Observable<ArrayBuffer> {
    const formData = new FormData();
    formData.append('file', audioBlob, 'answer.wav');
    return this.http.post(
      `${this.API}/answer`,
      formData,
      { responseType: 'arraybuffer' }
    );
  }

  // GET /api/interview/evaluate → reçoit EvaluationResponse JSON
  evaluate(): Observable<EvaluationResponse> {
    return this.http.get<EvaluationResponse>(`${this.API}/evaluate`);
  }
}
