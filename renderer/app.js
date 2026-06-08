// ===================== CUSTOM DIALOG (replaces prompt/confirm/alert) =====================
// Electron's contextIsolation blocks native prompt()/confirm()/alert()
// so we use our own modal dialog with Promise-based API.

const promptOverlay = document.getElementById('prompt-overlay');
const promptTitle = document.getElementById('prompt-title');
const promptInput = document.getElementById('prompt-input');
const promptMsg = document.getElementById('prompt-msg');
const btnOk = document.getElementById('btn-prompt-ok');
const btnCancel = document.getElementById('btn-prompt-cancel');

let promptResolve = null;

function showCustomPrompt(title, defaultVal, placeholder) {
  return new Promise(resolve => {
    promptResolve = resolve;
    promptTitle.textContent = title;
    promptInput.value = defaultVal || '';
    promptInput.placeholder = placeholder || '';
    promptInput.style.display = '';
    promptMsg.style.display = 'none';
    promptOverlay.classList.add('show');
    promptInput.focus();
  });
}

function showCustomAlert(title, message) {
  return new Promise(resolve => {
    promptResolve = resolve;
    promptTitle.textContent = title;
    promptInput.style.display = 'none';
    promptMsg.style.display = '';
    promptMsg.textContent = message;
    promptOverlay.classList.add('show');
  });
}

function showCustomConfirm(title, message) {
  return new Promise(resolve => {
    promptResolve = resolve;
    promptTitle.textContent = title;
    promptInput.style.display = 'none';
    promptMsg.style.display = '';
    promptMsg.textContent = message;
    promptOverlay.classList.add('show');
  });
}

btnOk.addEventListener('click', () => {
  promptOverlay.classList.remove('show');
  if (promptResolve) {
    const val = promptInput.style.display === 'none' ? true : promptInput.value;
    promptResolve(val);
    promptResolve = null;
  }
});

btnCancel.addEventListener('click', () => {
  promptOverlay.classList.remove('show');
  if (promptResolve) {
    promptResolve(promptInput.style.display === 'none' ? false : null);
    promptResolve = null;
  }
});

promptOverlay.addEventListener('click', function(e) {
  if (e.target === this) {
    this.classList.remove('show');
    if (promptResolve) { promptResolve(null); promptResolve = null; }
  }
});

// Allow Enter key to confirm
promptInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') btnOk.click();
  if (e.key === 'Escape') btnCancel.click();
});

// ===================== DATA LAYER =====================
const STORAGE_KEY = 'pomodoro-data';

function loadData() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch(e) {}
  return {
    tasks: [],
    records: [],
    settings: { workDuration: 25, shortBreakDuration: 5, longBreakDuration: 15, longBreakInterval: 4 }
  };
}

function saveData(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

let data = loadData();

// ===================== TIMER ENGINE =====================
const States = { IDLE: 0, WORKING: 1, SHORT_BREAK: 2, LONG_BREAK: 3, PAUSED: 4 };
const StateLabels = { 0: '准备开始', 1: '工作中 - 保持专注!', 2: '短休息 - 放松一下', 3: '长休息 - 好好休息', 4: '已暂停' };

let timer = {
  state: States.IDLE,
  remaining: 0,
  total: 0,
  completedPomodoros: 0,
  intervalId: null
};

function resetTimer() {
  stopTimerTick();
  timer.state = States.IDLE;
  timer.remaining = data.settings.workDuration * 60;
  timer.total = timer.remaining;
  timer.completedPomodoros = 0;
  updateTimerUI();
}

function startPause() {
  if (timer.state === States.IDLE) {
    timer.state = States.WORKING;
    timer.remaining = data.settings.workDuration * 60;
    timer.total = timer.remaining;
    startTimerTick();
  } else if (timer.state === States.PAUSED) {
    setStateByRemaining();
    startTimerTick();
  } else {
    timer.state = States.PAUSED;
    stopTimerTick();
  }
  updateTimerUI();
}

function skip() {
  stopTimerTick();
  advancePhase();
  updateTimerUI();
}

async function reset() {
  const ok = await showCustomConfirm('确认重置', '确定要重置计时器吗？当前进度将丢失。');
  if (ok) resetTimer();
}

function startTimerTick() {
  stopTimerTick();
  timer.intervalId = setInterval(tick, 1000);
}

function stopTimerTick() {
  if (timer.intervalId) { clearInterval(timer.intervalId); timer.intervalId = null; }
}

function tick() {
  timer.remaining--;
  if (timer.remaining <= 0) {
    stopTimerTick();
    if (timer.state === States.WORKING) {
      timer.completedPomodoros++;
      onPomodoroComplete();
    }
    advancePhase();
  }
  updateTimerUI();
}

function advancePhase() {
  if (timer.state === States.WORKING || timer.state === States.IDLE) {
    if (timer.completedPomodoros > 0 && timer.completedPomodoros % data.settings.longBreakInterval === 0) {
      timer.state = States.LONG_BREAK;
      timer.remaining = data.settings.longBreakDuration * 60;
    } else {
      timer.state = States.SHORT_BREAK;
      timer.remaining = data.settings.shortBreakDuration * 60;
    }
  } else {
    timer.state = States.WORKING;
    timer.remaining = data.settings.workDuration * 60;
  }
  timer.total = timer.remaining;
}

function setStateByRemaining() {
  if (timer.remaining === data.settings.workDuration * 60) timer.state = States.WORKING;
  else if (timer.remaining === data.settings.shortBreakDuration * 60) timer.state = States.SHORT_BREAK;
  else if (timer.remaining === data.settings.longBreakDuration * 60) timer.state = States.LONG_BREAK;
  else timer.state = States.WORKING;
}

function onPomodoroComplete() {
  const sel = document.getElementById('task-select');
  const taskId = sel.value;
  const today = new Date().toISOString().split('T')[0];
  data.records.push({ date: today, taskId: taskId, taskTitle: sel.options[sel.selectedIndex]?.text || '' });

  if (taskId) {
    const task = data.tasks.find(t => t.id === taskId);
    if (task) task.pomodoroCount++;
  }

  saveData(data);

  if (window.electronAPI) {
    window.electronAPI.showNotification('番茄钟完成!', `太棒了! 已完成 ${timer.completedPomodoros} 个番茄钟。`);
    window.electronAPI.updateTrayTooltip(`番茄钟 - 已完成 ${timer.completedPomodoros} 个`);
  }

  refreshTaskList();
  updatePomoCount();
}

// ===================== UI UPDATES =====================
function updateTimerUI() {
  const mins = Math.floor(timer.remaining / 60);
  const secs = timer.remaining % 60;
  document.getElementById('countdown').textContent = String(mins).padStart(2,'0') + ':' + String(secs).padStart(2,'0');
  document.getElementById('phase-label').textContent = StateLabels[timer.state] || '';

  const pct = timer.total > 0 ? ((timer.total - timer.remaining) / timer.total * 100) : 0;
  const fill = document.getElementById('progress-fill');
  fill.style.width = pct + '%';

  if (timer.state === States.WORKING) fill.style.background = 'var(--pink)';
  else if (timer.state === States.SHORT_BREAK) fill.style.background = '#FF8AB7';
  else if (timer.state === States.LONG_BREAK) fill.style.background = '#C21882';
  else fill.style.background = 'var(--pink-muted)';

  const label = document.getElementById('phase-label');
  if (timer.state === States.WORKING) label.style.color = 'var(--pink-dark)';
  else if (timer.state === States.SHORT_BREAK) label.style.color = '#FF8AB7';
  else if (timer.state === States.LONG_BREAK) label.style.color = '#C21882';
  else label.style.color = 'var(--text-medium)';

  const btn = document.getElementById('btn-start');
  const skipBtn = document.getElementById('btn-skip');
  if (timer.state === States.IDLE) {
    btn.textContent = '开始'; skipBtn.disabled = true;
  } else if (timer.state === States.PAUSED) {
    btn.textContent = '继续'; skipBtn.disabled = false;
  } else {
    btn.textContent = '暂停'; skipBtn.disabled = false;
  }

  if (timer.state === States.WORKING || timer.state === States.SHORT_BREAK || timer.state === States.LONG_BREAK) {
    btn.classList.add('btn-active');
  } else {
    btn.classList.remove('btn-active');
  }

  updatePomoCount();
}

function updatePomoCount() {
  document.getElementById('pomo-count').textContent = '本轮已完成: ' + timer.completedPomodoros + ' 个番茄';
}

// ===================== TASKS =====================
function refreshTaskList() {
  const list = document.getElementById('task-list');
  const sel = document.getElementById('task-select');

  const uncompleted = data.tasks.filter(t => !t.completed);
  const completed = data.tasks.filter(t => t.completed);
  const sorted = [...uncompleted, ...completed];

  list.innerHTML = sorted.map(t => `
    <div class="task-item ${t.completed ? 'done' : ''}" data-id="${t.id}">
      <div class="task-circle">&#10003;</div>
      <span class="task-title">${esc(t.title)}</span>
      ${t.pomodoroCount > 0 ? `<span class="task-badge">Px${t.pomodoroCount}</span>` : ''}
    </div>
  `).join('');

  list.querySelectorAll('.task-item').forEach(el => {
    el.addEventListener('click', (e) => {
      const id = el.dataset.id;
      if (e.target.classList.contains('task-circle')) {
        toggleTaskComplete(id);
      } else {
        selectTask(id);
      }
    });
  });

  const curVal = sel.value;
  sel.innerHTML = '<option value="">(无任务)</option>' +
    uncompleted.map(t => `<option value="${t.id}">${esc(t.title)}</option>`).join('');
  if (curVal && data.tasks.find(t => t.id === curVal && !t.completed)) sel.value = curVal;

  document.getElementById('task-count').textContent = `已完成 ${completed.length} / ${data.tasks.length}`;
}

async function addTask() {
  const title = await showCustomPrompt('新建任务', '', '请输入任务名称');
  if (title && title.trim()) {
    data.tasks.push({
      id: Date.now().toString(36) + Math.random().toString(36).substr(2, 5),
      title: title.trim(),
      pomodoroCount: 0,
      completed: false,
      createdAt: new Date().toISOString().split('T')[0]
    });
    saveData(data);
    refreshTaskList();
  }
}

async function editTask() {
  const sel = getSelectedTaskId();
  if (!sel) { await showCustomAlert('提示', '请先选中一个任务。'); return; }
  const task = data.tasks.find(t => t.id === sel);
  if (!task) return;
  const title = await showCustomPrompt('修改任务', task.title);
  if (title && title.trim()) {
    task.title = title.trim();
    saveData(data);
    refreshTaskList();
  }
}

async function deleteTask() {
  const sel = getSelectedTaskId();
  if (!sel) { await showCustomAlert('提示', '请先选中一个任务。'); return; }
  const task = data.tasks.find(t => t.id === sel);
  const ok = await showCustomConfirm('确认删除', `确定要删除 "${task.title}" 吗？`);
  if (ok) {
    data.tasks = data.tasks.filter(t => t.id !== sel);
    saveData(data);
    refreshTaskList();
  }
}

function toggleTaskComplete(id) {
  const task = data.tasks.find(t => t.id === id);
  if (task) {
    task.completed = !task.completed;
    saveData(data);
    refreshTaskList();
  }
}

function selectTask(id) {
  document.getElementById('task-select').value = id;
  document.querySelectorAll('.task-item').forEach(el => el.classList.toggle('selected', el.dataset.id === id));
}

function getSelectedTaskId() {
  const sel = document.getElementById('task-select').value;
  if (sel) return sel;
  const selected = document.querySelector('.task-item.selected');
  return selected ? selected.dataset.id : null;
}

// ===================== STATS =====================
function refreshStats() {
  const today = new Date().toISOString().split('T')[0];
  const todayCount = data.records.filter(r => r.date === today).length;

  const now = new Date();
  const dayOfWeek = now.getDay();
  const monday = new Date(now);
  monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
  monday.setHours(0,0,0,0);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  sunday.setHours(23,59,59,999);

  const mondayStr = monday.toISOString().split('T')[0];
  const sundayStr = sunday.toISOString().split('T')[0];
  const weekCount = data.records.filter(r => r.date >= mondayStr && r.date <= sundayStr).length;
  const totalCount = data.records.length;

  document.getElementById('stat-today').textContent = todayCount;
  document.getElementById('stat-week').textContent = weekCount;
  document.getElementById('stat-total').textContent = totalCount;

  drawBarChart(monday);
}

function drawBarChart(monday) {
  const canvas = document.getElementById('bar-chart');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const W = canvas.width;
  const H = canvas.height;
  ctx.clearRect(0, 0, W, H);

  const counts = new Array(7).fill(0);
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const ds = d.toISOString().split('T')[0];
    counts[i] = data.records.filter(r => r.date === ds).length;
  }

  const maxVal = Math.max(4, Math.max(...counts));
  const roundedMax = Math.ceil(maxVal / 4) * 4;
  const padding = { top: 10, right: 20, bottom: 30, left: 35 };
  const chartW = W - padding.left - padding.right;
  const chartH = H - padding.top - padding.bottom;
  const barGap = 8;
  const barW = (chartW - barGap * 6) / 7;
  const baseline = H - padding.bottom;

  ctx.strokeStyle = '#f5e0e8';
  ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i++) {
    const y = baseline - (chartH * i / 4);
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(W - padding.right, y);
    ctx.stroke();
    ctx.fillStyle = '#B48C9B';
    ctx.font = '11px "Microsoft YaHei"';
    ctx.textAlign = 'right';
    ctx.fillText(Math.round(roundedMax * i / 4), padding.left - 6, y + 4);
  }
  ctx.textAlign = 'center';

  const todayIdx = (new Date().getDay() + 6) % 7;
  const labels = ['一','二','三','四','五','六','日'];

  for (let i = 0; i < 7; i++) {
    const barH = Math.max(2, (counts[i] / roundedMax) * chartH);
    const x = padding.left + i * (barW + barGap);
    const y = baseline - barH;

    ctx.fillStyle = (i === todayIdx) ? '#C2185B' : '#E91E63';
    ctx.beginPath();
    const radius = 4;
    ctx.moveTo(x, y + radius);
    ctx.arcTo(x, y, x + radius, y, radius);
    ctx.arcTo(x + barW, y, x + barW, y + radius, radius);
    ctx.arcTo(x + barW, baseline, x + barW - radius, baseline, radius);
    ctx.arcTo(x, baseline, x, baseline - radius, radius);
    ctx.closePath();
    ctx.fill();

    if (counts[i] > 0) {
      ctx.fillStyle = '#4A1A2E';
      ctx.font = 'bold 11px "Microsoft YaHei"';
      ctx.fillText(counts[i], x + barW / 2, y - 6);
    }

    ctx.fillStyle = '#8A5A6E';
    ctx.font = '12px "Microsoft YaHei"';
    ctx.fillText(labels[i], x + barW / 2, baseline + 16);
  }
}

// ===================== SETTINGS =====================
function showSettings() {
  document.getElementById('set-work').value = data.settings.workDuration;
  document.getElementById('set-short-break').value = data.settings.shortBreakDuration;
  document.getElementById('set-long-break').value = data.settings.longBreakDuration;
  document.getElementById('set-interval').value = data.settings.longBreakInterval;
  document.getElementById('modal-overlay').classList.add('show');
}

function saveSettings() {
  data.settings.workDuration = parseInt(document.getElementById('set-work').value) || 25;
  data.settings.shortBreakDuration = parseInt(document.getElementById('set-short-break').value) || 5;
  data.settings.longBreakDuration = parseInt(document.getElementById('set-long-break').value) || 15;
  data.settings.longBreakInterval = parseInt(document.getElementById('set-interval').value) || 4;
  saveData(data);
  document.getElementById('modal-overlay').classList.remove('show');
  resetTimer();
}

document.getElementById('modal-overlay').addEventListener('click', function(e) {
  if (e.target === this) this.classList.remove('show');
});

// ===================== EVENT BINDINGS =====================
document.getElementById('btn-start').addEventListener('click', startPause);
document.getElementById('btn-reset').addEventListener('click', reset);
document.getElementById('btn-skip').addEventListener('click', skip);

document.getElementById('btn-add-task').addEventListener('click', addTask);
document.getElementById('btn-edit-task').addEventListener('click', editTask);
document.getElementById('btn-delete-task').addEventListener('click', deleteTask);

document.getElementById('btn-settings').addEventListener('click', showSettings);
document.getElementById('btn-save-settings').addEventListener('click', saveSettings);
document.getElementById('btn-cancel-settings').addEventListener('click', () => {
  document.getElementById('modal-overlay').classList.remove('show');
});

document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    tab.classList.add('active');
    document.getElementById('tab-' + tab.dataset.tab).classList.add('active');
    if (tab.dataset.tab === 'stats') refreshStats();
    if (tab.dataset.tab === 'tasks') refreshTaskList();
  });
});

// ===================== HELPERS =====================
function esc(s) {
  const div = document.createElement('div');
  div.textContent = s;
  return div.innerHTML;
}

// ===================== INIT =====================
resetTimer();
refreshTaskList();
