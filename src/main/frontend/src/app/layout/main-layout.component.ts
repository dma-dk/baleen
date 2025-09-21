import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterOutlet } from '@angular/router';
import { SidebarModule } from 'primeng/sidebar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterOutlet, SidebarModule, ButtonModule, MenuModule],
  template: `
    <div class="min-h-screen bg-surface-50 flex">
      <!-- Sidebar -->
      <div class="w-64 bg-surface-800 text-surface-0 shadow-lg flex flex-col">
        <!-- Header -->
        <div class="p-6 border-b border-surface-600">
          <h2 class="text-xl font-semibold text-surface-0">Baleen Console</h2>
        </div>

        <!-- Navigation Menu -->
        <nav class="flex-1 py-4">
          <ul class="space-y-1 px-3">
            <li>
              <a routerLink="/home"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-home mr-3 text-lg"></i>
                <span>Home</span>
              </a>
            </li>
            <li>
              <a routerLink="/subscribers"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-users mr-3 text-lg"></i>
                <span>Subscribers</span>
              </a>
            </li>
            <li>
              <a routerLink="/s124-datasets"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-chart-bar mr-3 text-lg"></i>
                <span>S-124 Datasets</span>
              </a>
            </li>
            <li>
              <a routerLink="/niord"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-map mr-3 text-lg"></i>
                <span>Niord</span>
              </a>
            </li>
            <li>
              <a routerLink="/logging"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-file-edit mr-3 text-lg"></i>
                <span>Logging</span>
              </a>
            </li>
            <li>
              <a routerLink="/about"
                 routerLinkActive="bg-surface-700 border-l-4 border-primary"
                 class="flex items-center px-3 py-2 text-surface-200 hover:text-surface-0 hover:bg-surface-700 rounded transition-colors duration-200">
                <i class="pi pi-info-circle mr-3 text-lg"></i>
                <span>About</span>
              </a>
            </li>
          </ul>
        </nav>

        <!-- Footer -->
        <div class="p-4 border-t border-surface-600">
          <p-button
            label="Logout"
            icon="pi pi-sign-out"
            severity="danger"
            [text]="true"
            class="w-full"
            (onClick)="logout()">
          </p-button>
        </div>
      </div>

      <!-- Main Content -->
      <div class="flex-1 overflow-auto">
        <main class="p-6">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: []
})
export class MainLayoutComponent {
  logout() {
    // Redirect to Spring Security logout endpoint
    // This will do nothing when security is disabled locally
    window.location.href = '/logout';
  }
}