import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { CardModule } from 'primeng/card';

interface AboutInfo {
  backendUrl: string;
  version: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CardModule],
  template: `
    <div class="space-y-8">
      <div class="space-y-2">
        <h1 class="text-3xl font-semibold text-color">Welcome to Baleen</h1>
        <p class="text-muted-color">S-124 Navigational Warnings Management Platform</p>
      </div>

      <!-- Application Info -->
      <p-card header="Application Information">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div class="space-y-2">
            <label class="text-sm font-medium text-muted-color">Backend URL:</label>
            <div class="p-3 bg-surface-100 rounded-md font-mono text-sm break-all">
              {{ aboutInfo?.backendUrl || window.location.origin }}
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-muted-color">Build Date:</label>
            <div class="p-3 bg-surface-100 rounded-md text-sm">
              {{ formatDate(environment.buildTimestamp) }}
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium text-muted-color">Version:</label>
            <div class="p-3 bg-surface-100 rounded-md text-sm">
              {{ aboutInfo?.version || 'Loading...' }}
            </div>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
})
export class HomeComponent implements OnInit {
  aboutInfo: AboutInfo | null = null;
  environment = environment;
  window = window;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAboutInfo();
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

  formatDate(timestamp: string): string {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    return date.toLocaleString();
  }
}