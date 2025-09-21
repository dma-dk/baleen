import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

interface DatabaseInfo {
  url: string;
  username: string;
  driverClassName: string;
  databaseProductName: string;
  databaseProductVersion: string;
  connectionStatus: string;
}

@Component({
  selector: 'app-database',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule, TagModule],
  template: `
    <div class="space-y-8">
      <!-- Header -->
      <div class="space-y-2">
        <h1 class="text-3xl font-semibold text-color">Database</h1>
        <p class="text-muted-color">Database configuration and connection status</p>
      </div>

      <!-- Database Info -->
      <p-card header="Database Information">
        <div class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div class="space-y-2">
              <label class="text-sm font-medium text-muted-color">URL:</label>
              <div class="p-4 bg-surface-100 rounded-md font-mono text-sm break-all">
                {{ databaseInfo?.url || 'Loading...' }}
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-muted-color">Username:</label>
              <div class="p-4 bg-surface-100 rounded-md font-mono text-sm">
                {{ databaseInfo?.username || 'Loading...' }}
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-muted-color">Driver:</label>
              <div class="p-4 bg-surface-100 rounded-md font-mono text-sm break-all">
                {{ databaseInfo?.driverClassName || 'Loading...' }}
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-muted-color">Database Type:</label>
              <div class="p-4 bg-surface-100 rounded-md text-sm">
                {{ databaseInfo?.databaseProductName || 'Loading...' }}
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium text-muted-color">Version:</label>
              <div class="p-4 bg-surface-100 rounded-md text-sm">
                {{ databaseInfo?.databaseProductVersion || 'Loading...' }}
              </div>
            </div>
          </div>

          <div class="border-t pt-6">
            <div class="flex items-center gap-4">
              <p-button
                label="Test Connection"
                icon="pi pi-link"
                (onClick)="testConnection()"
                [loading]="testing">
              </p-button>

              <p-tag
                *ngIf="testSuccess !== null"
                [value]="testMessage"
                [severity]="testSuccess ? 'success' : 'danger'"
                [icon]="testSuccess ? 'pi pi-check' : 'pi pi-times'">
              </p-tag>
            </div>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
})
export class DatabaseComponent implements OnInit {
  databaseInfo: DatabaseInfo | null = null;
  testing = false;
  testSuccess: boolean | null = null;
  testMessage = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadDatabaseInfo();
  }

  loadDatabaseInfo() {
    this.http.get<DatabaseInfo>('/api/about/database')
      .subscribe({
        next: (data) => {
          this.databaseInfo = data;
        },
        error: (error) => {
          console.error('Failed to load database info:', error);
        }
      });
  }

  testConnection() {
    this.testing = true;
    this.testSuccess = null;
    this.testMessage = '';

    this.http.post<{success: boolean, message: string}>('/api/about/database/test', {})
      .subscribe({
        next: (response) => {
          this.testing = false;
          this.testSuccess = response.success;
          this.testMessage = response.message;
        },
        error: (error) => {
          this.testing = false;
          this.testSuccess = false;
          this.testMessage = error.error?.message || 'Connection test failed';
        }
      });
  }
}