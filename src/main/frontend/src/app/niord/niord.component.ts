import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-niord',
  standalone: true,
  imports: [CommonModule, CardModule, SkeletonModule],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-3xl font-semibold text-color mb-2">Niord</h1>
        <p class="text-muted-color">Manage Niord system integration settings</p>
      </div>

      <!-- Configuration Card -->
      <p-card header="Niord Configuration">
        <div class="space-y-4">
          <div *ngIf="!loading">
            <label class="font-semibold text-color block mb-2">Niord Endpoint:</label>
            <div class="p-3 bg-surface-100 rounded-md font-mono text-sm break-all">
              <span *ngIf="niordEndpoint">{{ niordEndpoint }}</span>
              <span *ngIf="!niordEndpoint" class="text-muted-color">Not configured</span>
            </div>
          </div>
          <div *ngIf="loading">
            <label class="font-semibold text-color block mb-2">Niord Endpoint:</label>
            <p-skeleton height="3rem" styleClass="mb-2"></p-skeleton>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
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