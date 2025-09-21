import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SubscriberService, Subscriber } from '../services/subscriber.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-subscribers',
  standalone: true,
  imports: [CommonModule, TableModule, ButtonModule, CardModule, ConfirmDialogModule, ToastModule, TagModule, InputTextModule],
  providers: [ConfirmationService, MessageService],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-3xl font-semibold text-color mb-2">Subscribers</h1>
        <p class="text-muted-color">Manage and view all SECOM subscribers</p>
      </div>

      <!-- Controls -->
      <p-card class="mb-6">
        <div class="flex justify-between items-center flex-wrap gap-4">
          <div class="flex gap-2">
            <p-button
              label="Refresh"
              icon="pi pi-refresh"
              (onClick)="refreshSubscribers()"
              [loading]="loading">
            </p-button>
            <p-button
              label="Clear All"
              icon="pi pi-trash"
              severity="danger"
              (onClick)="confirmClearAll()"
              [disabled]="subscribers.length === 0 || loading">
            </p-button>
          </div>

          <div class="text-muted-color text-sm">
            Total subscribers: {{ subscribers.length }}
          </div>
        </div>
      </p-card>

      <!-- Data Table -->
      <p-card>
        <p-table
          [value]="subscribers"
          [loading]="loading"
          [paginator]="true"
          [rows]="10"
          [showCurrentPageReport]="true"
          currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"
          [rowsPerPageOptions]="[10, 25, 50]"
          [globalFilterFields]="['nodeMrn', 'dataProductType', 'productVersion', 'containerType', 'unlocode']"
          styleClass="p-datatable-striped">

          <ng-template pTemplate="caption">
            <div class="flex justify-between items-center">
              <span class="text-lg font-medium">SECOM Subscribers</span>
              <span class="p-input-icon-left">
                <i class="pi pi-search"></i>
                <input
                  pInputText
                  type="text"
                  (input)="onGlobalFilter($any($event.target).value)"
                  placeholder="Search subscribers..."
                  class="w-full sm:w-auto" />
              </span>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="nodeMrn">
                Node MRN <p-sortIcon field="nodeMrn"></p-sortIcon>
              </th>
              <th pSortableColumn="dataProductType">
                Product Type <p-sortIcon field="dataProductType"></p-sortIcon>
              </th>
              <th pSortableColumn="productVersion">
                Version <p-sortIcon field="productVersion"></p-sortIcon>
              </th>
              <th pSortableColumn="containerType">
                Container Type <p-sortIcon field="containerType"></p-sortIcon>
              </th>
              <th pSortableColumn="unlocode">
                UN/LOCODE <p-sortIcon field="unlocode"></p-sortIcon>
              </th>
              <th pSortableColumn="subscriptionStart">
                Start Date <p-sortIcon field="subscriptionStart"></p-sortIcon>
              </th>
              <th pSortableColumn="subscriptionEnd">
                End Date <p-sortIcon field="subscriptionEnd"></p-sortIcon>
              </th>
              <th>Status</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-subscriber>
            <tr>
              <td class="font-mono text-sm">{{ subscriber.nodeMrn || 'N/A' }}</td>
              <td>{{ subscriber.dataProductType || 'N/A' }}</td>
              <td>{{ subscriber.productVersion || 'N/A' }}</td>
              <td>{{ subscriber.containerType || 'N/A' }}</td>
              <td>{{ subscriber.unlocode || 'N/A' }}</td>
              <td>{{ formatDate(subscriber.subscriptionStart) }}</td>
              <td>{{ formatDate(subscriber.subscriptionEnd) }}</td>
              <td>
                <p-tag
                  [value]="isActive(subscriber) ? 'Active' : 'Expired'"
                  [severity]="isActive(subscriber) ? 'success' : 'danger'">
                </p-tag>
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="8" class="text-center py-8">
                <div class="text-muted-color">
                  <i class="pi pi-info-circle text-3xl mb-2 block"></i>
                  <p>{{ error || 'No subscribers found.' }}</p>
                </div>
              </td>
            </tr>
          </ng-template>

        </p-table>
      </p-card>

      <!-- Toast Messages -->
      <p-toast></p-toast>

      <!-- Confirmation Dialog -->
      <p-confirmDialog></p-confirmDialog>
    </div>
  `,
  styles: []
})
export class SubscribersComponent implements OnInit {
  subscribers: Subscriber[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private subscriberService: SubscriberService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

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
        let errorMessage = 'Failed to load subscribers';

        if (err.status === 0) {
          errorMessage = 'Cannot connect to the server. Make sure the backend is running on port 8080.';
        } else if (err.status === 404) {
          errorMessage = 'Subscribers endpoint not found. The API might not be deployed correctly.';
        } else if (err.status === 500) {
          errorMessage = 'Server error occurred. Check the backend logs for details.';
        } else {
          errorMessage = `Failed to load subscribers: ${err.message || 'Unknown error'}`;
        }

        this.error = errorMessage;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: errorMessage
        });
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

  confirmClearAll() {
    this.confirmationService.confirm({
      message: 'Are you sure you want to clear all subscribers? This action cannot be undone!',
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.clearAllSubscribers();
      }
    });
  }

  onGlobalFilter(value: string) {
    // Global filter method for table
    // This would be implemented with a table reference in a real component
  }

  clearAllSubscribers() {
    this.loading = true;
    this.error = null;

    this.subscriberService.clearAllSubscribers().subscribe({
      next: () => {
        this.subscribers = [];
        this.loading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'All subscribers have been cleared'
        });
      },
      error: (err) => {
        console.error('Error clearing subscribers:', err);
        const errorMessage = 'Failed to clear subscribers. Please try again.';
        this.error = errorMessage;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: errorMessage
        });
        this.loading = false;
        // Refresh the list in case some were deleted
        this.loadSubscribers();
      }
    });
  }
}