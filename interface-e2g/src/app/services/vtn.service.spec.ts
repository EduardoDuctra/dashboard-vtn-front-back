import { TestBed } from '@angular/core/testing';

import { VtnService } from './vtn.service';

describe('VtnService', () => {
  let service: VtnService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(VtnService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
