import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterOutlet],
  template: `
    <div class="layout-container">
      <nav class="sidebar">
        <div class="sidebar-header">
          <h2>Baleen Console</h2>
        </div>
        
        <ul class="nav-menu">
          <li>
            <a routerLink="/home" routerLinkActive="active">
              <span class="nav-icon">🏠</span>
              <span class="nav-text">Home</span>
            </a>
          </li>
          <li>
            <a routerLink="/subscribers" routerLinkActive="active">
              <span class="nav-icon">📋</span>
              <span class="nav-text">Subscribers</span>
            </a>
          </li>
          <li>
            <a routerLink="/s124-datasets" routerLinkActive="active">
              <span class="nav-icon">📊</span>
              <span class="nav-text">S-124 Datasets</span>
            </a>
          </li>
          <li>
            <a routerLink="/niord" routerLinkActive="active">
              <span class="nav-icon">🌊</span>
              <span class="nav-text">Niord</span>
            </a>
          </li>
          <li>
            <a routerLink="/logging" routerLinkActive="active">
              <span class="nav-icon">📝</span>
              <span class="nav-text">Logging</span>
            </a>
          </li>
          <li>
            <a routerLink="/about" routerLinkActive="active">
              <span class="nav-icon">ℹ️</span>
              <span class="nav-text">About</span>
            </a>
          </li>
        </ul>
        
        <div class="sidebar-footer">
          <button class="logout-btn" (click)="logout()">
            <span class="nav-icon">🚪</span>
            <span class="nav-text">Logout</span>
          </button>
        </div>
      </nav>
      
      <div class="main-content">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .layout-container {
      display: flex;
      height: 100vh;
      background-color: #f5f5f5;
    }
    
    .sidebar {
      width: 260px;
      background-color: #2c3e50;
      color: white;
      box-shadow: 2px 0 5px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
    }
    
    .sidebar-header {
      padding: 2rem 1.5rem;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }
    
    .sidebar-header h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 600;
    }
    
    .nav-menu {
      list-style: none;
      padding: 0;
      margin: 1rem 0;
    }
    
    .nav-menu li {
      margin: 0;
    }
    
    .nav-menu a {
      display: flex;
      align-items: center;
      padding: 0.875rem 1.5rem;
      color: rgba(255,255,255,0.8);
      text-decoration: none;
      transition: all 0.3s ease;
    }
    
    .nav-menu a:hover {
      background-color: rgba(255,255,255,0.1);
      color: white;
    }
    
    .nav-menu a.active {
      background-color: #34495e;
      color: white;
      border-left: 3px solid #3498db;
    }
    
    .nav-icon {
      margin-right: 0.75rem;
      font-size: 1.25rem;
    }
    
    .nav-text {
      font-size: 0.975rem;
      font-weight: 500;
    }
    
    .main-content {
      flex: 1;
      overflow-y: auto;
      padding: 2rem;
    }
    
    .sidebar-footer {
      margin-top: auto;
      padding: 1.5rem;
      border-top: 1px solid rgba(255,255,255,0.1);
    }
    
    .logout-btn {
      display: flex;
      align-items: center;
      width: 100%;
      padding: 0.875rem 0;
      background: none;
      border: none;
      color: rgba(255,255,255,0.8);
      text-decoration: none;
      transition: all 0.3s ease;
      cursor: pointer;
      font-family: inherit;
    }
    
    .logout-btn:hover {
      color: #e74c3c;
      background-color: rgba(231, 76, 60, 0.1);
      border-radius: 4px;
    }
  `]
})
export class MainLayoutComponent {
  logout() {
    // Redirect to Spring Security logout endpoint
    // This will do nothing when security is disabled locally
    window.location.href = '/logout';
  }
}