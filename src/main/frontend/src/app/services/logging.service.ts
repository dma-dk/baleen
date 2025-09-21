import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';

export interface LogEntry {
  timestamp: string;
  level: string;
  logger: string;
  message: string;
  thread: string;
}

@Injectable({
  providedIn: 'root'
})
export class LoggingService {
  private apiUrl = '/api/logs';

  constructor(private http: HttpClient) {}

  getLogs(limit: number = 1000): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}?limit=${limit}`);
  }

  // Get logs with auto-refresh every 2 seconds
  getLogsWithAutoRefresh(limit: number = 1000): Observable<LogEntry[]> {
    return interval(2000).pipe(
      startWith(0),
      switchMap(() => this.getLogs(limit))
    );
  }

  clearLogs(): Observable<any> {
    return this.http.delete(this.apiUrl);
  }

  generateTestLogs(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test`);
  }
}