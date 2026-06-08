#!/usr/bin/env node
/**
 * 番茄钟 CLI — 终端秒开版
 * 用法: node cli.js [命令]
 *
 * 命令:
 *   start           开始番茄钟 (25分钟)
 *   pause           暂停
 *   resume          继续
 *   skip            跳过当前阶段
 *   status          查看状态
 *   add-task <名>   添加任务
 *   list-tasks      列出任务
 *   done-task <id>  完成任务
 *   stats           今日统计
 *   settings        查看设置
 *   help            帮助
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');

const DATA_FILE = path.join(process.env.USERPROFILE || process.env.HOME, '.pomodoro-cli.json');

function load() {
  try { return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch(e) {}
  return { tasks: [], records: [], settings: { work: 25, shortBreak: 5, longBreak: 15, interval: 4 } };
}

function save(d) { fs.writeFileSync(DATA_FILE, JSON.stringify(d, null, 2)); }

// ===================== Interactive mode =====================
function runInteractive() {
  const data = load();
  let timer = null;
  let remaining = data.settings.work * 60;
  let total = remaining;
  let phase = 'work';
  let pomos = 0;
  let running = false;

  function fmt(s) { const m = Math.floor(s/60); const ss = s%60; return `${String(m).padStart(2,'0')}:${String(ss).padStart(2,'0')}`; }

  function show() {
    console.clear();
    const labels = { work: '工作中', shortBreak: '短休息', longBreak: '长休息' };
    const barW = 30;
    const pct = remaining / total;
    const filled = Math.round(barW * pct);
    const bar = '█'.repeat(filled) + '░'.repeat(barW - filled);
    console.log('  ╔══════════════════════════════════╗');
    console.log(`  ║  ${labels[phase] || '就绪'}  |  已完成 ${pomos} 个  ║`);
    console.log('  ╠══════════════════════════════════╣');
    console.log(`  ║         ${fmt(remaining)}              ║`);
    console.log(`  ║  ${bar}  ║`);
    console.log('  ╠══════════════════════════════════╣');
    console.log(`  ║  [s]开始/暂停 [k]跳过 [r]重置     ║`);
    console.log(`  ║  [a]加任务 [l]列表 [q]退出        ║`);
    console.log('  ╚══════════════════════════════════╝');
  }

  function tick() {
    remaining--;
    if (remaining <= 0) {
      if (phase === 'work') {
        pomos++;
        const today = new Date().toISOString().split('T')[0];
        data.records.push({ date: today });
        save(data);
        console.log('\n  *** 番茄钟完成! 休息一下吧 ***\n');
      }
      // Advance phase
      if (phase === 'work') {
        if (pomos > 0 && pomos % data.settings.interval === 0) {
          phase = 'longBreak'; remaining = data.settings.longBreak * 60;
        } else {
          phase = 'shortBreak'; remaining = data.settings.shortBreak * 60;
        }
      } else {
        phase = 'work'; remaining = data.settings.work * 60;
      }
      total = remaining;
    }
    show();
  }

  show();

  readline.emitKeypressEvents(process.stdin);
  if (process.stdin.isTTY) process.stdin.setRawMode(true);

  process.stdin.on('keypress', (str, key) => {
    if (key.name === 'q') { if (timer) clearInterval(timer); process.exit(0); }
    if (key.name === 's') {
      if (!running) { timer = setInterval(tick, 1000); running = true; }
      else { clearInterval(timer); timer = null; running = false; }
    }
    if (key.name === 'k') { // skip
      if (timer) clearInterval(timer); timer = null; running = false;
      if (phase === 'work') pomos++;
      if (phase === 'work') {
        if (pomos > 0 && pomos % data.settings.interval === 0) { phase = 'longBreak'; remaining = data.settings.longBreak * 60; }
        else { phase = 'shortBreak'; remaining = data.settings.shortBreak * 60; }
      } else { phase = 'work'; remaining = data.settings.work * 60; }
      total = remaining;
      show();
    }
    if (key.name === 'r') { // reset
      if (timer) clearInterval(timer); timer = null; running = false;
      phase = 'work'; remaining = data.settings.work * 60; total = remaining; pomos = 0;
      show();
    }
    if (key.name === 'a') {
      process.stdin.setRawMode(false);
      const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
      rl.question('  任务名称: ', (title) => {
        if (title.trim()) {
          data.tasks.push({ id: Date.now().toString(36), title: title.trim(), pomos: 0, done: false });
          save(data);
          console.log('  已添加: ' + title.trim());
        }
        rl.close();
        if (process.stdin.isTTY) process.stdin.setRawMode(true);
        setTimeout(show, 500);
      });
    }
    if (key.name === 'l') {
      console.log('\n  --- 任务列表 ---');
      data.tasks.forEach((t, i) => console.log(`  ${i+1}. ${t.done ? '[V]' : '[ ]'} ${t.title}`));
      console.log('');
    }
  });
}

// ===================== Entry =====================
const cmd = process.argv[2];
const arg = process.argv[3];

// No command → interactive mode
if (!cmd) {
  runInteractive();
  process.exit(0);
}

const data = load();

switch(cmd) {
  case 'stats': {
    const today = new Date().toISOString().split('T')[0];
    const todayCount = data.records.filter(r => r.date === today).length;
    console.log(`今日: ${todayCount} | 总计: ${data.records.length}`);
    break;
  }
  case 'add-task': {
    if (!arg) { console.log('用法: node cli.js add-task "任务名"'); break; }
    data.tasks.push({ id: Date.now().toString(36), title: arg, pomos: 0, done: false });
    save(data);
    console.log('已添加: ' + arg);
    break;
  }
  case 'list-tasks': {
    data.tasks.forEach((t, i) => console.log(`  ${i+1}. ${t.done ? '[V]' : '[ ]'} ${t.title}  [Px${t.pomos}]`));
    break;
  }
  case 'done-task': {
    const idx = parseInt(arg) - 1;
    if (data.tasks[idx]) { data.tasks[idx].done = true; save(data); console.log('已完成: ' + data.tasks[idx].title); }
    break;
  }
  case 'settings': {
    const s = data.settings;
    console.log(`工作: ${s.work}min | 短休: ${s.shortBreak}min | 长休: ${s.longBreak}min | 间隔: ${s.interval}轮`);
    break;
  }
  case 'help':
  default:
    console.log(`
  番茄钟 CLI 用法:
    node cli.js              交互模式 (推荐)
    node cli.js stats        查看统计
    node cli.js add-task "名" 添加任务
    node cli.js list-tasks   列出任务
    node cli.js done-task 1  完成任务1
    node cli.js settings     查看设置
    `);
    break;
  }
}
