import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 30,
  duration: '30s',
};

export default function () {
  const res = http.get('http://localhost:8080/');
  check(res, {
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
