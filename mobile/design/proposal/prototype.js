// Review-only interactions. No API calls, background jobs or notifications are created.
const params = new URLSearchParams(location.search);
const page = document.body.dataset.page;
const titles = {1: 'Oliver and the sleepy forest', 2: 'Oliver and the island of stars'};
const openDialog = id => document.getElementById(id).showModal();
document.querySelectorAll('[data-open]').forEach(button => button.addEventListener('click', () => openDialog(button.dataset.open)));
document.querySelectorAll('[data-art]').forEach(button => button.addEventListener('click', () => {
  document.querySelector('#art-viewer > img').src = button.dataset.art;
  openDialog('art-viewer');
}));
document.querySelectorAll('[data-font]').forEach(button => button.addEventListener('click', () => {
  document.documentElement.style.setProperty('--body-size', `${button.dataset.font}px`);
  document.querySelectorAll('[data-font]').forEach(option => option.setAttribute('aria-pressed', String(option === button)));
}));
document.querySelector('[data-night]')?.addEventListener('click', event => {
  const enabled = document.body.classList.toggle('night');
  event.currentTarget.setAttribute('aria-pressed', String(enabled));
});
document.querySelectorAll('[data-prompt]').forEach(button => button.addEventListener('click', () => {
  document.querySelector('textarea').value = button.dataset.prompt;
  document.querySelector('textarea').focus();
}));
// Session storage is optional: the prototypes still work when it is unavailable for file URLs.
function readPrompt() { try { return sessionStorage.getItem('dozy-proposal-prompt') || ''; } catch { return ''; } }
function savePrompt(value) { try { sessionStorage.setItem('dozy-proposal-prompt', value); } catch { /* Preview only. */ } }
if (page === 'continue') {
  const textarea = document.querySelector('textarea');
  textarea.value = readPrompt();
  document.querySelector('form[action]').addEventListener('submit', event => {
    event.preventDefault();
    savePrompt(textarea.value);
    // Keep the entered story idea out of URLs and browser history.
    location.href = 'generating.html';
  });
  if (params.get('after') === '2') {
    document.querySelector('.previous-story h3').textContent = titles[2];
    document.querySelector('.previous-story .eyebrow').textContent = 'CONTINUING FROM PART 2';
    document.querySelector('.previous-story').href = 'reader.html?part=2';
    document.querySelector('form > button[type=submit]').innerHTML = 'Create part 3 <span>✦</span>';
    document.querySelector('.topbar > a').href = 'reader.html?part=2';
  }
}
if (page === 'home') {
  const requested = params.get('status');
  const status = ['generating', 'ready', 'error'].includes(requested) ? requested : 'generating';
  document.getElementById(`job-${status}`).hidden = false;
  document.querySelector(`[data-status="${status}"]`).classList.add('active');
}
if (page === 'reader' && params.get('part') === '2') {
  document.querySelector('.reader > .eyebrow').textContent = 'THE MOONLIGHT ADVENTURES · PART 2';
  document.querySelector('.reader > h1').textContent = titles[2];
  document.querySelector('.metadata').innerHTML = '7 min read <span>·</span> Oliver & Luna';
  document.querySelector('.story-copy').innerHTML = '<p class="dropcap">The next evening, Oliver and Luna found a little boat waiting by the garden gate. Its sail was stitched from a silver leaf, and a tiny star was sleeping on the seat.</p><p>“Shall we take you home?” Oliver whispered. The star blinked once, softly, and the boat began to drift towards a quiet island in the sky.</p><h2>A harbour for sleepy stars</h2><p>On the island, every tree held a little light. Oliver lifted the tired star into the lowest branch, while Luna curled up beneath it. At last, all the stars were home.</p>';
  document.querySelector('.reader > p.story-copy').textContent = 'The little boat carried them gently home. By the time they reached the garden gate, the moon had tucked the island beneath a blanket of clouds. Oliver yawned. Tomorrow could wait.';
  document.querySelector('.ending > a.button').href = 'continue.html?after=2';
}
