const fields = {
  person: [
    ["face", "Face", ["Round", "Oval", "Soft square"]],
    ["hair", "Hair", ["Short", "Curly", "Long", "Shaved"]],
    ["outfit", "Outfit", ["Sage T-shirt", "Berry hoodie", "Blue coat", "Mustard scarf"]],
  ],
  animal: [
    ["species", "Head", ["Cat", "Dog", "Rabbit", "Fox"]],
    ["ears", "Ears", ["Natural", "Rounded", "Floppy"]],
    ["accessory", "Accessory", ["None", "Bandana", "Scarf", "Bow"]],
  ],
  fantasy: [
    ["variant", "Portrait", ["Little dragon", "Moon sprite"]],
  ],
};

const defaults = {
  person: { face: 0, hair: 1, outfit: 0, skin: 1 },
  animal: { species: 0, ears: 0, accessory: 0 },
  fantasy: { variant: 0 },
};

function portraitFor(type, parts) {
  if (type === "person") return { ...parts, color: parts.outfit };
  if (type === "animal") return { ...parts, coat: [0, 2, 3, 1][parts.species], color: parts.accessory };
  return { fantasy: true, species: parts.variant === 0 ? 3 : 0, coat: parts.variant === 0 ? 0 : 3, accessory: 0, color: 0 };
}

const colors = ["#62a38b", "#d45369", "#37659b", "#ffb52c"];
const skin = ["#f5c8ab", "#dba377", "#a56a4c", "#784c43"];
const coats = ["#ecceb1", "#da8a53", "#9c6653", "#777084"];
const $ = (id) => document.getElementById(id);

function drawPortrait(canvas, portrait) {
  const ctx = canvas.getContext("2d");
  const ink = "#30252a";
  function outline() {
    ctx.strokeStyle = ink;
    ctx.lineWidth = 2.8;
    ctx.lineJoin = "round";
    ctx.lineCap = "round";
    ctx.stroke();
  }
  const ellipse = (x, y, rx, ry, fill, edged = false) => {
    ctx.fillStyle = fill;
    ctx.beginPath();
    ctx.ellipse(x, y, rx, ry, 0, 0, 2 * Math.PI);
    ctx.fill();
    if (edged) outline();
  };
  const path = (points, fill, edged = false) => {
    ctx.fillStyle = fill;
    ctx.beginPath();
    points.forEach(([x, y], index) => index ? ctx.lineTo(x, y) : ctx.moveTo(x, y));
    ctx.closePath();
    ctx.fill();
    if (edged) outline();
  };
  function smile(x, y) {
    ctx.beginPath();
    ctx.moveTo(x - 7, y);
    ctx.quadraticCurveTo(x, y + 8, x + 7, y);
    outline();
  }
  ctx.clearRect(0, 0, 144, 144);
  const color = colors[portrait.color];
  if ("species" in portrait) {
    const fur = portrait.fantasy ? ["#2cb7a3", "#da8a53", "#9c6653", "#9c8bb2"][portrait.coat] : coats[portrait.coat];
    if (portrait.ears === 1) {
      ellipse(42, 52, 13, 17, fur, true);
      ellipse(102, 52, 13, 17, fur, true);
    } else if (portrait.ears === 2) {
      ellipse(38, 72, 13, 25, fur, true);
      ellipse(106, 72, 13, 25, fur, true);
    } else if (portrait.species === 2) {
      ellipse(54, 40, 10, 29, fur, true);
      ellipse(90, 40, 10, 29, fur, true);
      ellipse(54, 38, 4, 19, "#e6a6a2");
      ellipse(90, 38, 4, 19, "#e6a6a2");
    } else if (portrait.species === 1) {
      ellipse(38, 69, 15, 27, "#714f49", true);
      ellipse(106, 69, 15, 27, "#714f49", true);
    } else {
      path([[34, 71], [35, 30], [64, 55], [80, 55], [109, 30], [110, 71]], fur, true);
      path([[40, 57], [40, 39], [55, 55]], "#e6a6a2");
      path([[89, 55], [104, 39], [104, 57]], "#e6a6a2");
    }
    ellipse(72, 82, 39, 34, fur, true);
    if (portrait.species === 3) {
      path([[39, 78], [72, 91], [105, 78], [97, 101], [72, 115], [47, 101]], "#f8e6cb");
    } else {
      ellipse(72, 94, 23, 15, "#f8e6cb");
    }
    ellipse(59, 78, 3, 4, "#30272d");
    ellipse(85, 78, 3, 4, "#30272d");
    ellipse(48, 87, 6, 4, "#e98273");
    ellipse(96, 87, 6, 4, "#e98273");
    ellipse(72, 91, 5, 3, ink);
    smile(72, 98);
    if (portrait.fantasy) {
      path([[64, 47], [72, 26], [80, 47]], "#ffbf51", true);
    }
    if (portrait.accessory === 1) path([[47, 106], [97, 106], [73, 131]], color);
    if (portrait.accessory === 2) {
      ellipse(72, 112, 29, 7, color);
      path([[82, 111], [96, 112], [98, 133], [84, 133]], color);
    }
    if (portrait.accessory === 3) {
      path([[52, 104], [72, 111], [92, 104], [92, 122], [72, 115], [52, 122]], color);
      ellipse(72, 113, 5, 5, color);
    }
    return;
  }

  const face = skin[portrait.skin];
  const hair = ["#60403a", "#4f3441", "#3e2b37", "#523b35"][portrait.skin];
  if (portrait.hair === 2) ellipse(72, 79, 38, 46, hair, true);
  if (portrait.outfit === 1) ellipse(72, 116, 36, 20, "#e2d3c1");
  ellipse(72, 149, 49, 39, color, true);
  ellipse(72, 105, 10, 13, face);
  ellipse(43, 76, 7, 9, face, true);
  ellipse(101, 76, 7, 9, face, true);
  if (portrait.face === 2) {
    ctx.fillStyle = face;
    ctx.beginPath();
    ctx.roundRect(43, 43, 58, 62, 19);
    ctx.fill();
    outline();
  } else {
    ellipse(72, 73, portrait.face === 0 ? 32 : 28, portrait.face === 0 ? 31 : 36, face, true);
  }
  if (portrait.hair !== 3) ellipse(72, 49, 31, 16, hair, true);
  if (portrait.hair === 1) {
    [45, 56, 68, 81, 93, 101].forEach((x, i) => ellipse(x, i % 2 ? 40 : 46, 10, 11, hair));
  } else if (portrait.hair !== 3) {
    ellipse(55, 55, 13, 9, hair);
  }
  ellipse(61, 75, 3, 4, "#30272d");
  ellipse(83, 75, 3, 4, "#30272d");
  ellipse(53, 86, 6, 4, "#e98273");
  ellipse(91, 86, 6, 4, "#e98273");
  smile(72, 89);
  if (portrait.outfit === 1) {
    path([[63, 115], [66, 115], [66, 132], [63, 132]], "#fff3df");
    path([[78, 115], [81, 115], [81, 132], [78, 132]], "#fff3df");
  }
  if (portrait.outfit === 2) {
    path([[62, 111], [72, 144], [55, 118]], "#e0c5a6");
    path([[82, 111], [72, 144], [89, 118]], "#e0c5a6");
  }
  if (portrait.outfit === 3) {
    ellipse(72, 110, 21, 7, "#c38b39");
    path([[80, 108], [90, 110], [91, 135], [80, 135]], "#c38b39");
  }
}

const cast = [
  { name: "Oliver", type: "person", personality: "Curious and kind", appearance: "", parts: { ...defaults.person } },
  { name: "Pippin", type: "animal", personality: "A bold little cat", appearance: "", parts: { ...defaults.animal, accessory: 2 } },
];

let editingIndex;
let type = "person";
let parts;
let draftByType;

function showForm(visible) {
  $("characters").hidden = visible;
  $("character").hidden = !visible;
  $("toolbar-title").textContent = visible ? "CHARACTER" : "CHARACTERS";
  $("back").hidden = !visible;
  $("add").hidden = visible;
  $("left-space").hidden = visible;
  $("right-space").hidden = !visible;
}

function renderCast() {
  $("cast").replaceChildren();
  cast.forEach((character, index) => {
    const button = document.createElement("button");
    button.className = "character-card";
    const canvas = document.createElement("canvas");
    canvas.className = "portrait";
    canvas.width = canvas.height = 144;
    canvas.setAttribute("aria-hidden", "true");
    const name = document.createElement("h2");
    name.textContent = character.name;
    drawPortrait(canvas, portraitFor(character.type, character.parts));
    button.append(canvas, name);
    button.addEventListener("click", () => editCharacter(index));
    $("cast").append(button);
  });
}

function editCharacter(index) {
  editingIndex = index;
  const item = cast[index];
  $("form-title").textContent = item ? "Edit character" : "New character";
  type = item?.type ?? "person";
  parts = { ...(item?.parts ?? defaults[type]) };
  draftByType = { [type]: parts };
  ["name", "personality", "appearance"].forEach((key) => { $(key).value = item?.[key] ?? ""; });
  renderEditor();
  showForm(true);
}

function renderEditor() {
  document.querySelectorAll("[data-type]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.type === type));
  });
  drawPortrait($("preview"), portraitFor(type, parts));
  $("controls").replaceChildren();
  fields[type].forEach(([key, label, values]) => {
    const row = document.createElement("div");
    row.className = "portrait-option";
    const caption = document.createElement("span");
    caption.textContent = label;
    const value = document.createElement("strong");
    value.setAttribute("aria-live", "polite");
    value.textContent = values[parts[key]];
    function arrow(delta, symbol, direction) {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "portrait-arrow";
      button.textContent = symbol;
      button.setAttribute("aria-label", direction + " " + label.toLowerCase());
      button.addEventListener("click", () => {
        parts[key] = (parts[key] + delta + values.length) % values.length;
        value.textContent = values[parts[key]];
        drawPortrait($("preview"), portraitFor(type, parts));
      });
      return button;
    }
    row.append(caption, arrow(-1, "‹", "Previous"), value, arrow(1, "›", "Next"));
    $("controls").append(row);
  });
}

document.querySelectorAll("[data-type]").forEach((button) => {
  button.addEventListener("click", () => {
    if (type === button.dataset.type) return;
    type = button.dataset.type;
    draftByType[type] ??= { ...defaults[type] };
    parts = draftByType[type];
    renderEditor();
  });
});
$("add").addEventListener("click", () => editCharacter());
$("back").addEventListener("click", () => showForm(false));
$("character").addEventListener("submit", (event) => {
  event.preventDefault();
  const item = { type, parts: { ...parts } };
  ["name", "personality", "appearance"].forEach((key) => { item[key] = $(key).value.trim(); });
  if (!item.name) return;
  if (editingIndex === undefined) cast.push(item);
  else cast[editingIndex] = item;
  renderCast();
  showForm(false);
});
renderCast();
showForm(false);
