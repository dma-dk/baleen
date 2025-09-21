import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { S124DatasetService, S124Dataset, S124DatasetDetail } from '../services/s124-dataset.service';

@Component({
  selector: 'app-s124-datasets',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <header class="page-header">
        <h1>S-124 Datasets</h1>
        <p>Manage and view all S-124 navigational warning datasets</p>
      </header>

      <!-- Confirmation Dialog -->
      <div *ngIf="showConfirmDialog" class="dialog-overlay" (click)="cancelClearAll()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <h3>Confirm Delete</h3>
          <p>{{ confirmMessage }}</p>
          <p class="warning">This action cannot be undone!</p>
          <div class="dialog-buttons">
            <button class="btn-secondary" (click)="cancelClearAll()">Cancel</button>
            <button class="btn-danger" (click)="executeConfirmedAction()">{{ confirmButtonText }}</button>
          </div>
        </div>
      </div>

      <!-- Dataset Details Dialog -->
      <div *ngIf="showDetailsDialog && selectedDatasetDetail" class="dialog-overlay" (click)="closeDetailsDialog()">
        <div class="details-dialog" (click)="$event.stopPropagation()">
          <div class="details-header">
            <h3>Dataset Details</h3>
            <button class="close-btn" (click)="closeDetailsDialog()">×</button>
          </div>
          
          <div class="details-content">
            <div class="details-tabs">
              <button 
                class="tab-btn" 
                [class.active]="activeTab === 'attributes'"
                (click)="activeTab = 'attributes'">
                Attributes
              </button>
              <button 
                class="tab-btn" 
                [class.active]="activeTab === 'gml'"
                (click)="activeTab = 'gml'">
                GML Content
              </button>
            </div>

            <div class="tab-content">
              <!-- Attributes Tab -->
              <div *ngIf="activeTab === 'attributes'" class="attributes-tab">
                <div class="attribute-grid">
                  <div class="attribute-row">
                    <label>ID:</label>
                    <span>{{ selectedDatasetDetail.id }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>MRN:</label>
                    <span class="monospace">{{ selectedDatasetDetail.mrn || 'N/A' }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>UUID:</label>
                    <span class="monospace">{{ selectedDatasetDetail.uuid || 'N/A' }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Data Product Version:</label>
                    <span>{{ selectedDatasetDetail.dataProductVersion || 'N/A' }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Valid From:</label>
                    <span>{{ formatDate(selectedDatasetDetail.validFrom) }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Valid To:</label>
                    <span>{{ formatDate(selectedDatasetDetail.validTo) }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Created At:</label>
                    <span>{{ formatDate(selectedDatasetDetail.createdAt) }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Geometry (WKT):</label>
                    <span class="monospace geometry-text">{{ selectedDatasetDetail.geometryWkt || 'N/A' }}</span>
                  </div>
                  <div class="attribute-row">
                    <label>Referenced Datasets:</label>
                    <span>
                      <span class="status-badge references">
                        {{ selectedDatasetDetail.referencedDatasetIds.length }}
                      </span>
                      <span *ngIf="selectedDatasetDetail.referencedDatasetIds.length > 0" class="reference-ids">
                        ({{ selectedDatasetDetail.referencedDatasetIds.join(', ') }})
                      </span>
                    </span>
                  </div>
                </div>
              </div>

              <!-- GML Content Tab -->
              <div *ngIf="activeTab === 'gml'" class="gml-tab">
                <div class="gml-header">
                  <span class="text-muted text-small">GML/XML Content</span>
                  <button class="btn-secondary copy-btn" (click)="copyGmlToClipboard()">
                    📋 Copy
                  </button>
                </div>
                <pre class="gml-content">{{ selectedDatasetDetail.gml || 'No GML content available' }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="controls">
        <div class="button-group">
          <button (click)="refreshDatasets()" class="btn-primary">
            🔄 Refresh
          </button>
          <button (click)="showClearConfirmation()" class="btn-danger" [disabled]="datasets.length === 0 || loading">
            🗑️ Clear All
          </button>
          <button 
            *ngIf="niordConfigured"
            (click)="showReloadConfirmation()" 
            class="btn-primary" 
            [disabled]="loading">
            📥 Reload from Niord
          </button>
        </div>
        
        <div class="text-muted text-small">
          Total datasets: {{ totalElements }}
        </div>
      </div>

      <div class="card card-flex">

        <div *ngIf="loading" class="loading">
          Loading datasets...
        </div>

        <div *ngIf="error" class="error">
          {{ error }}
        </div>

        <div *ngIf="!loading && !error && datasets.length === 0" class="no-data">
          No datasets found.
        </div>

        <div *ngIf="!loading && !error && datasets.length > 0" class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th class="sortable-header" (click)="sortBy('id')">
                  ID
                  <span class="sort-indicator" [class]="getSortClass('id')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('mrn')">
                  MRN
                  <span class="sort-indicator" [class]="getSortClass('mrn')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('uuid')">
                  UUID
                  <span class="sort-indicator" [class]="getSortClass('uuid')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('dataProductVersion')">
                  Version
                  <span class="sort-indicator" [class]="getSortClass('dataProductVersion')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('validFrom')">
                  Valid From
                  <span class="sort-indicator" [class]="getSortClass('validFrom')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('validTo')">
                  Valid To
                  <span class="sort-indicator" [class]="getSortClass('validTo')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th class="sortable-header" (click)="sortBy('createdAt')">
                  Created
                  <span class="sort-indicator" [class]="getSortClass('createdAt')">
                    <span class="sort-arrow">↕</span>
                  </span>
                </th>
                <th>References</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let dataset of datasets" class="clickable-row" (click)="showDatasetDetails(dataset.id)">
                <td>{{ dataset.id }}</td>
                <td class="mrn-cell">{{ dataset.mrn || 'N/A' }}</td>
                <td class="uuid-cell">{{ dataset.uuid ? (dataset.uuid.substring(0, 8) + '...') : 'N/A' }}</td>
                <td>{{ dataset.dataProductVersion || 'N/A' }}</td>
                <td>{{ formatDate(dataset.validFrom) }}</td>
                <td>{{ formatDate(dataset.validTo) }}</td>
                <td>{{ formatDate(dataset.createdAt) }}</td>
                <td>
                  <span class="status-badge references">
                    {{ dataset.referencedDatasetIds.length }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!loading && !error && datasets.length > 0 && totalPages > 1" class="pagination">
          <div class="pagination-info">
            Showing page {{ currentPage + 1 }} of {{ totalPages }}
          </div>
          <div class="pagination-controls">
            <button 
              (click)="previousPage()" 
              [disabled]="currentPage === 0"
              class="btn-secondary">
              Previous
            </button>
            <button 
              (click)="nextPage()" 
              [disabled]="currentPage === totalPages - 1"
              class="btn-secondary">
              Next
            </button>
          </div>
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

    .mrn-cell, .uuid-cell {
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

    .status-badge.references {
      background-color: #e3f2fd;
      color: #1565c0;
    }

    .pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 1rem;
      margin-top: 1rem;
      border-top: 1px solid #e5e7eb;
    }

    .pagination-info {
      font-size: 0.875rem;
      color: #6b7280;
    }

    .pagination-controls {
      display: flex;
      gap: 0.5rem;
    }

    .clickable-row {
      cursor: pointer;
    }

    .clickable-row:hover {
      background-color: #f8f9fa !important;
    }

    .details-dialog {
      background: white;
      border-radius: 8px;
      width: 90%;
      max-width: 800px;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
    }

    .details-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }

    .details-header h3 {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      padding: 0.25rem;
      width: 2rem;
      height: 2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
    }

    .close-btn:hover {
      background-color: #f3f4f6;
    }

    .details-content {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .details-tabs {
      display: flex;
      border-bottom: 1px solid #e5e7eb;
      padding: 0 1.5rem;
    }

    .tab-btn {
      background: none;
      border: none;
      padding: 1rem 1.5rem;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      color: #6b7280;
      font-weight: 500;
    }

    .tab-btn.active {
      color: #3b82f6;
      border-bottom-color: #3b82f6;
    }

    .tab-btn:hover {
      color: #374151;
    }

    .tab-content {
      flex: 1;
      overflow: auto;
      padding: 1.5rem;
    }

    .attribute-grid {
      display: grid;
      gap: 1rem;
    }

    .attribute-row {
      display: grid;
      grid-template-columns: 200px 1fr;
      gap: 1rem;
      align-items: start;
    }

    .attribute-row label {
      font-weight: 600;
      color: #374151;
    }

    .monospace {
      font-family: monospace;
      font-size: 0.9rem;
    }

    .geometry-text {
      word-break: break-all;
      font-size: 0.8rem;
    }

    .reference-ids {
      font-size: 0.875rem;
      color: #6b7280;
      margin-left: 0.5rem;
    }

    .gml-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .copy-btn {
      font-size: 0.875rem;
      padding: 0.5rem 1rem;
    }

    .gml-content {
      background-color: #f8f9fa;
      border: 1px solid #e5e7eb;
      border-radius: 4px;
      padding: 1rem;
      font-family: monospace;
      font-size: 0.875rem;
      line-height: 1.5;
      white-space: pre-wrap;
      word-wrap: break-word;
      overflow: auto;
      max-height: 400px;
    }

    .sortable-header {
      cursor: pointer;
      user-select: none;
      position: relative;
      padding-right: 1.5rem !important;
    }

    .sortable-header:hover {
      background-color: #f8f9fa;
    }

    .sort-indicator {
      position: absolute;
      right: 0.5rem;
      top: 50%;
      transform: translateY(-50%);
      opacity: 0.5;
      font-size: 0.75rem;
    }

    .sort-indicator.asc .sort-arrow::before {
      content: '↑';
      opacity: 1;
      color: #3b82f6;
    }

    .sort-indicator.desc .sort-arrow::before {
      content: '↓';
      opacity: 1;
      color: #3b82f6;
    }

    .sort-indicator.asc .sort-arrow,
    .sort-indicator.desc .sort-arrow {
      opacity: 0;
    }

    .sortable-header:hover .sort-indicator {
      opacity: 0.8;
    }

    .sort-indicator.asc,
    .sort-indicator.desc {
      opacity: 1;
    }
  `]
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
  showConfirmDialog = false;
  confirmMessage = '';
  confirmButtonText = '';
  pendingAction: 'clear' | 'reload' | null = null;
  showDetailsDialog = false;
  selectedDatasetDetail: S124DatasetDetail | null = null;
  activeTab: 'attributes' | 'gml' = 'attributes';
  currentSortBy = 'createdAt';
  currentSortDirection: 'ASC' | 'DESC' = 'DESC';

  constructor(private datasetService: S124DatasetService) { }

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
    this.confirmMessage = 'Are you sure you want to clear all datasets?';
    this.confirmButtonText = 'Clear All';
    this.pendingAction = 'clear';
    this.showConfirmDialog = true;
  }

  showReloadConfirmation(): void {
    this.confirmMessage = 'This will clear all existing datasets and reload from Niord. Continue?';
    this.confirmButtonText = 'Reload';
    this.pendingAction = 'reload';
    this.showConfirmDialog = true;
  }

  cancelClearAll(): void {
    this.showConfirmDialog = false;
    this.pendingAction = null;
  }

  executeConfirmedAction(): void {
    this.showConfirmDialog = false;
    
    if (this.pendingAction === 'clear') {
      this.clearAllDatasets();
    } else if (this.pendingAction === 'reload') {
      this.reloadFromNiord();
    }
    
    this.pendingAction = null;
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
      },
      error: (err) => {
        console.error('Error clearing datasets:', err);
        this.error = 'Failed to clear datasets. Please try again.';
        this.loading = false;
        // Refresh the list in case some were deleted
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
        } else {
          this.error = 'Failed: ' + result.message;
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error reloading from Niord:', err);
        this.error = 'Failed to reload from Niord. Please try again.';
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
        // Could add a toast notification here
        console.log('GML content copied to clipboard');
      }).catch(err => {
        console.error('Failed to copy GML content:', err);
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