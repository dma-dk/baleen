import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoggingService, LogEntry } from '../services/logging.service';
import { Subscription } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';

@Component({
  selector: 'app-logging',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, CardModule, DropdownModule, CheckboxModule, TagModule, ConfirmDialogModule],
  providers: [CookieService, ConfirmationService],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-3xl font-semibold text-color mb-2">System Logs</h1>
        <p class="text-muted-color">Real-time system log monitoring</p>
      </div>

      <!-- Controls -->
      <p-card class="mb-6">
        <div class="flex justify-between items-center flex-wrap gap-4">
          <div class="flex gap-2">
            <p-button
              [label]="autoRefresh ? 'Pause' : 'Resume'"
              [icon]="autoRefresh ? 'pi pi-pause' : 'pi pi-play'"
              [severity]="autoRefresh ? 'success' : 'secondary'"
              (onClick)="toggleAutoRefresh()">
            </p-button>
            <p-button
              label="Refresh"
              icon="pi pi-refresh"
              (onClick)="refreshLogs()">
            </p-button>
            <p-button
              label="Clear Logs"
              icon="pi pi-trash"
              severity="danger"
              (onClick)="clearLogs()">
            </p-button>
          </div>

          <div class="flex items-center gap-4">
            <div class="flex items-center gap-2">
              <label class="font-medium text-color">Level Filter:</label>
              <p-dropdown
                [(ngModel)]="levelFilter"
                [options]="levelOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="All Levels"
                (onChange)="applyFilter()"
                styleClass="w-40">
              </p-dropdown>
            </div>

            <div class="flex items-center gap-2">
              <p-checkbox
                [(ngModel)]="followLogs"
                [binary]="true"
                inputId="followLogs"
                (onChange)="onFollowLogsChange()">
              </p-checkbox>
              <label for="followLogs" class="font-medium text-color">Follow logs</label>
            </div>
          </div>
        </div>
      </p-card>

      <!-- Log Table -->
      <p-card>
        <p-table
          [value]="filteredLogs"
          [scrollable]="true"
          scrollHeight="600px"
          styleClass="p-datatable-sm">

          <ng-template pTemplate="caption">
            <div class="flex justify-between items-center">
              <span class="text-lg font-medium">System Logs</span>
              <span class="text-muted-color text-sm">
                Showing {{ filteredLogs.length }} of {{ logs.length }} logs
              </span>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th style="width: 200px">Timestamp</th>
              <th style="width: 80px" class="text-center">Level</th>
              <th style="width: 200px">Logger</th>
              <th>Message</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-log>
            <tr [class]="getLogRowClass(log.level)">
              <td class="font-mono text-sm">{{ log.timestamp }}</td>
              <td class="text-center">
                <p-tag
                  [value]="log.level"
                  [severity]="getLogSeverity(log.level)"
                  styleClass="text-xs">
                </p-tag>
              </td>
              <td class="font-mono text-sm" [title]="log.logger">{{ truncateLogger(log.logger) }}</td>
              <td>{{ log.message }}</td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="4" class="text-center py-8">
                <div class="text-muted-color">
                  <i class="pi pi-info-circle text-3xl mb-2 block"></i>
                  <p>No logs available.</p>
                </div>
              </td>
            </tr>
          </ng-template>

        </p-table>
      </p-card>

      <!-- Confirmation Dialog -->
      <p-confirmDialog></p-confirmDialog>
    </div>
  `,
  styles: []
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

  levelOptions = [
    { label: 'All Levels', value: '' },
    { label: 'ERROR', value: 'ERROR' },
    { label: 'WARN', value: 'WARN' },
    { label: 'INFO', value: 'INFO' },
    { label: 'DEBUG', value: 'DEBUG' },
    { label: 'TRACE', value: 'TRACE' }
  ];

  constructor(
    private loggingService: LoggingService,
    private confirmationService: ConfirmationService
  ) {}

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
    this.confirmationService.confirm({
      message: 'Are you sure you want to clear all logs? This action cannot be undone!',
      header: 'Confirm Clear',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.loggingService.clearLogs().subscribe(() => {
          this.logs = [];
          this.filteredLogs = [];
        });
      }
    });
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

  getLogSeverity(level: string): string {
    switch (level) {
      case 'ERROR': return 'danger';
      case 'WARN': return 'warning';
      case 'INFO': return 'info';
      case 'DEBUG': return 'success';
      case 'TRACE': return 'secondary';
      default: return 'secondary';
    }
  }

  getLogRowClass(level: string): string {
    switch (level) {
      case 'ERROR': return 'bg-surface-100';
      case 'WARN': return 'bg-surface-50';
      default: return '';
    }
  }
}