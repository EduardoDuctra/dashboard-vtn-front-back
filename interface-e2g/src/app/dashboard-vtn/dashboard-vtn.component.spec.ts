import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardVtnComponent } from './dashboard-vtn.component';

describe('DashboardVtnComponent', () => {
  let component: DashboardVtnComponent;
  let fixture: ComponentFixture<DashboardVtnComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(DashboardVtnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should compile', () => {
    expect(component).toBeTruthy();
  });
});
