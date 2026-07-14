import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'sa-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  connected = signal(false);
  checked = signal(false);

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.getHealth().subscribe({
      next: (health) => {
        this.connected.set(health.status === 'UP');
        this.checked.set(true);
      },
      error: () => {
        this.connected.set(false);
        this.checked.set(true);
      },
    });
  }
}