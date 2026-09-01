const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

function gatewayUrl() {
  return $('#gatewayUrl').value.replace(/\/$/, '');
}

async function api(path, options = {}) {
  const res = await fetch(gatewayUrl() + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status}: ${body}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

// ---------- Tab navigation ----------
$$('.nav-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    $$('.nav-btn').forEach((b) => b.classList.remove('active'));
    $$('.tab').forEach((t) => t.classList.remove('active'));
    btn.classList.add('active');
    $(`#tab-${btn.dataset.tab}`).classList.add('active');
    refreshTab(btn.dataset.tab);
  });
});

function refreshTab(tab) {
  if (tab === 'conversations') loadConversations();
  if (tab === 'projects') loadProjects();
  if (tab === 'permissions') { loadPermissions(); loadProjectOptions(); }
  if (tab === 'activity') loadActivity();
  if (tab === 'models') loadModels();
  if (tab === 'chat') { loadProjectOptions(); loadModelOptions(); }
}

// ---------- Chat ----------
let currentConversationId = null;

function appendMessage(role, content) {
  const log = $('#chatLog');
  const div = document.createElement('div');
  div.className = `msg ${role}`;
  div.innerHTML = `<div class="role">${role}</div><div class="bubble"></div>`;
  div.querySelector('.bubble').textContent = content;
  log.appendChild(div);
  log.scrollTop = log.scrollHeight;
}

$('#newChatBtn').addEventListener('click', () => {
  currentConversationId = null;
  $('#chatLog').innerHTML = '';
});

$('#chatForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = $('#chatInput');
  const message = input.value.trim();
  if (!message) return;
  appendMessage('user', message);
  input.value = '';

  try {
    const payload = {
      conversationId: currentConversationId,
      projectId: $('#chatProject').value || null,
      model: $('#chatModel').value || null,
      message,
    };
    const result = await api('/api/chat', { method: 'POST', body: JSON.stringify(payload) });
    currentConversationId = result.conversationId;
    appendMessage('assistant', result.reply);
  } catch (err) {
    appendMessage('assistant', `Error: ${err.message}`);
  }
});

async function loadProjectOptions() {
  try {
    const projects = await api('/api/projects');
    for (const sel of [$('#chatProject'), $('#permProject')]) {
      const keep = sel.value;
      sel.innerHTML = sel.id === 'chatProject'
        ? '<option value="">No project</option>'
        : '<option value="">Global (all projects)</option>';
      projects.forEach((p) => {
        const opt = document.createElement('option');
        opt.value = p.id;
        opt.textContent = p.name;
        sel.appendChild(opt);
      });
      sel.value = keep;
    }
  } catch (err) { console.error(err); }
}

async function loadModelOptions() {
  try {
    const { models } = await api('/api/models');
    const sel = $('#chatModel');
    sel.innerHTML = '<option value="">Default model</option>';
    models.forEach((m) => {
      const opt = document.createElement('option');
      opt.value = m;
      opt.textContent = m;
      sel.appendChild(opt);
    });
  } catch (err) { console.error(err); }
}

// ---------- Conversations ----------
async function loadConversations() {
  const list = $('#conversationList');
  list.innerHTML = 'Loading…';
  try {
    const conversations = await api('/api/conversations');
    list.innerHTML = '';
    conversations.forEach((c) => {
      const div = document.createElement('div');
      div.className = 'card';
      div.innerHTML = `
        <div>
          <div>${escapeHtml(c.title)}</div>
          <div class="meta">${c.model} · ${new Date(c.updatedAt).toLocaleString()}</div>
        </div>
        <div>
          <button data-open="${c.id}">Open</button>
          <button data-delete="${c.id}">Delete</button>
        </div>`;
      list.appendChild(div);
    });
    list.querySelectorAll('[data-open]').forEach((btn) =>
      btn.addEventListener('click', () => openConversation(btn.dataset.open)));
    list.querySelectorAll('[data-delete]').forEach((btn) =>
      btn.addEventListener('click', async () => {
        await api(`/api/conversations/${btn.dataset.delete}`, { method: 'DELETE' });
        loadConversations();
      }));
  } catch (err) {
    list.innerHTML = `Error: ${err.message}`;
  }
}

async function openConversation(id) {
  const data = await api(`/api/conversations/${id}`);
  currentConversationId = id;
  $('#chatLog').innerHTML = '';
  data.messages.forEach((m) => appendMessage(m.role, m.content));
  $$('.nav-btn').forEach((b) => b.classList.remove('active'));
  $$('.tab').forEach((t) => t.classList.remove('active'));
  $('.nav-btn[data-tab="chat"]').classList.add('active');
  $('#tab-chat').classList.add('active');
}

// ---------- Projects ----------
$('#projectForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  await api('/api/projects', {
    method: 'POST',
    body: JSON.stringify({
      name: $('#projectName').value,
      defaultModel: $('#projectModel').value || null,
      workspacePath: $('#projectWorkspace').value || null,
    }),
  });
  $('#projectForm').reset();
  loadProjects();
});

async function loadProjects() {
  const list = $('#projectList');
  list.innerHTML = 'Loading…';
  try {
    const projects = await api('/api/projects');
    list.innerHTML = '';
    projects.forEach((p) => {
      const div = document.createElement('div');
      div.className = 'card';
      div.innerHTML = `
        <div>
          <div>${escapeHtml(p.name)}</div>
          <div class="meta">${p.defaultModel || 'no default model'} · ${p.workspacePath || 'no workspace path'}</div>
        </div>
        <button data-delete="${p.id}">Delete</button>`;
      list.appendChild(div);
    });
    list.querySelectorAll('[data-delete]').forEach((btn) =>
      btn.addEventListener('click', async () => {
        await api(`/api/projects/${btn.dataset.delete}`, { method: 'DELETE' });
        loadProjects();
      }));
  } catch (err) {
    list.innerHTML = `Error: ${err.message}`;
  }
}

// ---------- Permissions ----------
$('#permissionForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  await api('/api/permissions', {
    method: 'POST',
    body: JSON.stringify({
      toolName: $('#permToolName').value,
      decision: $('#permDecision').value,
      projectId: $('#permProject').value || null,
    }),
  });
  $('#permissionForm').reset();
  loadPermissions();
});

async function loadPermissions() {
  const list = $('#permissionList');
  list.innerHTML = 'Loading…';
  try {
    const permissions = await api('/api/permissions');
    list.innerHTML = '';
    permissions.forEach((p) => {
      const div = document.createElement('div');
      div.className = 'card';
      div.innerHTML = `
        <div>
          <div>${escapeHtml(p.toolName)} <span class="badge ${p.decision}">${p.decision}</span></div>
          <div class="meta">${p.projectId ? 'project-scoped' : 'global'}</div>
        </div>
        <button data-delete="${p.id}">Delete</button>`;
      list.appendChild(div);
    });
    list.querySelectorAll('[data-delete]').forEach((btn) =>
      btn.addEventListener('click', async () => {
        await api(`/api/permissions/${btn.dataset.delete}`, { method: 'DELETE' });
        loadPermissions();
      }));
  } catch (err) {
    list.innerHTML = `Error: ${err.message}`;
  }
}

// ---------- Agent Activity ----------
async function loadActivity() {
  const list = $('#activityList');
  list.innerHTML = 'Loading…';
  try {
    const events = await api('/api/tool-events');
    list.innerHTML = '';
    events.forEach((ev) => {
      const div = document.createElement('div');
      div.className = 'card';
      div.innerHTML = `
        <div>
          <div>${escapeHtml(ev.toolName)} <span class="badge ${ev.status}">${ev.status}</span></div>
          <div class="meta">${new Date(ev.createdAt).toLocaleString()} ${ev.resultSummary ? '· ' + escapeHtml(ev.resultSummary) : ''}</div>
        </div>`;
      list.appendChild(div);
    });
    if (events.length === 0) list.innerHTML = '<div class="meta">No tool activity yet. This fills up once the agent runtime reports tool calls.</div>';
  } catch (err) {
    list.innerHTML = `Error: ${err.message}`;
  }
}

// ---------- Models ----------
async function loadModels() {
  const list = $('#modelList');
  list.innerHTML = 'Loading…';
  try {
    const { models } = await api('/api/models');
    list.innerHTML = '';
    models.forEach((m) => {
      const div = document.createElement('div');
      div.className = 'card';
      div.innerHTML = `<div>${escapeHtml(m)}</div>`;
      list.appendChild(div);
    });
    if (models.length === 0) list.innerHTML = '<div class="meta">No models found. Run `ollama pull &lt;model&gt;` first.</div>';
  } catch (err) {
    list.innerHTML = `Error: ${err.message}`;
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str ?? '';
  return div.innerHTML;
}

// Initial load
loadProjectOptions();
loadModelOptions();

//very Bottom one
// ---------- Theme Toggle ----------

const themeToggle = document.getElementById('themeToggle');

if (themeToggle) {
  const themeIcon = themeToggle.querySelector('.theme-icon');
  const themeLabel = themeToggle.querySelector('.theme-label');

  function updateThemeButton() {
    const isLight = document.body.classList.contains('light-theme');

    if (themeIcon) {
      themeIcon.textContent = isLight ? '🌙' : '☀️';
    }

    if (themeLabel) {
      themeLabel.textContent = isLight
          ? 'Dark mode'
          : 'Light mode';
    }
  }

  function setTheme(theme) {
    if (theme === 'light') {
      document.body.classList.add('light-theme');
    } else {
      document.body.classList.remove('light-theme');
    }

    localStorage.setItem('gateway-theme', theme);

    updateThemeButton();
  }

  // Load saved theme
  const savedTheme = localStorage.getItem('gateway-theme');

  setTheme(savedTheme === 'light' ? 'light' : 'dark');

  // Toggle
  themeToggle.addEventListener('click', () => {
    const isLight =
        document.body.classList.contains('light-theme');

    setTheme(isLight ? 'dark' : 'light');
  });
}
