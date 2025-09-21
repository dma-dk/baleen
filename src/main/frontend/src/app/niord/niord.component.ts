import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-niord',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Niord</h1>
        <p>Manage Niord system integration settings</p>
      </header>
      
      <div class="card card-padded">
        <div class="endpoint-info" *ngIf="!loading">
          <strong>Niord Endpoint:</strong> 
          <span *ngIf="niordEndpoint">{{ niordEndpoint }}</span>
          <span *ngIf="!niordEndpoint" class="text-muted">Not configured</span>
        </div>
        <div class="endpoint-info" *ngIf="loading">
          <strong>Niord Endpoint:</strong> <span class="text-muted">Loading...</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .endpoint-info {
      font-size: 1.1rem;
      color: #333;
      padding: 1rem;
      background-color: #f8f9fa;
      border-radius: 4px;
      border: 1px solid #dee2e6;
    }
  `]
})
export class NiordComponent implements OnInit {
  niordEndpoint: string = '';
  loading: boolean = true;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<{endpoint?: string}>('/api/niord/config')
      .subscribe({
        next: (config) => {
          console.log('Niord config received:', config);
          this.niordEndpoint = config.endpoint || '';
          this.loading = false;
        },
        error: (error) => {
          console.error('Error fetching Niord config:', error);
          this.niordEndpoint = '';
          this.loading = false;
        }
      });
  }
}