import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { VtnDTO } from '../model/VtnDTO';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class VtnService {
  private http = inject(HttpClient);
  private url = environment.apiUrl;

  criarEvento(dados: VtnDTO) {
    return this.http.post(this.url + '/create', dados);
  }

  buscarEventos() {
    return this.http.get<VtnDTO[]>(this.url + '/events');
  }

  deletarEvento(id: string) {
    return this.http.delete<string>(this.url + '/' + id);
  }

  constructor() {}
}
