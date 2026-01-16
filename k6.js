import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:17272/lab3/api';
const TOTAL_MARINES = 15;

export const options = {
    vus: 50,
    duration: '20s',
};

export default function () {
    const id = Math.floor(Math.random() * TOTAL_MARINES) + 1;
    const url = `${BASE_URL}/dragons/${id}`;

    const res = http.get(url);

    check(res, {
        'status 200': (r) => r.status === 200,
    });

    sleep(Math.random() * 0.1);
}
