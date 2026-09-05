import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity, BarChart3, Bell, Bookmark, Bot, Camera, Check, Facebook, Heart, Home as HomeIcon,
  Image, Instagram, Menu, MessageCircle, Moon, Plus, Play, Search, Send, Settings as SettingsIcon,
  Share2, Sparkles, Sun, Trash2, UserPlus, Users, Video, X, LogOut, Paperclip
} from 'lucide-react';
import * as api from './api';
import SockJS from 'sockjs-client';
// stompjs via SockJS

// stompjs 2.x may not export Client the same way — fallback shim
function createStompClient(url, onMessage, onStatus) {
  try {
    // Prefer native SockJS + @stomp/stompjs if available; otherwise use sockjs + stompjs global pattern
    const sock = new SockJS(url);
    // stompjs 2.3 style
    const StompLib = window.Stomp || null;
    if (typeof window !== 'undefined' && !window.Stomp) {
      // dynamic require alternative via import - use simple polling fallback if STOMP fails
    }
    return { sock, connect: () => {}, disconnect: () => {}, subscribe: () => {} };
  } catch {
    return null;
  }
}

const P = {
  whatsapp: { name: 'WhatsApp', icon: MessageCircle, accent: '#25D366' },
  instagram: { name: 'Instagram', icon: Instagram, accent: '#E1306C' },
  facebook: { name: 'Facebook', icon: Facebook, accent: '#1877F2' },
  x: { name: 'X', icon: X, accent: '#F5F5F5' },
  reels: { name: 'Reels', icon: Play, accent: '#FF375F' },
};

const seedContacts = [
  { id: 'rahul', name: 'Rahul Varma', initials: 'RV', preview: 'When are we meeting?', unreadCount: 0 },
  { id: 'family', name: 'Family Group', initials: 'FG', preview: 'Dinner at 8?', unreadCount: 0 },
];

function Avatar({ text = 'A', size = 'md' }) {
  return <div className={`avatar a-${size}`}>{text}</div>;
}

function Toast({ text, close }) {
  if (!text) return null;
  return (
    <div className="toast">
      <span className="check-circle"><Check size={13} /></span>
      <span>{text}</span>
      <button type="button" onClick={close}>×</button>
    </div>
  );
}

/* ---------- Real platform connections ---------- */
function ConnectionsPage({ toast }) {
  const [connections, setConnections] = useState([]);
  const [providers, setProviders] = useState({});
  const [busy, setBusy] = useState('');
  const [waTo, setWaTo] = useState('');
  const [waText, setWaText] = useState('');
  const [xText, setXText] = useState('');
  const platforms = [
    ['whatsapp', 'WhatsApp', MessageCircle, '#25D366'],
    ['instagram', 'Instagram', Instagram, '#E1306C'],
    ['facebook', 'Facebook', Facebook, '#1877F2'],
    ['x', 'X', X, '#f5f5f5']
  ];
  const load = useCallback(async () => {
    try { setConnections(await api.getSocialConnections()); setProviders(await api.getSocialProviders()); }
    catch(e){ toast(e.message || 'Could not load connections'); }
  }, [toast]);
  useEffect(() => { load(); }, [load]);
  const connected = (p) => connections.find(x => x.platform === p)?.connected;
  async function disconnect(p){ setBusy(p); try { await api.disconnectSocial(p); await load(); toast('Disconnected '+p); } catch(e){toast(e.message)} finally{setBusy('');} }
  async function connect(p){ try { setBusy(p); const r = await api.connectSocial(p); window.location.href = r.url; } catch(e){ toast(e.message || 'Connection failed'); setBusy(''); } }
  async function sendWA(){ if(!waTo || !waText) return; setBusy('whatsapp-send'); try{await api.sendWhatsAppCloud(waTo,waText); setWaText(''); toast('WhatsApp message sent through Cloud API');}catch(e){toast(e.message)}finally{setBusy('');} }
  async function postX(){ if(!xText) return; setBusy('x-post'); try{await api.publishX(xText); setXText(''); toast('Posted to X');}catch(e){toast(e.message)}finally{setBusy('');} }
  return <div>
    <div className="page-head"><div><div className="eyebrow">INTEGRATIONS · OFFICIAL APIS</div><h2>Social Connections</h2><p>Connect your real accounts and use the official platform APIs from NEXUS.</p></div></div>
    <div className="integration-grid">
      {platforms.map(([id,name,Icon,accent]) => <div className="integration-card" key={id} style={{'--platform':accent}}>
        <div className="integration-icon"><Icon size={22}/></div><div className="integration-copy"><b>{name}</b><span>{connected(id) ? 'Connected · API access active' : 'Not connected'}</span></div>
        <span className={connected(id)?'connection-dot on':'connection-dot'} />
        {connected(id) ? <button disabled={busy===id} onClick={()=>disconnect(id)} className="integration-btn">{busy===id?'...':'Disconnect'}</button> : <button onClick={()=>connect(id)} className="integration-btn primary">Connect</button>}
      </div>)}
    </div>
    <div className="integration-grid tools-grid">
      <div className="post integration-tool"><h3>WhatsApp Cloud API</h3><p>Send a real WhatsApp Business message. Requires a connected Meta/WhatsApp Business app and approved recipient rules.</p><input value={waTo} onChange={e=>setWaTo(e.target.value)} placeholder="Recipient number, e.g. 919876543210"/><textarea value={waText} onChange={e=>setWaText(e.target.value)} placeholder="Message"/><button disabled={busy==='whatsapp-send'} onClick={sendWA} style={btnPrimary}>{busy==='whatsapp-send'?'Sending…':'Send real WhatsApp message'}</button></div>
      <div className="post integration-tool"><h3>X API v2</h3><p>Publish a real post using the connected X account. API permissions and plan limits apply.</p><textarea value={xText} onChange={e=>setXText(e.target.value)} placeholder="What do you want to post?"/><button disabled={busy==='x-post'} onClick={postX} style={btnPrimary}>{busy==='x-post'?'Publishing…':'Publish to X'}</button></div>
    </div>
    <div className="post api-note"><b>Production setup</b><p>API credentials are intentionally not stored in the frontend. Add provider credentials as Render environment variables, configure each provider's callback URL to <code>/api/social/callback/&lt;platform&gt;</code>, then connect the account. Features marked * depend on provider permissions, account type, app review and current API availability.</p></div>
  </div>;
}

/* ---------- Auth screens ---------- */
function AuthScreen({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      let res;
      if (mode === 'register') {
        res = await api.register({ username, email, password, displayName: displayName || username });
      } else {
        res = await api.login({ usernameOrEmail: username || email, password });
      }
      api.setAuth(res.token, { id: res.userId, username: res.username, displayName: res.displayName, role: res.role });
      onAuth(res);
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="nexus" style={{ justifyContent: 'center', alignItems: 'center', minHeight: '100vh', padding: 24 }}>
      <div style={{
        width: '100%', maxWidth: 420, background: '#0c0e0f', border: '1px solid #ffffff12',
        borderRadius: 20, padding: 32, boxShadow: '0 20px 60px #0008'
      }}>
        <div className="brand" style={{ marginBottom: 28, justifyContent: 'center' }}>
          <div className="nexus-logo"><span>N</span><i /></div>
          <div><b>NEXUS</b><small>Social Intelligence Workspace</small></div>
        </div>
        <h2 style={{ font: "700 22px 'Space Grotesk'", margin: '0 0 8px' }}>{mode === 'login' ? 'Sign in' : 'Create account'}</h2>
        <p style={{ color: '#727a78', fontSize: 12, marginBottom: 20 }}>
          {mode === 'login' ? 'Access your unified social command center.' : 'Join NEXUS — real social workspace with optional official platform connections.'}
        </p>
        <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {mode === 'register' && (
            <>
              <input className="form-input" placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} required minLength={3} style={inputStyle} />
              <input className="form-input" placeholder="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} required style={inputStyle} />
              <input className="form-input" placeholder="Display name" value={displayName} onChange={e => setDisplayName(e.target.value)} style={inputStyle} />
            </>
          )}
          {mode === 'login' && (
            <input className="form-input" placeholder="Username or email" value={username} onChange={e => setUsername(e.target.value)} required style={inputStyle} />
          )}
          <input className="form-input" placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={8} style={inputStyle} />
          {error && <div style={{ color: '#f87171', fontSize: 12 }}>{error}</div>}
          <button type="submit" disabled={busy} style={{
            background: 'var(--accent, #45e39a)', color: '#05140e', border: 0, borderRadius: 12,
            padding: '12px 16px', fontWeight: 700, cursor: busy ? 'wait' : 'pointer'
          }}>
            {busy ? 'Please wait…' : (mode === 'login' ? 'Sign in' : 'Create account')}
          </button>
        </form>
        <p style={{ marginTop: 16, fontSize: 12, color: '#727a78', textAlign: 'center' }}>
          {mode === 'login' ? (
            <>No account? <button type="button" onClick={() => { setMode('register'); setError(''); }} style={{ background: 'none', border: 0, color: '#45e39a', cursor: 'pointer' }}>Register</button></>
          ) : (
            <>Have an account? <button type="button" onClick={() => { setMode('login'); setError(''); }} style={{ background: 'none', border: 0, color: '#45e39a', cursor: 'pointer' }}>Sign in</button></>
          )}
        </p>
        <p style={{ marginTop: 8, fontSize: 10, color: '#4f5655', textAlign: 'center' }}>Demo: demo / demo12345</p>
      </div>
    </div>
  );
}

const inputStyle = {
  background: '#ffffff08', border: '1px solid #ffffff14', borderRadius: 10, padding: '11px 14px',
  color: '#eef2f1', outline: 'none'
};

/* ---------- External social-event WebSocket ---------- */
function useSocialEventSocket(enabled, userId, onEvent) {
  useEffect(() => {
    if (!enabled || !userId) return undefined;
    let dead = false;
    let client = null;
    const base = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '');
    const sock = new SockJS(`${base}/ws`);
    import('stompjs').then((StompMod) => {
      if (dead) return;
      const Stomp = StompMod.default || StompMod;
      client = Stomp.over(sock);
      client.debug = () => {};
      client.connect({}, () => {
        if (dead) return;
        client.subscribe(`/topic/social/${userId}`, (frame) => {
          try { onEvent?.(JSON.parse(frame.body)); } catch {}
        });
      }, () => {});
    }).catch(() => {});
    return () => {
      dead = true;
      try { client?.disconnect(() => {}); } catch {}
      try { sock.close(); } catch {}
    };
  }, [enabled, userId, onEvent]);
}

/* ---------- WebSocket helper ---------- */
function useNexusSocket(enabled, platform, conversationId, onMessage) {
  const [status, setStatus] = useState('disconnected');
  const clientRef = useRef(null);
  const backoffRef = useRef(1000);

  useEffect(() => {
    if (!enabled || !conversationId) return undefined;
    let dead = false;
    let timer;

    function connect() {
      if (dead) return;
      setStatus('connecting');
      try {
        const base = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '');
        const sock = new SockJS(`${base}/ws`);
        // stompjs 2.3
        import('stompjs').then((StompMod) => {
          if (dead) return;
          const Stomp = StompMod.default || StompMod;
          const client = Stomp.over(sock);
          client.debug = () => {};
          client.connect({}, () => {
            if (dead) return;
            setStatus('connected');
            backoffRef.current = 1000;
            client.subscribe(`/topic/messages/${platform}/${conversationId}`, (frame) => {
              try {
                const body = JSON.parse(frame.body);
                onMessage?.(body);
              } catch { /* ignore */ }
            });
            clientRef.current = client;
          }, () => {
            setStatus('error');
            scheduleReconnect();
          });
        }).catch(() => {
          setStatus('unavailable');
        });
      } catch {
        setStatus('unavailable');
        scheduleReconnect();
      }
    }

    function scheduleReconnect() {
      if (dead) return;
      const delay = Math.min(backoffRef.current, 15000);
      backoffRef.current = Math.min(delay * 2, 15000);
      timer = setTimeout(connect, delay);
    }

    connect();
    return () => {
      dead = true;
      clearTimeout(timer);
      try { clientRef.current?.disconnect?.(); } catch { /* */ }
      clientRef.current = null;
    };
  }, [enabled, platform, conversationId, onMessage]);

  return status;
}

/* ---------- Camera modal ---------- */
function CameraModal({ onCapture, onClose }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState(null);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        if (!navigator.mediaDevices?.getUserMedia) {
          setError('Camera not supported in this browser');
          return;
        }
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false });
        if (!active) {
          stream.getTracks().forEach(t => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
      } catch (e) {
        if (e.name === 'NotAllowedError') setError('Camera permission denied');
        else if (e.name === 'NotFoundError') setError('No camera available');
        else setError(e.message || 'Could not open camera');
      }
    })();
    return () => {
      active = false;
      streamRef.current?.getTracks().forEach(t => t.stop());
    };
  }, []);

  function capture() {
    const video = videoRef.current;
    if (!video) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    canvas.getContext('2d').drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      if (blob) setPreview(URL.createObjectURL(blob));
      canvas.toBlob((b) => {
        if (b) window.__nexusCaptureBlob = b;
      }, 'image/jpeg', 0.92);
    }, 'image/jpeg', 0.92);
  }

  function usePhoto() {
    const blob = window.__nexusCaptureBlob;
    if (!blob) return;
    const file = new File([blob], `capture-${Date.now()}.jpg`, { type: 'image/jpeg' });
    onCapture(file);
    onClose();
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: '#000c', zIndex: 100, display: 'grid', placeItems: 'center', padding: 16
    }}>
      <div style={{ background: '#111', borderRadius: 16, padding: 16, maxWidth: 480, width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
          <b>Camera</b>
          <button type="button" className="icon-btn" onClick={onClose}><X size={16} /></button>
        </div>
        {error && <p style={{ color: '#f87171', fontSize: 12 }}>{error}</p>}
        {!preview ? (
          <video ref={videoRef} playsInline muted style={{ width: '100%', borderRadius: 12, background: '#000' }} />
        ) : (
          <img src={preview} alt="capture" style={{ width: '100%', borderRadius: 12 }} />
        )}
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          {!preview && !error && (
            <button type="button" onClick={capture} style={btnPrimary}>Capture</button>
          )}
          {preview && (
            <>
              <button type="button" onClick={() => { setPreview(null); window.__nexusCaptureBlob = null; }} style={btnGhost}>Retake</button>
              <button type="button" onClick={usePhoto} style={btnPrimary}>Use photo</button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

const btnPrimary = { background: '#45e39a', color: '#05140e', border: 0, borderRadius: 10, padding: '10px 14px', fontWeight: 700, cursor: 'pointer' };
const btnGhost = { background: '#ffffff10', color: '#fff', border: '1px solid #ffffff20', borderRadius: 10, padding: '10px 14px', cursor: 'pointer' };

/* ---------- Main App ---------- */
export default function App() {
  const [user, setUser] = useState(api.getStoredUser());
  const [authChecked, setAuthChecked] = useState(false);
  const [platform, setPlatform] = useState('whatsapp');
  const [page, setPage] = useState('home');
  const [theme, setTheme] = useState(localStorage.getItem('nexus-theme') || 'dark');
  const [contacts, setContacts] = useState(seedContacts);
  const [conversation, setConversation] = useState('Rahul Varma');
  const [conversationId, setConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [posts, setPosts] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [draft, setDraft] = useState('');
  const [postDraft, setPostDraft] = useState('');
  const [aiDraft, setAiDraft] = useState('Summarize my workspace activity.');
  const [aiReply, setAiReply] = useState('');
  const [searchQ, setSearchQ] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [toast, setToast] = useState('');
  const [busy, setBusy] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [notifications, setNotifications] = useState({ items: [], unreadCount: 0 });
  const [showNotif, setShowNotif] = useState(false);
  const [showCamera, setShowCamera] = useState(false);
  const [mediaPreview, setMediaPreview] = useState(null);
  const [wsStatus, setWsStatus] = useState('disconnected');
  const endRef = useRef(null);
  const brand = P[platform] || P.whatsapp;
  const C = brand.icon;

  const context = useMemo(
    () => JSON.stringify({ platform, conversation, messages: messages.slice(-10), posts: posts.slice(0, 10) }),
    [platform, conversation, messages, posts]
  );

  // Auth bootstrap
  useEffect(() => {
    (async () => {
      const token = localStorage.getItem('nexus_token');
      if (!token) {
        setAuthChecked(true);
        return;
      }
      try {
        const me = await api.me();
        setUser(me);
        api.setAuth(token, me);
      } catch {
        api.clearAuth();
        setUser(null);
      } finally {
        setAuthChecked(true);
      }
    })();
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('nexus-theme', theme);
    document.documentElement.style.setProperty('--accent', brand.accent);
  }, [theme, brand.accent]);

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(''), 3200);
      return () => clearTimeout(t);
    }
  }, [toast]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Load page data
  useEffect(() => {
    if (!user) return;
    let dead = false;
    (async () => {
      try {
        if (page === 'contacts') {
          const x = await api.getContacts();
          if (!dead && x?.length) setContacts(x);
        } else if (page === 'analytics') {
          setAnalytics(await api.getAnalytics());
        } else if (page === 'messages' || platform === 'whatsapp') {
          const x = await api.getMessages('whatsapp', conversation);
          if (!dead) {
            setMessages(x || []);
            if (x?.[0]?.conversationId) setConversationId(x[0].conversationId);
          }
        } else if (['feeds', 'reels'].includes(page) || ['instagram', 'facebook', 'x', 'reels'].includes(platform)) {
          const pageData = await api.getPosts(platform === 'reels' ? 'reels' : platform);
          const list = pageData?.content || pageData || [];
          if (!dead) setPosts(Array.isArray(list) ? list : []);
        }
        if (page === 'home' || showNotif) {
          try {
            const n = await api.getNotifications();
            if (!dead) setNotifications(n);
          } catch { /* optional */ }
        }
      } catch (e) {
        if (!dead) setToast(e.message || 'Load failed');
      }
    })();
    return () => { dead = true; };
  }, [page, platform, conversation, user, showNotif]);

  // WebSocket
  const onWsMessage = useCallback((msg) => {
    setMessages((prev) => {
      if (prev.some((m) => m.id === msg.id)) return prev;
      return [...prev, msg];
    });
  }, []);

  const socketStatus = useNexusSocket(
    !!user && (page === 'messages' || platform === 'whatsapp') && !!conversationId,
    'whatsapp',
    conversationId,
    onWsMessage
  );
  useEffect(() => { setWsStatus(socketStatus); }, [socketStatus]);
  const onSocialEvent = useCallback((event) => {
    const platformName = event?.provider === 'meta' ? 'Meta' : (event?.provider || 'Social');
    setToast(`${platformName} event received in real time`);
  }, []);
  useSocialEventSocket(!!user, user?.id, onSocialEvent);

  function go(p, pg) {
    setPlatform(p);
    setPage(pg);
    setMobile(false);
  }

  async function handleLogout() {
    api.clearAuth();
    setUser(null);
    setMessages([]);
    setPosts([]);
  }

  async function send() {
    const content = draft.trim();
    if (!content) return;
    setBusy(true);
    try {
      const m = await api.sendMessage({ platform: 'whatsapp', conversation, content });
      setMessages((v) => (v.some((x) => x.id === m.id) ? v : [...v, m]));
      if (m.conversationId) setConversationId(m.conversationId);
      setDraft('');
      setToast('Message sent');
    } catch (e) {
      setToast(e.message || 'Send failed');
    } finally {
      setBusy(false);
    }
  }

  async function publish(mediaUrl, mediaType) {
    const content = postDraft.trim();
    if (!content && !mediaUrl) return;
    setBusy(true);
    try {
      const p = await api.createPost({
        platform,
        content: content || (mediaType === 'VIDEO' ? 'New reel' : 'New post'),
        mediaUrl,
        mediaType,
      });
      setPosts((v) => [p, ...v]);
      setPostDraft('');
      setMediaPreview(null);
      setToast(`${brand.name} post published`);
    } catch (e) {
      setToast(e.message || 'Publish failed');
    } finally {
      setBusy(false);
    }
  }

  async function onFileSelected(file) {
    if (!file) return;
    setBusy(true);
    try {
      const up = await api.uploadMedia(file);
      setMediaPreview(up);
      setToast('Media uploaded');
      return up;
    } catch (e) {
      setToast(e.message || 'Upload failed');
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function like(id, liked) {
    try {
      const p = liked ? await api.unlikePost(id) : await api.likePost(id);
      setPosts((v) => v.map((x) => (x.id === id ? p : x)));
    } catch (e) {
      setToast(e.message || 'Like failed');
    }
  }

  async function toggleSave(id, saved) {
    try {
      const p = saved ? await api.unsavePost(id) : await api.savePost(id);
      setPosts((v) => v.map((x) => (x.id === id ? p : x)));
      setToast(saved ? 'Removed bookmark' : 'Saved');
    } catch (e) {
      setToast(e.message || 'Save failed');
    }
  }

  async function share(id) {
    try {
      const p = await api.sharePost(id);
      setPosts((v) => v.map((x) => (x.id === id ? p : x)));
      setToast('Shared');
    } catch (e) {
      setToast(e.message || 'Share failed');
    }
  }

  async function removePost(id) {
    try {
      await api.deletePost(id);
      setPosts((v) => v.filter((x) => x.id !== id));
      setToast('Post deleted');
    } catch (e) {
      setToast(e.message || 'Delete failed');
    }
  }

  async function addComment(id, content) {
    if (!content?.trim()) return;
    try {
      await api.commentPost(id, content.trim());
      const p = posts.find((x) => x.id === id);
      if (p) setPosts((v) => v.map((x) => (x.id === id ? { ...x, commentCount: (x.commentCount || 0) + 1 } : x)));
      setToast('Comment added');
    } catch (e) {
      setToast(e.message || 'Comment failed');
    }
  }

  async function runAI() {
    setBusy(true);
    setAiReply('');
    try {
      const r = await api.askAI(aiDraft, context);
      setAiReply((r.source === 'fallback' ? '[Fallback] ' : '') + (r.reply || ''));
    } catch (e) {
      setAiReply('AI error: ' + (e.message || 'failed'));
    } finally {
      setBusy(false);
    }
  }

  async function doSearch(q) {
    setSearchQ(q);
    if (!q.trim()) {
      setSearchResults(null);
      return;
    }
    try {
      setSearchResults(await api.search(q));
    } catch (e) {
      setToast(e.message);
    }
  }

  if (!authChecked) {
    return <div className="nexus" style={{ placeItems: 'center', display: 'grid' }}><p style={{ color: '#727a78' }}>Loading NEXUS…</p></div>;
  }
  if (!user) {
    return <AuthScreen onAuth={(res) => setUser({ id: res.userId, username: res.username, displayName: res.displayName, role: res.role })} />;
  }

  const nav = (key, label, icon, p, pg) => (
    <button type="button" className={`nav ${page === pg && platform === p ? 'active' : ''}`} onClick={() => go(p, pg)}>
      {icon} <span>{label}</span>
    </button>
  );

  return (
    <div className="nexus" style={{ '--accent': brand.accent }}>
      <aside className={`sidebar ${mobile ? 'open' : ''}`}>
        <div className="brand">
          <div className="nexus-logo"><span>N</span><i /></div>
          <div><b>NEXUS</b><small>Social Intelligence</small></div>
        </div>
        <div className="eyebrow">WORKSPACE</div>
        {nav('home', 'Overview', <HomeIcon size={16} />, 'whatsapp', 'home')}
        {nav('wa', 'WhatsApp', <MessageCircle size={16} />, 'whatsapp', 'messages')}
        {nav('ig', 'Instagram', <Instagram size={16} />, 'instagram', 'feeds')}
        {nav('fb', 'Facebook', <Facebook size={16} />, 'facebook', 'feeds')}
        {nav('x', 'X', <X size={16} />, 'x', 'feeds')}
        {nav('reels', 'Reels', <Play size={16} />, 'reels', 'reels')}
        {nav('ai', 'AI Agent', <Bot size={16} />, 'whatsapp', 'ai')}
        {nav('contacts', 'Contacts', <Users size={16} />, 'whatsapp', 'contacts')}
        {nav('analytics', 'Analytics', <BarChart3 size={16} />, 'whatsapp', 'analytics')}
        {nav('connections', 'Connections', <Activity size={16} />, 'whatsapp', 'connections')}
        {nav('settings', 'Settings', <SettingsIcon size={16} />, 'whatsapp', 'settings')}
        <div className="side-bottom">
          <div className="profile">
            <Avatar text={(user.displayName || user.username || 'U').slice(0, 2).toUpperCase()} />
            <div>
              <b>{user.displayName || user.username}</b>
              <small>@{user.username}</small>
            </div>
            <button type="button" className="icon-btn" title="Logout" onClick={handleLogout}><LogOut size={14} /></button>
          </div>
          <div className="version">NEXUS v2.0 · local workspace</div>
        </div>
      </aside>

      <main className="main">
        <div className="top">
          <button type="button" className="icon-btn" onClick={() => setMobile((m) => !m)}><Menu size={18} /></button>
          <div className="title">
            <div className="eyebrow">{brand.name.toUpperCase()} · <span className="live" /> LIVE</div>
            <h1>{page === 'home' ? 'Command Center' : brand.name}</h1>
            <p>Unified social intelligence workspace</p>
          </div>
          <div className="top-right" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <div className="search">
              <Search size={14} />
              <input placeholder="Search NEXUS…" value={searchQ} onChange={(e) => doSearch(e.target.value)} />
            </div>
            <button type="button" className="icon-btn" onClick={() => setShowNotif((v) => !v)} title="Notifications">
              <Bell size={16} />
              {notifications.unreadCount > 0 && <span style={{ fontSize: 9, color: '#45e39a' }}>{notifications.unreadCount}</span>}
            </button>
            <button type="button" className="icon-btn" onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}>
              {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
            </button>
            <Avatar text={(user.displayName || 'U').slice(0, 1)} size="sm" />
          </div>
        </div>

        {searchResults && (
          <div className="post" style={{ marginBottom: 16 }}>
            <b>Search results</b>
            <pre style={{ fontSize: 11, whiteSpace: 'pre-wrap' }}>{JSON.stringify(searchResults, null, 2)}</pre>
            <button type="button" onClick={() => setSearchResults(null)} style={btnGhost}>Close</button>
          </div>
        )}

        {showNotif && (
          <div className="post" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <b>Notifications</b>
              <button type="button" style={btnGhost} onClick={async () => { await api.markAllNotificationsRead(); setNotifications((n) => ({ ...n, unreadCount: 0, items: n.items.map((i) => ({ ...i, read: true })) })); }}>Mark all read</button>
            </div>
            {(notifications.items || []).length === 0 && <p style={{ color: '#727a78', fontSize: 12 }}>No notifications</p>}
            {(notifications.items || []).map((n) => (
              <div key={n.id} style={{ padding: '8px 0', borderBottom: '1px solid #ffffff08', fontSize: 12, opacity: n.read ? 0.6 : 1 }}>
                {n.message}
              </div>
            ))}
          </div>
        )}

        {page === 'home' && (
          <div className="home">
            <div className="hero">
              <h2>One workspace.<br />Every conversation.</h2>
              <p>NEXUS unifies messaging, feeds, reels and AI — backed by your own database with optional official platform integrations.</p>
              <div className="platform-grid">
                {Object.entries(P).map(([k, v]) => {
                  const Icon = v.icon;
                  return (
                    <button type="button" key={k} className="platform-card" onClick={() => go(k, k === 'whatsapp' ? 'messages' : k === 'reels' ? 'reels' : 'feeds')} style={{ '--accent': v.accent }}>
                      <Icon size={20} />
                      <span>{v.name}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {(page === 'messages' || (platform === 'whatsapp' && page !== 'home' && page !== 'ai' && page !== 'contacts' && page !== 'analytics' && page !== 'settings')) && page === 'messages' && (
          <div className="chat">
            <div className="chat-list">
              <div className="chat-search"><Search size={14} /><input placeholder="Search chats" /></div>
              {contacts.map((c) => (
                <button type="button" key={c.id || c.name} className={`chat-item ${conversation === c.name ? 'active' : ''}`} onClick={() => setConversation(c.name)}>
                  <Avatar text={c.initials || c.name?.slice(0, 2)} />
                  <div><b>{c.name}</b><small>{c.preview || ''}</small></div>
                </button>
              ))}
            </div>
            <div className="conversation">
              <div className="conv-head">
                <Avatar text={conversation.slice(0, 2)} />
                <div><b>{conversation}</b><small>WS: {wsStatus}</small></div>
              </div>
              <div className="messages">
                {messages.length === 0 && <p style={{ color: '#727a78', fontSize: 12, textAlign: 'center' }}>No messages yet</p>}
                {messages.map((m) => (
                  <div key={m.id} className={`msg ${m.mine ? 'mine' : ''}`}>
                    <div className="bubble">{m.content}</div>
                    <small>{m.senderName} · {m.createdAt ? new Date(m.createdAt).toLocaleTimeString() : ''}</small>
                  </div>
                ))}
                <div ref={endRef} />
              </div>
              <div className="composer">
                <input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Type a message" onKeyDown={(e) => e.key === 'Enter' && !busy && send()} />
                <button type="button" disabled={busy} onClick={send}><Send size={16} /></button>
              </div>
            </div>
          </div>
        )}

        {(['feeds', 'reels'].includes(page)) && (
          <div className="feed-layout">
            <div>
              <div className="composer-post">
                <textarea value={postDraft} onChange={(e) => setPostDraft(e.target.value)} placeholder={`Share on ${brand.name}…`} />
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                  <label className="media-btn" style={{ cursor: 'pointer' }}>
                    <Image size={14} /> Media
                    <input type="file" accept="image/*,video/*" hidden onChange={async (e) => {
                      const f = e.target.files?.[0];
                      if (f) await onFileSelected(f);
                    }} />
                  </label>
                  <button type="button" className="media-btn" onClick={() => setShowCamera(true)}><Camera size={14} /> Camera</button>
                  {mediaPreview && <span className="file-chip">{mediaPreview.mediaType}: uploaded</span>}
                  <button type="button" disabled={busy} onClick={() => publish(mediaPreview?.mediaUrl, mediaPreview?.mediaType)} style={{ marginLeft: 'auto', ...btnPrimary }}>
                    {busy ? 'Publishing…' : 'Publish'}
                  </button>
                </div>
              </div>
              {posts.length === 0 && <p style={{ color: '#727a78', fontSize: 12 }}>No posts yet — publish the first one.</p>}
              {posts.map((p) => (
                <article key={p.id} className="post">
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <b>{p.author || 'User'}</b>
                    <small>{p.platform}</small>
                  </div>
                  <p>{p.content}</p>
                  {p.mediaUrl && (p.mediaType === 'VIDEO'
                    ? <video src={p.mediaUrl.startsWith('http') ? p.mediaUrl : `${api.getApiBase().replace(/\/api$/, '')}${p.mediaUrl}`} controls style={{ maxWidth: '100%', borderRadius: 12 }} />
                    : <img src={p.mediaUrl.startsWith('http') ? p.mediaUrl : `${api.getApiBase().replace(/\/api$/, '')}${p.mediaUrl}`} alt="" style={{ maxWidth: '100%', borderRadius: 12 }} />
                  )}
                  <div className="post-actions">
                    <button type="button" onClick={() => like(p.id, p.likedByMe)}><Heart size={14} /> {p.likeCount || 0}</button>
                    <button type="button" onClick={() => {
                      const c = window.prompt('Comment');
                      if (c) addComment(p.id, c);
                    }}><MessageCircle size={14} /> {p.commentCount || 0}</button>
                    <button type="button" onClick={() => share(p.id)}><Share2 size={14} /> {p.shareCount || 0}</button>
                    <button type="button" onClick={() => toggleSave(p.id, p.savedByMe)}><Bookmark size={14} /> {p.saveCount || 0}</button>
                    {p.userId === user.id && (
                      <button type="button" onClick={() => removePost(p.id)}><Trash2 size={14} /></button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </div>
        )}

        {page === 'ai' && (
          <div className="ai-grid">
            <div className="post">
              <h3><Sparkles size={16} /> AI Agent</h3>
              <p style={{ fontSize: 11, color: '#727a78' }}>OpenAI-compatible (Groq by default). Keys stay server-side.</p>
              <textarea className="large" value={aiDraft} onChange={(e) => setAiDraft(e.target.value)} />
              <button type="button" disabled={busy} onClick={runAI} style={btnPrimary}>{busy ? 'Thinking…' : 'Ask NEXUS AI'}</button>
              {aiReply && <pre style={{ marginTop: 12, whiteSpace: 'pre-wrap', fontSize: 13 }}>{aiReply}</pre>}
            </div>
          </div>
        )}

        {page === 'contacts' && (
          <div className="contact-grid">
            {contacts.length === 0 && <p style={{ color: '#727a78' }}>No contacts</p>}
            {contacts.map((c) => (
              <div key={c.id || c.name} className="post">
                <Avatar text={c.initials || c.name?.slice(0, 2)} />
                <b>{c.name}</b>
                <small>{c.preview}</small>
              </div>
            ))}
          </div>
        )}

        {page === 'analytics' && (
          <div className="metrics">
            {!analytics && <p style={{ color: '#727a78' }}>Loading analytics…</p>}
            {analytics && Object.entries(analytics).filter(([, v]) => typeof v === 'number').map(([k, v]) => (
              <div key={k} className="metric"><b>{v}</b><span>{k}</span></div>
            ))}
          </div>
        )}

        {page === 'connections' && <ConnectionsPage toast={setToast} />}

        {page === 'settings' && (
          <div className="post">
            <h3>Settings</h3>
            <p style={{ fontSize: 12, color: '#727a78' }}>Theme, account, and deployment notes.</p>
            <button type="button" style={btnGhost} onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}>Toggle theme</button>
            <button type="button" style={{ ...btnGhost, marginLeft: 8 }} onClick={handleLogout}>Logout</button>
            <p style={{ marginTop: 16, fontSize: 11, color: '#4f5655' }}>
              Official social integrations are available under Connections. Provider permissions, OAuth setup, account eligibility and API limits determine which real actions are available.
            </p>
          </div>
        )}
      </main>

      <Toast text={toast} close={() => setToast('')} />
      {showCamera && (
        <CameraModal
          onClose={() => setShowCamera(false)}
          onCapture={async (file) => {
            const up = await onFileSelected(file);
            if (up) setToast('Captured image uploaded');
          }}
        />
      )}
    </div>
  );
}
