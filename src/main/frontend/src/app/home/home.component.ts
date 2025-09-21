import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';
import { CardModule } from 'primeng/card';

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

      <p-card header="System Information">
        <div class="flex items-center justify-between">
          <div class="space-y-2">
            <p class="text-muted-color">
              <strong>Build Date:</strong> {{ buildDate | date:'full' }}
            </p>
            <p class="text-muted-color">
              <strong>Environment:</strong> {{ isProduction ? 'Production' : 'Development' }}
            </p>
          </div>
          <div class="text-right">
            <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              <i class="pi pi-check-circle"></i>
              System Online
            </div>
          </div>
        </div>
      </p-card>
    </div>
  `,
  styles: []
})
export class HomeComponent implements OnInit {
  buildDate = new Date(environment.buildTimestamp);
  isProduction = environment.production;

  constructor() {}

  ngOnInit(): void {
  }

}