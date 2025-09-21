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
        <nav class="flex-1 py-6">
          <ul class="space-y-2 px-4">
            <li>
              <a routerLink="/home"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-home nav-icon"></i>
                <span>Home</span>
              </a>
            </li>
            <li>
              <a routerLink="/subscribers"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-users nav-icon"></i>
                <span>Subscribers</span>
              </a>
            </li>
            <li>
              <a routerLink="/s124-datasets"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-chart-bar nav-icon"></i>
                <span>S-124 Datasets</span>
              </a>
            </li>
            <li>
              <a routerLink="/niord"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-map nav-icon"></i>
                <span>Niord</span>
              </a>
            </li>
            <li>
              <a routerLink="/logging"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-file-edit nav-icon"></i>
                <span>Logging</span>
              </a>
            </li>
            <li>
              <a routerLink="/database"
                 routerLinkActive="nav-link-active"
                 class="nav-link">
                <i class="pi pi-database nav-icon"></i>
                <span>Database</span>
              </a>
            </li>
          </ul>
        </nav>

        <!-- Footer -->
        <div class="p-6 border-t border-surface-600">
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
        <main class="p-8">
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