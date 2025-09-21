import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoggingService, LogEntry } from '../services/logging.service';
import { Subscription } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';

@Component({
  selector: 'app-logging',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [CookieService],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>System Logs</h1>
        <p>Real-time system log monitoring</p>
      </header>

      <div class="controls">
        <div class="button-group">
          <button (click)="toggleAutoRefresh()" [class.active]="autoRefresh" class="btn-secondary">
            {{ autoRefresh ? '⏸️ Pause' : '▶️ Resume' }}
          </button>
          <button (click)="refreshLogs()" class="btn-primary">
            🔄 Refresh
          </button>
          <button (click)="clearLogs()" class="btn-danger">
            🗑️ Clear Logs
          </button>
        </div>
        
        <div class="filter-group">
          <label>Level Filter:</label>
          <select [(ngModel)]="levelFilter" (change)="applyFilter()" class="form-control">
            <option value="">All Levels</option>
            <option value="ERROR">ERROR</option>
            <option value="WARN">WARN</option>
            <option value="INFO">INFO</option>
            <option value="DEBUG">DEBUG</option>
            <option value="TRACE">TRACE</option>
          </select>
          
          <label class="form-checkbox">
            <input type="checkbox" [(ngModel)]="followLogs" (change)="onFollowLogsChange()">
            Follow logs
          </label>
        </div>
      </div>

      <div class="log-viewer">
        <div class="log-count">
          Showing {{ filteredLogs.length }} of {{ logs.length }} logs
        </div>
        
        <div class="log-table-wrapper" #logTableWrapper>
          <table class="data-table">
            <thead>
              <tr>
                <th class="timestamp-col">Timestamp</th>
                <th class="level-col">Level</th>
                <th class="logger-col">Logger</th>
                <th class="message-col">Message</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let log of filteredLogs" [class]="'log-' + log.level.toLowerCase()">
                <td class="timestamp-col">{{ log.timestamp }}</td>
                <td class="level-col">
                  <span class="level-badge" [class]="'level-' + log.level.toLowerCase()">
                    {{ log.level }}
                  </span>
                </td>
                <td class="logger-col" [title]="log.logger">{{ truncateLogger(log.logger) }}</td>
                <td class="message-col">{{ log.message }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .btn-secondary.active {
      background-color: #28a745;
    }

    .btn-secondary.active:hover {
      background-color: #218838;
    }

    .filter-group {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .filter-group label {
      font-weight: 500;
      color: #495057;
    }

    .form-control {
      width: auto;
      min-width: 150px;
    }

    .log-viewer {
      flex: 1;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .log-count {
      padding: 1rem;
      background-color: #f8f9fa;
      border-bottom: 1px solid #dee2e6;
      font-size: 0.875rem;
      color: #6c757d;
    }

    .log-table-wrapper {
      flex: 1;
      overflow: auto;
    }

    .timestamp-col {
      width: 220px;
      min-width: 220px;
      font-family: monospace;
      font-size: 0.8rem;
      white-space: nowrap;
    }

    .level-col {
      width: 80px;
      text-align: center;
    }

    .logger-col {
      width: 180px;
      min-width: 180px;
      font-family: monospace;
      font-size: 0.8rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .message-col {
      min-width: 300px;
    }

    .level-badge {
      display: inline-block;
      padding: 0.25rem 0.5rem;
      border-radius: 3px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      min-width: 50px;
      text-align: center;
    }

    .level-error {
      background-color: #f8d7da;
      color: #721c24;
    }

    .level-warn {
      background-color: #fff3cd;
      color: #856404;
    }

    .level-info {
      background-color: #d1ecf1;
      color: #0c5460;
    }

    .level-debug {
      background-color: #d4edda;
      color: #155724;
    }

    .level-trace {
      background-color: #e2e3e5;
      color: #383d41;
    }

    .log-error {
      background-color: #fff5f5;
    }

    .log-warn {
      background-color: #fffaf0;
    }
  `]
})
export class LoggingComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('logTableWrapper') private logTableWrapper!: ElementRef;
  
  logs: LogEntry[] = [];
  filteredLogs: LogEntry[] = [];
  autoRefresh = true;
  levelFilter = '';
  followLogs = true;
  private logsSubscription?: Subscription;
  private shouldScrollToBottom = false;
  private cookieService = inject(CookieService);

  constructor(private loggingService: LoggingService) {}

  ngOnInit() {
    this.loadFollowLogsFromCookie();
    this.loadLevelFilterFromCookie();
    this.startAutoRefresh();
  }

  ngOnDestroy() {
    this.stopAutoRefresh();
  }
  
  ngAfterViewChecked() {
    if (this.shouldScrollToBottom && this.followLogs) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  startAutoRefresh() {
    if (this.autoRefresh) {
      this.logsSubscription = this.loggingService.getLogsWithAutoRefresh().subscribe(
        logs => {
          const previousLength = this.logs.length;
          this.logs = logs;
          this.applyFilter();
          if (logs.length > previousLength) {
            this.shouldScrollToBottom = true;
          }
        }
      );
    }
  }

  stopAutoRefresh() {
    if (this.logsSubscription) {
      this.logsSubscription.unsubscribe();
    }
  }

  toggleAutoRefresh() {
    this.autoRefresh = !this.autoRefresh;
    if (this.autoRefresh) {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  refreshLogs() {
    this.loggingService.getLogs().subscribe(
      logs => {
        this.logs = logs;
        this.applyFilter();
      }
    );
  }

  clearLogs() {
    if (confirm('Are you sure you want to clear all logs?')) {
      this.loggingService.clearLogs().subscribe(() => {
        this.logs = [];
        this.filteredLogs = [];
      });
    }
  }


  applyFilter() {
    if (this.levelFilter) {
      this.filteredLogs = this.logs.filter(log => log.level === this.levelFilter);
    } else {
      this.filteredLogs = [...this.logs];
    }
    this.saveLevelFilterToCookie();
  }

  truncateLogger(logger: string): string {
    const parts = logger.split('.');
    if (parts.length > 3) {
      return '...' + parts.slice(-3).join('.');
    }
    return logger;
  }
  
  onFollowLogsChange() {
    this.saveFollowLogsToCookie();
    if (this.followLogs) {
      this.scrollToBottom();
    }
  }
  
  private scrollToBottom() {
    if (this.logTableWrapper) {
      const element = this.logTableWrapper.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }
  
  private loadFollowLogsFromCookie() {
    const cookieValue = this.cookieService.get('followLogs');
    this.followLogs = cookieValue !== 'false'; // Default to true if not set
  }
  
  private saveFollowLogsToCookie() {
    this.cookieService.set('followLogs', this.followLogs.toString(), 365);
  }
  
  private loadLevelFilterFromCookie() {
    const cookieValue = this.cookieService.get('logLevelFilter');
    this.levelFilter = cookieValue || ''; // Default to empty (All Levels)
  }
  
  private saveLevelFilterToCookie() {
    this.cookieService.set('logLevelFilter', this.levelFilter, 365);
  }
}