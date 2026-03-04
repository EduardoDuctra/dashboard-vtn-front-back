import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraficoHistoricoComponent } from './grafico-historico.component';

describe('GraficoHistoricoComponent', () => {
  let component: GraficoHistoricoComponent;
  let fixture: ComponentFixture<GraficoHistoricoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraficoHistoricoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GraficoHistoricoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
