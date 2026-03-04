import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabelaAgendamentosComponent } from './tabela-agendamentos.component';

describe('TabelaAgendamentosComponent', () => {
  let component: TabelaAgendamentosComponent;
  let fixture: ComponentFixture<TabelaAgendamentosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TabelaAgendamentosComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TabelaAgendamentosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
