import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { S124DatasetService, S124Dataset, S124DatasetDetail } from '../services/s124-dataset.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { TabViewModule } from 'primeng/tabview';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-s124-datasets',
  standalone: true,
  imports: [CommonModule, TableModule, ButtonModule, CardModule, ConfirmDialogModule, ToastModule, TagModule, DialogModule, TabViewModule],
  providers: [ConfirmationService, MessageService],
  template: `
    <div class="space-y-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-3xl font-semibold text-color mb-2">S-124 Datasets</h1>
        <p class="text-muted-color">Manage and view all S-124 navigational warning datasets</p>
      </div>

      <!-- Controls -->
      <p-card class="mb-6">
        <div class="flex justify-between items-center flex-wrap gap-4">
          <div class="flex gap-2">
            <p-button
              label="Refresh"
              icon="pi pi-refresh"
              (onClick)="refreshDatasets()"
              [loading]="loading">
            </p-button>
            <p-button
              label="Clear All"
              icon="pi pi-trash"
              severity="danger"
              (onClick)="showClearConfirmation()"
              [disabled]="datasets.length === 0 || loading">
            </p-button>
            <p-button
              *ngIf="niordConfigured"
              label="Reload from Niord"
              icon="pi pi-download"
              (onClick)="showReloadConfirmation()"
              [disabled]="loading">
            </p-button>
          </div>

          <div class="text-muted-color text-sm">
            Total datasets: {{ totalElements }}
          </div>
        </div>
      </p-card>

      <!-- Data Table -->
      <p-card>
        <p-table
          [value]="datasets"
          [loading]="loading"
          [paginator]="true"
          [rows]="pageSize"
          [showCurrentPageReport]="true"
          currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"
          [rowsPerPageOptions]="[10, 20, 50]"
          styleClass="p-datatable-striped">

          <ng-template pTemplate="caption">
            <div class="flex justify-between items-center">
              <span class="text-lg font-medium">S-124 Datasets</span>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="id">
                ID <p-sortIcon field="id"></p-sortIcon>
              </th>
              <th pSortableColumn="mrn">
                MRN <p-sortIcon field="mrn"></p-sortIcon>
              </th>
              <th pSortableColumn="uuid">
                UUID <p-sortIcon field="uuid"></p-sortIcon>
              </th>
              <th pSortableColumn="dataProductVersion">
                Version <p-sortIcon field="dataProductVersion"></p-sortIcon>
              </th>
              <th pSortableColumn="validFrom">
                Valid From <p-sortIcon field="validFrom"></p-sortIcon>
              </th>
              <th pSortableColumn="validTo">
                Valid To <p-sortIcon field="validTo"></p-sortIcon>
              </th>
              <th pSortableColumn="createdAt">
                Created <p-sortIcon field="createdAt"></p-sortIcon>
              </th>
              <th>References</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-dataset>
            <tr class="cursor-pointer" (click)="showDatasetDetails(dataset.id)">
              <td>{{ dataset.id }}</td>
              <td class="font-mono text-sm">{{ dataset.mrn || 'N/A' }}</td>
              <td class="font-mono text-sm">{{ dataset.uuid ? (dataset.uuid.substring(0, 8) + '...') : 'N/A' }}</td>
              <td>{{ dataset.dataProductVersion || 'N/A' }}</td>
              <td>{{ formatDate(dataset.validFrom) }}</td>
              <td>{{ formatDate(dataset.validTo) }}</td>
              <td>{{ formatDate(dataset.createdAt) }}</td>
              <td>
                <p-tag
                  [value]="dataset.referencedDatasetIds.length.toString()"
                  severity="info">
                </p-tag>
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="8" class="text-center py-8">
                <div class="text-muted-color">
                  <i class="pi pi-info-circle text-3xl mb-2 block"></i>
                  <p>{{ error || 'No datasets found.' }}</p>
                </div>
              </td>
            </tr>
          </ng-template>

        </p-table>
      </p-card>

      <!-- Dataset Details Dialog -->
      <p-dialog
        [(visible)]="showDetailsDialog"
        [modal]="true"
        [closable]="true"
        [draggable]="false"
        [resizable]="false"
        header="Dataset Details"
        styleClass="w-11/12 max-w-4xl">

        <p-tabView *ngIf="selectedDatasetDetail">
          <p-tabPanel header="Attributes">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="space-y-4">
                <div>
                  <label class="font-semibold text-color block mb-1">ID:</label>
                  <span>{{ selectedDatasetDetail.id }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">MRN:</label>
                  <span class="font-mono text-sm">{{ selectedDatasetDetail.mrn || 'N/A' }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">UUID:</label>
                  <span class="font-mono text-sm break-all">{{ selectedDatasetDetail.uuid || 'N/A' }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">Data Product Version:</label>
                  <span>{{ selectedDatasetDetail.dataProductVersion || 'N/A' }}</span>
                </div>
              </div>
              <div class="space-y-4">
                <div>
                  <label class="font-semibold text-color block mb-1">Valid From:</label>
                  <span>{{ formatDate(selectedDatasetDetail.validFrom) }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">Valid To:</label>
                  <span>{{ formatDate(selectedDatasetDetail.validTo) }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">Created At:</label>
                  <span>{{ formatDate(selectedDatasetDetail.createdAt) }}</span>
                </div>
                <div>
                  <label class="font-semibold text-color block mb-1">Referenced Datasets:</label>
                  <p-tag
                    [value]="selectedDatasetDetail.referencedDatasetIds.length.toString()"
                    severity="info">
                  </p-tag>
                  <span *ngIf="selectedDatasetDetail.referencedDatasetIds.length > 0" class="text-muted-color text-sm ml-2">
                    ({{ selectedDatasetDetail.referencedDatasetIds.join(', ') }})
                  </span>
                </div>
              </div>
              <div class="col-span-1 md:col-span-2">
                <label class="font-semibold text-color block mb-1">Geometry (WKT):</label>
                <span class="font-mono text-sm break-all">{{ selectedDatasetDetail.geometryWkt || 'N/A' }}</span>
              </div>
            </div>
          </p-tabPanel>

          <p-tabPanel header="GML Content">
            <div class="flex justify-between items-center mb-4">
              <span class="text-muted-color text-sm">GML/XML Content</span>
              <p-button
                label="Copy"
                icon="pi pi-copy"
                size="small"
                severity="secondary"
                (onClick)="copyGmlToClipboard()">
              </p-button>
            </div>
            <pre class="bg-surface-100 border border-surface-300 rounded-lg p-4 font-mono text-sm overflow-auto max-h-96 whitespace-pre-wrap">{{ selectedDatasetDetail.gml || 'No GML content available' }}</pre>
          </p-tabPanel>
        </p-tabView>

      </p-dialog>

      <!-- Toast Messages -->
      <p-toast></p-toast>

      <!-- Confirmation Dialog -->
      <p-confirmDialog></p-confirmDialog>
    </div>
  `,
  styles: []
})
export class S124DatasetsComponent implements OnInit {
  datasets: S124Dataset[] = [];
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  niordConfigured = false;
  loading = false;
  error: string | null = null;
  showDetailsDialog = false;
  selectedDatasetDetail: S124DatasetDetail | null = null;
  activeTab: 'attributes' | 'gml' = 'attributes';
  currentSortBy = 'createdAt';
  currentSortDirection: 'ASC' | 'DESC' = 'DESC';

  constructor(
    private datasetService: S124DatasetService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.loadDatasets();
    this.checkNiordStatus();
  }

  loadDatasets(): void {
    this.loading = true;
    this.error = null;
    
    this.datasetService.getDatasets(this.currentPage, this.pageSize, this.currentSortBy, this.currentSortDirection).subscribe({
      next: (page) => {
        this.datasets = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading datasets:', err);
        if (err.status === 0) {
          this.error = 'Cannot connect to the server. Make sure the backend is running on port 8080.';
        } else if (err.status === 404) {
          this.error = 'Datasets endpoint not found. The API might not be deployed correctly.';
        } else if (err.status === 500) {
          this.error = 'Server error occurred. Check the backend logs for details.';
        } else {
          this.error = `Failed to load datasets: ${err.message || 'Unknown error'}`;
        }
        this.loading = false;
      }
    });
  }

  refreshDatasets(): void {
    this.loadDatasets();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadDatasets();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadDatasets();
    }
  }

  formatDate(date: string | null): string {
    if (!date) return 'N/A';
    try {
      const dateObj = new Date(date);
      return dateObj.toLocaleDateString() + ' ' + dateObj.toLocaleTimeString();
    } catch {
      return 'N/A';
    }
  }

  checkNiordStatus(): void {
    this.datasetService.getNiordStatus().subscribe({
      next: (status) => {
        this.niordConfigured = status.configured;
      },
      error: (error) => {
        console.error('Error checking Niord status:', error);
      }
    });
  }

  showClearConfirmation(): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to clear all datasets? This action cannot be undone!',
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.clearAllDatasets();
      }
    });
  }

  showReloadConfirmation(): void {
    this.confirmationService.confirm({
      message: 'This will clear all existing datasets and reload from Niord. Continue?',
      header: 'Confirm Reload',
      icon: 'pi pi-question-circle',
      accept: () => {
        this.reloadFromNiord();
      }
    });
  }

  clearAllDatasets(): void {
    this.loading = true;
    this.error = null;

    this.datasetService.clearAllDatasets().subscribe({
      next: () => {
        this.datasets = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.currentPage = 0;
        this.loading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'All datasets have been cleared'
        });
      },
      error: (err) => {
        console.error('Error clearing datasets:', err);
        this.error = 'Failed to clear datasets. Please try again.';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to clear datasets. Please try again.'
        });
        this.loading = false;
        this.loadDatasets();
      }
    });
  }

  reloadFromNiord(): void {
    this.loading = true;
    this.error = null;

    this.datasetService.reloadFromNiord().subscribe({
      next: (result) => {
        if (result.success) {
          this.loadDatasets();
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Datasets reloaded from Niord successfully'
          });
        } else {
          this.error = 'Failed: ' + result.message;
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed: ' + result.message
          });
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error reloading from Niord:', err);
        this.error = 'Failed to reload from Niord. Please try again.';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to reload from Niord. Please try again.'
        });
        this.loading = false;
      }
    });
  }

  showDatasetDetails(datasetId: number): void {
    this.loading = true;
    this.error = null;
    
    this.datasetService.getDatasetDetails(datasetId).subscribe({
      next: (detail) => {
        this.selectedDatasetDetail = detail;
        this.activeTab = 'attributes';
        this.showDetailsDialog = true;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading dataset details:', err);
        this.error = 'Failed to load dataset details. Please try again.';
        this.loading = false;
      }
    });
  }

  closeDetailsDialog(): void {
    this.showDetailsDialog = false;
    this.selectedDatasetDetail = null;
  }

  copyGmlToClipboard(): void {
    if (this.selectedDatasetDetail?.gml) {
      navigator.clipboard.writeText(this.selectedDatasetDetail.gml).then(() => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'GML content copied to clipboard'
        });
      }).catch(err => {
        console.error('Failed to copy GML content:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to copy GML content'
        });
      });
    }
  }

  sortBy(column: string): void {
    if (this.currentSortBy === column) {
      // Toggle direction if same column
      this.currentSortDirection = this.currentSortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      // New column, default to DESC for most fields, ASC for ID
      this.currentSortBy = column;
      this.currentSortDirection = column === 'id' ? 'ASC' : 'DESC';
    }
    
    // Reset to first page when sorting changes
    this.currentPage = 0;
    this.loadDatasets();
  }

  getSortClass(column: string): string {
    if (this.currentSortBy !== column) {
      return '';
    }
    return this.currentSortDirection.toLowerCase();
  }
}