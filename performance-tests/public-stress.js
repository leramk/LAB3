import http from 'k6/http'; import { check } from 'k6';

export const options = { vus: 30,
duration: '30s',
};
check(res, {
'response time < 500ms': (r) => r.timings.duration < 500,
});
