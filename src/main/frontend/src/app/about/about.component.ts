import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

interface AboutInfo {
  backendUrl: string;
  version: string;
}

interface DatabaseInfo {
  url: string;
  username: string;
  driverClassName: string;
  databaseProductName: string;
  databaseProductVersion: string;
  connectionStatus: string;
}

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>About</h1>
        <p>System information and configuration</p>
      </header>

      <!-- Application Info -->
      <div class="card mb-3">
        <div class="card-padded">
          <h2>Application</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>Backend URL:</label>
              <span class="text-monospace">{{ aboutInfo?.backendUrl || window.location.origin }}</span>
            </div>
            <div class="info-item">
              <label>Build Date:</label>
              <span>{{ formatDate(environment.buildTimestamp) }}</span>
            </div>
            <div class="info-item">
              <label>Version:</label>
              <span>{{ aboutInfo?.version || 'Loading...' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Database Info -->
      <div class="card">
        <div class="card-padded">
          <h2>Database</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>URL:</label>
              <span class="text-monospace">{{ databaseInfo?.url || 'Loading...' }}</span>
            </div>
            <div class="info-item">
              <label>Username:</label>
              <span class="text-monospace">{{ databaseInfo?.username || 'Loading...' }}</span>
            </div>
            <div class="info-item">
              <label>Driver:</label>
              <span class="text-monospace">{{ databaseInfo?.driverClassName || 'Loading...' }}</span>
            </div>
            <div class="info-item">
              <label>Database Type:</label>
              <span>{{ databaseInfo?.databaseProductName || 'Loading...' }}</span>
            </div>
            <div class="info-item">
              <label>Version:</label>
              <span>{{ databaseInfo?.databaseProductVersion || 'Loading...' }}</span>
            </div>
          </div>
          
          <div class="connection-test mt-3">
            <button 
              class="btn-primary" 
              (click)="testConnection()"
              [disabled]="testing">
              {{ testing ? '⏳ Testing...' : '🔗 Test Connection' }}
            </button>
            <div class="test-result mt-2" [class.success]="testSuccess" [class.error]="testSuccess === false">
              {{ testMessage }}
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    h2 {
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
      color: #444;
      border-bottom: 2px solid #e0e0e0;
      padding-bottom: 10px;
    }
    
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 15px;
    }
    
    .info-item {
      display: flex;
      flex-direction: column;
      gap: 5px;
    }
    
    .info-item label {
      font-weight: 600;
      color: #666;
      font-size: 0.9rem;
    }
    
    .info-item span {
      background: #f5f5f5;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 0.95rem;
      word-break: break-all;
    }
    
    .connection-test {
      display: flex;
      align-items: flex-start;
      gap: 15px;
      flex-direction: column;
    }
    
    .test-result {
      font-size: 0.95rem;
      padding: 8px 12px;
      border-radius: 4px;
      display: none;
    }
    
    .test-result:not(:empty) {
      display: block;
    }
    
    .test-result.success {
      background: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }
    
    .test-result.error {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }

    .mb-3 {
      margin-bottom: 1.5rem;
    }
  `]
})
export class AboutComponent implements OnInit {
  aboutInfo: AboutInfo | null = null;
  databaseInfo: DatabaseInfo | null = null;
  testing = false;
  testSuccess: boolean | null = null;
  testMessage = '';
  environment = environment;
  window = window;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadAboutInfo();
    this.loadDatabaseInfo();
  }

  loadAboutInfo() {
    this.http.get<AboutInfo>('/api/about')
      .subscribe({
        next: (data) => {
          this.aboutInfo = data;
        },
        error: (error) => {
          console.error('Failed to load about info:', error);
        }
      });
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

  formatDate(timestamp: string): string {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    return date.toLocaleString();
  }
}