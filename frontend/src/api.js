const API = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '');

function getToken() {
  return localStorage.getItem('nexus_token');
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  }
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API}${path}`, { ...options, headers });
  } catch {
    const err = new Error('Network error — is the backend running?');
    err.status = 0;
    throw err;
  }

  if (res.status === 204) return null;
  const text = await res.text();
  let body;
  try { body = text ? JSON.parse(text) : null; } catch { body = { message: text }; }

  if (!res.ok) {
    const err = new Error(body?.message || `HTTP ${res.status}`);
    err.status = res.status;
    err.body = body;
    if (res.status === 401) {
      localStorage.removeItem('nexus_token');
      localStorage.removeItem('nexus_user');
    }
    throw err;
  }
  return body;
}

export const health = () => request('/health');
export const register = (d) => request('/auth/register', { method: 'POST', body: JSON.stringify(d) });
export const login = (d) => request('/auth/login', { method: 'POST', body: JSON.stringify(d) });
export const me = () => request('/auth/me');

export const getMessages = (platform, conversation) =>
  request(`/messages?platform=${encodeURIComponent(platform)}&conversation=${encodeURIComponent(conversation)}`);
export const sendMessage = (d) => request('/messages', { method: 'POST', body: JSON.stringify(d) });
export const deleteMessage = (id) => request(`/messages/${id}`, { method: 'DELETE' });

export const getPosts = (platform, page = 0, size = 20) =>
  request(`/posts?platform=${encodeURIComponent(platform || '')}&page=${page}&size=${size}`);
export const createPost = (d) => request('/posts', { method: 'POST', body: JSON.stringify(d) });
export const likePost = (id) => request(`/posts/${id}/like`, { method: 'POST' });
export const unlikePost = (id) => request(`/posts/${id}/like`, { method: 'DELETE' });
export const savePost = (id) => request(`/posts/${id}/save`, { method: 'POST' });
export const unsavePost = (id) => request(`/posts/${id}/save`, { method: 'DELETE' });
export const sharePost = (id) => request(`/posts/${id}/share`, { method: 'POST' });
export const deletePost = (id) => request(`/posts/${id}`, { method: 'DELETE' });
export const getSavedPosts = () => request('/posts/saved');

export const getComments = (id) => request(`/comments/post/${id}`);
export const commentPost = (id, content) =>
  request(`/comments/post/${id}`, { method: 'POST', body: JSON.stringify({ content }) });

export const getContacts = () => request('/contacts');
export const createContact = (d) => request('/contacts', { method: 'POST', body: JSON.stringify(d) });
export const deleteContact = (id) => request(`/contacts/${id}`, { method: 'DELETE' });

export const getAnalytics = () => request('/analytics');
export const askAI = (prompt, context) =>
  request('/ai/chat', { method: 'POST', body: JSON.stringify({ prompt, context }) });

export const getNotifications = () => request('/notifications');
export const markNotificationRead = (id) => request(`/notifications/${id}/read`, { method: 'PATCH' });
export const markAllNotificationsRead = () => request('/notifications/read-all', { method: 'PATCH' });

export const search = (q) => request(`/search?q=${encodeURIComponent(q)}`);
export const follow = (userId) => request(`/follows/${userId}`, { method: 'POST' });
export const unfollow = (userId) => request(`/follows/${userId}`, { method: 'DELETE' });

export async function uploadMedia(file) {
  const fd = new FormData();
  fd.append('file', file);
  return request('/media/upload', { method: 'POST', body: fd, headers: {} });
}

export function setAuth(token, user) {
  if (token) localStorage.setItem('nexus_token', token);
  if (user) localStorage.setItem('nexus_user', JSON.stringify(user));
}
export function clearAuth() {
  localStorage.removeItem('nexus_token');
  localStorage.removeItem('nexus_user');
}
export function getStoredUser() {
  try { return JSON.parse(localStorage.getItem('nexus_user') || 'null'); } catch { return null; }
}
export function getApiBase() { return API; }
