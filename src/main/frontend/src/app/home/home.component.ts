import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CardModule],
  template: `
    <div class="p-6">
      <h1 class="text-color text-2xl font-semibold mb-4">Welcome to Baleen</h1>
      <p class="text-muted-color">S-124 Navigational Warnings Management Platform</p>

      <p-card header="System Information" class="mt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-muted-color mb-2">
              <strong>Build Date:</strong> {{ buildDate | date:'full' }}
            </p>
            <p class="text-muted-color">
              <strong>Environment:</strong> {{ isProduction ? 'Production' : 'Development' }}
            </p>
          </div>
          <div class="text-right">
            <div class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800">
              <i class="pi pi-check-circle mr-2"></i>
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