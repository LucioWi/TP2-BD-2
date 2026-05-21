import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || 'Error de conexión con el servidor';
    console.error('[API Error]', message);
    return Promise.reject(error);
  }
);

export default apiClient;
