// ===================================================================
// app.js — shared helpers used by every page:
//   - API_BASE_URL: where the Spring Boot backend is running
//   - apiGet / apiPost / apiPut / apiDelete: fetch wrappers that
//     automatically attach the JWT and parse error responses
//   - saveSession / getSession / logout: simple session handling
//   - requireRole: route guard for protected pages
// ===================================================================

const API_BASE_URL = 'http://localhost:8080';

function getSession() {
  const raw = localStorage.getItem('quizapp_session');
  return raw ? JSON.parse(raw) : null;
}

function saveSession(authResponse) {
  localStorage.setItem('quizapp_session', JSON.stringify(authResponse));
}

function logout() {
  localStorage.removeItem('quizapp_session');
  window.location.href = 'login.html';
}

/** Redirects to login if not authenticated, or to the correct dashboard if the role doesn't match. */
function requireRole(role) {
  const session = getSession();
  if (!session || !session.token) {
    window.location.href = 'login.html';
    return;
  }
  if (session.role !== role) {
    window.location.href = session.role === 'ADMIN' ? 'admin.html' : 'dashboard.html';
  }
}

async function request(method, path, body, authRequired = true) {
  const headers = { 'Content-Type': 'application/json' };

  if (authRequired) {
    const session = getSession();
    if (session && session.token) {
      headers['Authorization'] = `Bearer ${session.token}`;
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });

  if (response.status === 401) {
    logout();
    throw new Error('Session expired. Please log in again.');
  }

  const isJson = response.headers.get('content-type')?.includes('application/json');
  const data = isJson ? await response.json() : null;

  if (!response.ok) {
    const message = data?.message || `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return data;
}

const apiGet = (path) => request('GET', path);
const apiPost = (path, body, authRequired = true) => request('POST', path, body, authRequired);
const apiPut = (path, body) => request('PUT', path, body);
const apiDelete = (path) => request('DELETE', path);

/** Basic HTML-escaping to avoid rendering unsafe content pulled from the API. */
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
