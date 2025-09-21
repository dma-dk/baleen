import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Home</h1>
        <p>Welcome to Baleen - S-124 Navigational Warnings Management</p>
      </header>
      
      <div class="card card-padded">
        <h2>Platform Overview</h2>
      </div>
      
      <footer class="page-footer">
        <p>Built on {{ buildDate | date:'medium' }}</p>
      </footer>
    </div>
  `,
  styles: [`
    .feature-list {
      list-style-type: none;
      padding-left: 0;
    }
    
    .feature-list li {
      padding: 0.5rem 0;
      border-bottom: 1px solid #e0e0e0;
    }
    
    .feature-list li:last-child {
      border-bottom: none;
    }
    
    .page-footer {
      margin-top: 3rem;
      padding-top: 2rem;
      border-top: 1px solid #e0e0e0;
      text-align: center;
      color: #7f8c8d;
      font-size: 0.9rem;
    }
  `]
})
export class HomeComponent implements OnInit {
  buildDate = new Date(environment.buildTimestamp);

  constructor() {}

  ngOnInit() {
    console.log('Home component initialized');
  }
}