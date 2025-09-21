import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SubscriberService, Subscriber } from '../services/subscriber.service';

@Component({
  selector: 'app-subscribers',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>Subscribers</h1>
        <p>Manage and view all SECOM subscribers</p>
      </header>

      <!-- Confirmation Dialog -->
      <div *ngIf="showConfirmDialog" class="dialog-overlay" (click)="cancelClearAll()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <h3>Confirm Delete</h3>
          <p>Are you sure you want to clear all subscribers?</p>
          <p class="warning">This action cannot be undone!</p>
          <div class="dialog-buttons">
            <button class="btn-secondary" (click)="cancelClearAll()">Cancel</button>
            <button class="btn-danger" (click)="clearAllSubscribers()">Clear All</button>
          </div>
        </div>
      </div>

      <div class="controls">
        <div class="button-group">
          <button (click)="refreshSubscribers()" class="btn-primary">
            🔄 Refresh
          </button>
          <button (click)="showClearConfirmation()" class="btn-danger" [disabled]="subscribers.length === 0">
            🗑️ Clear All
          </button>
        </div>
        
        <div class="text-muted text-small">
          Total subscribers: {{ subscribers.length }}
        </div>
      </div>

      <div class="card card-flex">

        <div *ngIf="loading" class="loading">
          Loading subscribers...
        </div>

        <div *ngIf="error" class="error">
          {{ error }}
        </div>

        <div *ngIf="!loading && !error && subscribers.length === 0" class="no-data">
          No subscribers found.
        </div>

        <div *ngIf="!loading && !error && subscribers.length > 0" class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th>Node MRN</th>
                <th>Product Type</th>
                <th>Version</th>
                <th>Container Type</th>
                <th>UN/LOCODE</th>
                <th>Subscription Start</th>
                <th>Subscription End</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let subscriber of subscribers">
                <td class="mrn-cell">{{ subscriber.nodeMrn || 'N/A' }}</td>
                <td>{{ subscriber.dataProductType || 'N/A' }}</td>
                <td>{{ subscriber.productVersion || 'N/A' }}</td>
                <td>{{ subscriber.containerType || 'N/A' }}</td>
                <td>{{ subscriber.unlocode || 'N/A' }}</td>
                <td>{{ formatDate(subscriber.subscriptionStart) }}</td>
                <td>{{ formatDate(subscriber.subscriptionEnd) }}</td>
                <td>
                  <span class="status-badge" [class.active]="isActive(subscriber)" [class.expired]="!isActive(subscriber)">
                    {{ isActive(subscriber) ? 'Active' : 'Expired' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card {
      padding: 1.5rem;
    }

    .table-wrapper {
      margin: -1.5rem;
      padding: 1.5rem;
    }

    .mrn-cell {
      font-family: monospace;
      font-size: 0.8rem;
    }

    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.active {
      background-color: #d4edda;
      color: #155724;
    }

    .status-badge.expired {
      background-color: #f8d7da;
      color: #721c24;
    }
  `]
})
export class SubscribersComponent implements OnInit {
  subscribers: Subscriber[] = [];
  loading = false;
  error: string | null = null;
  showConfirmDialog = false;

  constructor(private subscriberService: SubscriberService) {}

  ngOnInit() {
    this.loadSubscribers();
  }

  loadSubscribers() {
    this.loading = true;
    this.error = null;
    
    this.subscriberService.getAllSubscribers().subscribe({
      next: (data) => {
        this.subscribers = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading subscribers:', err);
        if (err.status === 0) {
          this.error = 'Cannot connect to the server. Make sure the backend is running on port 8080.';
        } else if (err.status === 404) {
          this.error = 'Subscribers endpoint not found. The API might not be deployed correctly.';
        } else if (err.status === 500) {
          this.error = 'Server error occurred. Check the backend logs for details.';
        } else {
          this.error = `Failed to load subscribers: ${err.message || 'Unknown error'}`;
        }
        this.loading = false;
      }
    });
  }

  refreshSubscribers() {
    this.loadSubscribers();
  }

  formatDate(dateString: string | null): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  }

  isActive(subscriber: Subscriber): boolean {
    if (!subscriber.subscriptionEnd) return true;
    const endDate = new Date(subscriber.subscriptionEnd);
    return endDate > new Date();
  }

  showClearConfirmation() {
    this.showConfirmDialog = true;
  }

  cancelClearAll() {
    this.showConfirmDialog = false;
  }

  clearAllSubscribers() {
    this.showConfirmDialog = false;
    this.loading = true;
    this.error = null;
    
    this.subscriberService.clearAllSubscribers().subscribe({
      next: () => {
        this.subscribers = [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Error clearing subscribers:', err);
        this.error = 'Failed to clear subscribers. Please try again.';
        this.loading = false;
        // Refresh the list in case some were deleted
        this.loadSubscribers();
      }
    });
  }
}