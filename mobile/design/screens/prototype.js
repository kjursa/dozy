// Local design prototype only. No network generation, notifications or real persistence.
const page = document.body.dataset.page;
const params = new URLSearchParams(location.search);
const empty = params.get('state') === 'empty';
const $ = selector => document.querySelector(selector);
const all = selector => [...document.querySelectorAll(selector)];
all('[data-filled]').forEach(node => node.hidden = empty);
all('[data-empty]').forEach(node => node.hidden = !empty);
all('[data-open]').forEach(button => button.addEventListener('click', () => document.getElementById(button.dataset.open).showModal()));
all('[data-art]').forEach(button => button.addEventListener('click', () => {
  $('#art-viewer > img').src = button.dataset.art;
  $('#art-viewer').showModal();
}));
all('[data-font]').forEach(button => button.addEventListener('click', () => {
  document.documentElement.style.setProperty('--body-size', `${button.dataset.font}px`);
  all('[data-font]').forEach(option => option.setAttribute('aria-pressed', String(option === button)));
}));
$('[data-night]')?.addEventListener('click', event => {
  event.currentTarget.setAttribute('aria-pressed', String(document.body.classList.toggle('night')));
});
// Optional tab-local storage. File URLs may not provide storage in every browser.
function read(key) { try { return JSON.parse(sessionStorage.getItem(`dozy-proposal2-${key}`)); } catch { return null; } }
function save(key, value) { try { sessionStorage.setItem(`dozy-proposal2-${key}`, JSON.stringify(value)); } catch { /* Nonessential in a mockup. */ } }
if (page === 'home') {
  $('[data-empty-home]').hidden = !empty;
  $('[data-populated-home]').hidden = empty;
  const states = {
    generating: ['YOUR NEW STORY', 'A little magic is happening…', 'We’re creating the story and its cover. You can close the app while we work.', 'View progress', 'generating.html'],
    ready: ['READY FOR TONIGHT', 'Your new story is ready!', 'Oliver and the sleepy forest · 6 min read. A brand-new adventure with familiar faces.', 'Read your story', 'reader.html'],
    error: ['LET’S TRY THAT AGAIN', 'The adventure needs another moment.', 'We couldn’t finish the story. Your choices are saved — no need to start again.', 'Try again', 'generating.html']
  };
  const job = states[params.get('state')];
  if (job) {
    const node = $('[data-home-job]');
    node.hidden = false;
    node.className = `job-card ${params.get('state')}`;
    node.innerHTML = `<div class="job-label">${job[0]}</div><h2>${job[1]}</h2><p>${job[2]}</p><a class="button primary" href="${job[4]}">${job[3]} <span>→</span></a>`;
  }
}
if (page === 'create-story') {
  const form = $('#story-form');
  const mode = params.get('mode') === 'auto' ? 'auto' : 'manual';
  $(`input[name=mode][value=${mode}]`).checked = true;
  function updateMode() {
    const auto = $('input[name=mode]:checked').value === 'auto';
    $('[data-manual]').hidden = auto;
    $('[data-auto]').hidden = !auto;
    all('[data-manual] input, [data-manual] textarea').forEach(input => input.disabled = auto);
  }
  all('input[name=mode]').forEach(input => input.addEventListener('change', updateMode));
  updateMode();
  if (params.get('cast') === 'same') {
    all('input[name=heroes]').forEach(input => input.checked = ['oliver', 'luna'].includes(input.value));
    $('[data-cast-note]').textContent = 'Oliver and Luna are selected. New plot, new setting — no previous events carried over.';
  }
  if (empty) {
    all('input[name=heroes]').forEach(input => { input.checked = false; input.disabled = true; });
    $('button[type=submit]').disabled = true;
    $('[data-submit-hint]').textContent = 'Create a character to start your first story.';
  }
  form.addEventListener('submit', event => {
    event.preventDefault();
    const heroes = all('input[name=heroes]:checked');
    if (!heroes.length) {
      $('#cast-error').hidden = false;
      $('input[name=heroes]').focus();
      return;
    }
    $('#cast-error').hidden = true;
    const data = Object.fromEntries(new FormData(form));
    data.heroes = heroes.map(input => input.value);
    save('story', data);
    // Do not expose the parent's description in the URL or browser history.
    location.href = 'generating.html';
  });
  all('input[name=heroes]').forEach(input => input.addEventListener('change', () => $('#cast-error').hidden = true));
}
if (page === 'character-form') {
  const form = $('#character-form');
  const editing = params.has('edit');
  $('[data-character-inspiration]').hidden = !empty || editing;
  const defaults = {name: 'Oliver', personality: 'Curious and brave, loves exploring mysterious places and asking “what’s over there?”', appearance: 'Five years old, with curly brown hair, bright green eyes, a yellow hoodie and blue sneakers.'};
  const data = editing ? (read('character') || defaults) : defaults;
  if (!empty) Object.entries(data).forEach(([key, value]) => { const field = document.getElementById(key); if (field) field.value = value; });
  if (editing) { $('.topbar > span').textContent = 'EDIT CHARACTER'; $('[data-form-title]').textContent = 'A little more like them.'; }
  if (params.get('from') === 'story') $('.topbar > a').href = 'create-story.html?state=empty';
  let avatarState = 'empty';
  function avatar(state) {
    avatarState = state;
    all('[data-avatar]').forEach(node => node.hidden = node.dataset.avatar !== state);
    $('[data-avatar-error]').hidden = state !== 'error';
    $('[data-generate-avatar]').disabled = state === 'loading';
    $('[data-generate-avatar]').textContent = state === 'loading' ? 'Generating avatar…' : state === 'ready' ? 'Regenerate avatar ✦' : state === 'error' ? 'Try avatar again ✦' : 'Generate avatar ✦';
    $('[data-save-character]').disabled = state !== 'ready';
    $('[data-save-note]').hidden = state === 'ready';
    $('[data-avatar-note]').textContent = state === 'ready' ? 'Avatar preview ready. Save when you’re happy with it.' : state === 'loading' ? 'Your description is safe while the avatar is being prepared.' : 'Preview their avatar, then save your character.';
    $('#avatar-simulation').hidden = state !== 'loading';
  }
  const simulation = document.createElement('div');
  simulation.id = 'avatar-simulation';
  simulation.className = 'simulation';
  simulation.innerHTML = '<p>Prototype only — no image is being generated. Choose a result to preview:</p><button type="button" data-avatar-result="ready">Preview success</button><button type="button" data-avatar-result="error">Preview failure</button>';
  $('.review-controls').append(simulation);
  all('[data-avatar-result]').forEach(button => button.addEventListener('click', () => avatar(button.dataset.avatarResult)));
  avatar(['ready','loading','error'].includes(params.get('avatar')) ? params.get('avatar') : 'empty');
  $('[data-generate-avatar]').addEventListener('click', () => { if (form.reportValidity()) avatar('loading'); });
  all('#character-form input, #character-form textarea').forEach(input => input.addEventListener('input', () => {
    if (avatarState !== 'empty') avatar('empty');
    $('[data-avatar-note]').textContent = 'Description updated. Generate an avatar for these details.';
  }));
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (avatarState !== 'ready') return;
    save('character', {name: $('#name').value, personality: $('#personality').value, appearance: $('#appearance').value});
    location.href = params.get('from') === 'story' ? 'create-story.html?cast=oliver' : 'character.html';
  });
}
if (page === 'character') {
  const character = read('character');
  if (character) {
    $('[data-character-name]').textContent = character.name;
    $('[data-personality]').textContent = character.personality;
    $('[data-appearance]').textContent = character.appearance;
    $('main a.button.primary').textContent = `Create a story with ${character.name} ✦`;
  }
}
// Mark the exact preview URL, not just the filename, to distinguish empty and filled variants.
all('.preview-switch a').forEach(link => {
  if (link.pathname === location.pathname && link.search === location.search) {
    link.classList.add('active');
    link.setAttribute('aria-current', 'page');
  }
});

// Places stay independent from the main cast; inhabitants are optional story inspiration.
const placeExamples = {
  surprise: {name: 'Surprise me', description: 'Let us dream up somewhere new.', residents: ''},
  forest: {name: 'Whispering woods', description: 'A peaceful forest with glowing paths and old trees.', residents: 'Gentle fireflies and an owl who knows every woodland path.'},
  island: {name: 'Starlight island', description: 'A little island with sandy coves and a sea that glows at night.', residents: 'Friendly turtles and tiny crabs collecting seashells.'},
  village: {name: 'Cloudtop village', description: 'A cosy village floating between soft clouds.', residents: 'Cloud gardeners and sleepy birds delivering letters.'},
  custom: read('place') || {name: 'Grandma’s secret garden', description: 'A sunny garden with a crooked apple tree and a little wooden gate.', residents: 'A kind hedgehog and a robin who loves a chat.'}
};
if (page === 'create-story') {
  const form = $('#story-form');
  const placeKey = Object.hasOwn(placeExamples, params.get('place')) ? params.get('place') : 'surprise';
  const draft = params.has('place') ? read('story-draft') : null;
  if (draft) {
    all('input[name=heroes]').forEach(input => input.checked = draft.heroes.includes(input.value));
    for (const name of ['mode', 'length']) {
      all(`input[name=${name}]`).forEach(input => input.checked = input.value === draft[name]);
    }
    $('#idea').value = draft.idea;
    $('#ending').value = draft.ending;
    $('input[name=mode]:checked').dispatchEvent(new Event('change'));
  }
  $('[data-place-name]').textContent = placeExamples[placeKey].name;
  $('[data-place-summary]').textContent = placeExamples[placeKey].description;
  $('[data-place-input]').value = placeKey;
  $('.place-selection').addEventListener('click', () => save('story-draft', {
    heroes: all('input[name=heroes]:checked').map(input => input.value),
    mode: $('input[name=mode]:checked').value,
    length: $('input[name=length]:checked').value,
    idea: $('#idea').value,
    ending: $('#ending').value
  }));
  form.addEventListener('submit', () => {
    if (all('input[name=heroes]:checked').length && $('input[name=mode]:checked').value !== 'auto') {
      const story = read('story');
      if (story) save('story', {...story, placeDetails: placeExamples[placeKey]});
    }
  });
}
if (page === 'places') {
  $('[data-custom-name]').textContent = placeExamples.custom.name;
  $('[data-custom-description]').textContent = placeExamples.custom.description;
  $('[data-custom-residents]').textContent = placeExamples.custom.residents ? `You might meet: ${placeExamples.custom.residents}` : '';
  all('[data-select-place]').forEach(button => button.addEventListener('click', () => {
    location.href = `create-story.html?place=${button.dataset.selectPlace}`;
  }));
}
if (page === 'place-form') {
  if (!empty) {
    $('#place-name').value = placeExamples.custom.name;
    $('#place-description').value = placeExamples.custom.description;
    $('#place-residents').value = placeExamples.custom.residents;
  }
  if (params.has('edit')) $('[data-place-form-title]').textContent = 'A little more like your world.';
  $('#place-form').addEventListener('submit', event => {
    event.preventDefault();
    save('place', {name: $('#place-name').value, description: $('#place-description').value, residents: $('#place-residents').value});
    location.href = 'create-story.html?place=custom';
  });
}
if (page === 'home') {
  $('[data-home-place-name]').textContent = placeExamples.custom.name;
  $('[data-home-place-description]').textContent = placeExamples.custom.description;
  if (empty) $('.home-places .section-title > a').href = 'places.html?state=empty';
}
