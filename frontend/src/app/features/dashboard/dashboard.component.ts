import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'sa-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  connected = false;
  checked = false;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.getHealth().subscribe({
      next: (health) => {
        this.connected = health.status === 'UP';
        this.checked = true;
      },
      error: () => {
        this.connected = false;
        this.checked = true;
      },
    });
  }
}
