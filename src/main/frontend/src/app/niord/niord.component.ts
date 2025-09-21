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
    <div class="space-y-8">
      <!-- Header -->
      <div class="space-y-2">
        <h1 class="text-3xl font-semibold text-color">Niord</h1>
        <p class="text-muted-color">Manage Niord system integration settings</p>
      </div>

      <!-- Configuration Card -->
      <p-card header="Niord Configuration">
        <div class="space-y-6">
          <div *ngIf="!loading" class="space-y-2">
            <label class="font-semibold text-color block">Niord Endpoint:</label>
            <div class="p-4 bg-surface-100 rounded-md font-mono text-sm break-all">
              <span *ngIf="niordEndpoint">{{ niordEndpoint }}</span>
              <span *ngIf="!niordEndpoint" class="text-muted-color">Not configured</span>
            </div>
          </div>
          <div *ngIf="loading" class="space-y-2">
            <label class="font-semibold text-color block">Niord Endpoint:</label>
            <p-skeleton height="3rem"></p-skeleton>
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