import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalAgendamentoComponent } from './modal-agendamento.component';

describe('ModalAgendamentoComponent', () => {
  let component: ModalAgendamentoComponent;
  let fixture: ComponentFixture<ModalAgendamentoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalAgendamentoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModalAgendamentoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
