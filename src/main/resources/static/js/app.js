(function () {
  'use strict';

  const sleep = ms => new Promise(r => setTimeout(r, ms));

  function toast(message, kind) {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = message;
    el.className = 'toast show ' + (kind || '');
    clearTimeout(toast.t);
    toast.t = setTimeout(() => { el.className = 'toast'; }, 3500);
  }

  async function api(url, options) {
    const res = await fetch(url, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
    if (!res.ok) {
      const err = new Error((data && data.message) || ('Request failed (' + res.status + ')'));
      err.fieldErrors = data && data.fieldErrors;
      throw err;
    }
    return data;
  }

  function switchProject(id) {
    const url = new URL(window.location.href);
    url.searchParams.set('project', id);
    window.location.href = url.toString();
  }

  async function applyScenario(button) {
    const pid = button.parentElement.dataset.project;
    toast('Applying scenario and running pipeline. Live scans can take several minutes...', '');
    try {
      await api('/api/projects/' + pid + '/scenario', { method: 'PUT', body: JSON.stringify({ scenario: button.dataset.scenario }) });
      const run = await api('/api/projects/' + pid + '/pipeline', { method: 'POST' });
      toast('Scenario applied. Pipeline #' + run.id + ' result: ' + run.risk.level + ' risk.', 'ok');
      await sleep(700);
      window.location.reload();
    } catch (e) { toast(e.message, 'error'); }
  }

  function setStage(node, status) {
    node.className = 'stage s-' + status;
    const badge = node.querySelector('.badge');
    badge.textContent = status;
    badge.className = 'badge b-' + status;
  }

  async function runPipeline(pid, animate) {
    const btn = document.getElementById('runBtn');
    if (btn) btn.disabled = true;
    toast('Running pipeline. Live scans can take several minutes...', '');
    try {
      const run = await api('/api/projects/' + pid + '/pipeline', { method: 'POST' });
      const nodes = document.querySelectorAll('.stage');
      if (animate && nodes.length === run.stages.length) {
        nodes.forEach(n => setStage(n, 'PENDING'));
        for (let i = 0; i < run.stages.length; i++) {
          setStage(nodes[i], 'RUNNING');
          await sleep(550);
          setStage(nodes[i], run.stages[i].status);
          await sleep(120);
        }
        toast('Pipeline #' + run.id + ' finished: ' + run.status + ' (' + run.risk.level + ' risk)', run.status === 'FAILED' ? 'error' : 'ok');
        await sleep(1200);
      } else {
        toast('Pipeline #' + run.id + ' finished: ' + run.status, 'ok');
        await sleep(500);
      }
      window.location.reload();
    } catch (e) {
      toast(e.message, 'error');
      if (btn) btn.disabled = false;
    }
  }

  async function simulateDeploy(pid) {
    const box = document.getElementById('deployResult');
    try {
      const r = await api('/api/projects/' + pid + '/deploy', { method: 'POST' });
      box.className = 'result r-' + r.outcome;
      box.replaceChildren();
      const title = document.createElement('strong');
      title.textContent = r.title + ' (simulated)';
      const msg = document.createElement('p');
      msg.textContent = r.message;
      const list = document.createElement('ul');
      list.className = 'reasons';
      r.steps.forEach(s => { const li = document.createElement('li'); li.textContent = s; list.appendChild(li); });
      box.append(title, msg, list);
      box.hidden = false;
    } catch (e) { toast(e.message, 'error'); }
  }

  async function downloadReport(pid) {
    try {
      const res = await fetch('/api/projects/' + pid + '/report?download=true');
      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error((data && data.message) || 'Could not generate the report.');
      }
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'security-report-project-' + pid + '.html';
      document.body.appendChild(a);
      a.click();
      a.remove();
      toast('Report generated.', 'ok');
    } catch (e) { toast(e.message, 'error'); }
  }

  function filterRows(button) {
    const sev = button.dataset.sev;
    document.querySelectorAll('[data-action="filter"]').forEach(b => b.classList.toggle('ghost', b !== button));
    document.querySelectorAll('tr[data-sev]').forEach(row => {
      row.hidden = sev !== 'ALL' && row.dataset.sev !== sev;
    });
  }

  async function addProject(event) {
    event.preventDefault();
    const form = event.target;
    form.querySelectorAll('.field-error').forEach(n => n.remove());
    const body = {};
    new FormData(form).forEach((v, k) => { body[k] = v; });
    try {
      await api('/api/projects', { method: 'POST', body: JSON.stringify(body) });
      toast('Project added.', 'ok');
      await sleep(500);
      window.location.reload();
    } catch (e) {
      if (e.fieldErrors) {
        Object.keys(e.fieldErrors).forEach(name => {
          const input = form.elements[name];
          if (!input) return;
          const msg = document.createElement('div');
          msg.className = 'field-error';
          msg.textContent = e.fieldErrors[name];
          input.insertAdjacentElement('afterend', msg);
        });
      }
      toast(e.message, 'error');
    }
  }

  document.addEventListener('click', event => {
    const el = event.target.closest('[data-action]');
    if (!el) return;
    switch (el.dataset.action) {
      case 'run': runPipeline(el.dataset.project, el.dataset.animate === 'true'); break;
      case 'deploy': simulateDeploy(el.dataset.project); break;
      case 'scenario': applyScenario(el); break;
      case 'filter': filterRows(el); break;
      case 'report': {
        const select = document.getElementById('reportProject');
        if (select) downloadReport(select.value);
        break;
      }
      default: break;
    }
  });

  document.addEventListener('change', event => {
    if (event.target.id === 'projectSelect') switchProject(event.target.value);
  });

  const projectForm = document.getElementById('projectForm');
  if (projectForm) projectForm.addEventListener('submit', addProject);

  const loginForm = document.getElementById('loginForm');
  if (loginForm) loginForm.addEventListener('submit', e => { e.preventDefault(); window.location.href = '/dashboard'; });
})();
