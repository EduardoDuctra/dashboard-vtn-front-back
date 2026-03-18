import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { EventoDTO } from '../model/EventoDTO';
import { environment } from '../../environments/environment';
import { EventoUnificado } from '../model/EventoUnificado';

@Injectable({
  providedIn: 'root',
})
export class VtnService {
  private http = inject(HttpClient);
  private url = environment.apiUrl;

  criarEvento(dados: EventoDTO) {
    return this.http.post(this.url + '/create', dados);
  }

  buscarEventos() {
    return this.http.get<EventoUnificado[]>(this.url + '/events-complete');
  }

  deletarEvento(id: string) {
    return this.http.delete<string>(this.url + '/events/' + id);
  }

  constructor() {}
}
