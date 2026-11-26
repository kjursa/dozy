const characters = {
  person: ["Curly", "Bob", "Side part", "Top knot", "Waves", "Pigtails", "Buzz cut", "Bun", "Fringe", "Spiky"],
  creature: ["Cat", "Puppy", "Rabbit", "Fox", "Dragon", "Forest sprite", "Little wizard"],
};
const palettes = {
  person: [["Peach", "#ffd3af"], ["Warm tan", "#e8ac79"], ["Brown", "#b77a55"], ["Deep brown", "#80513e"]],
  creature: [["Mint", "#41b5a2"], ["Amber", "#e9a055"], ["Lilac", "#a491cd"], ["Cocoa", "#95614e"]],
};
const clothes = ["Hoodie", "T-shirt", "Coat", "Scarf"];
const hats = ["None", "Cap", "Beanie", "Pirate hat"];
const selection = {
  person: { character: 0, color: 0, outfit: 0, hat: 0 },
  creature: { character: 0, color: 1 },
};

let type = "person";
const byId = (id) => document.getElementById(id);
const currentPalette = () => palettes[type === "person" ? "person" : "creature"];

function select(container, index) {
  Array.from(container.children).forEach((button, i) => {
    button.setAttribute("aria-pressed", String(i === index));
  });
}

function drawCharacter(canvas, character, color, outfit, hat, layer) {
  if (type === "person") {
    drawPortrait(canvas, type, character, color, outfit, hat, layer);
  } else if (character < 4) {
    drawPortrait(canvas, "animal", character, color);
  } else {
    drawPortrait(canvas, "fictional", character - 4, color);
  }
}

function drawPreview() {
  const { character, color, outfit, hat } = selection[type];
  const [colorName, value] = currentPalette()[color];
  const preview = byId("preview");
  drawCharacter(preview, character, value, outfit, hat);
  preview.setAttribute("aria-label", [characters[type][character], colorName, clothes[outfit], hats[hat]].filter(Boolean).join(", "));
  byId("result").textContent = "";
}

function renderChoice(id, labels, key, renderTile) {
  const container = byId(id);
  const current = document.createElement("div");
  current.className = "current-option";
  current.setAttribute("aria-live", "polite");

  function showCurrent() {
    const index = selection[type][key];
    current.replaceChildren();
    renderTile(current, index);
    current.append(labels[index]);
  }

  function arrow(step, symbol, direction) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "portrait-arrow";
    button.textContent = symbol;
    const label = { character: "character", outfit: "outfit", hat: "hat" }[key];
    button.setAttribute("aria-label", direction + " " + label);
    button.addEventListener("click", () => {
      selection[type][key] = (selection[type][key] + step + labels.length) % labels.length;
      showCurrent();
      drawPreview();
    });
    return button;
  }

  showCurrent();
  container.replaceChildren(arrow(-1, "‹", "Previous"), current, arrow(1, "›", "Next"));
}

function portraitTile(button, render) {
  const tile = document.createElement("canvas");
  tile.width = tile.height = 120;
  tile.setAttribute("aria-hidden", "true");
  render(tile);
  button.append(tile);
}

function renderEditor() {
  const isPerson = type === "person";
  const palette = currentPalette();
  byId("palette-label").textContent = isPerson ? "Skin tone" : "Color";
  byId("person-options").hidden = !isPerson;
  renderChoice("characters", characters[type], "character", (tileContainer, index) => {
    portraitTile(tileContainer, (tile) => drawCharacter(tile, index, palette[0][1], 0, 0, "head"));
  });

  const swatches = byId("palette");
  swatches.replaceChildren();
  palette.forEach(([name, color], index) => {
    const button = document.createElement("button");
    const swatch = document.createElement("span");
    button.type = "button";
    button.setAttribute("aria-label", name);
    button.title = name;
    swatch.style.setProperty("--swatch", color);
    button.append(swatch);
    button.addEventListener("click", () => {
      selection[type].color = index;
      select(swatches, index);
      drawPreview();
    });
    swatches.append(button);
  });
  select(swatches, selection[type].color);

  if (isPerson) {
    renderChoice("clothes", clothes, "outfit", (button, index) => {
      portraitTile(button, (tile) => drawPortrait(tile, type, 0, palette[0][1], index, 0, "clothes"));
    });
    renderChoice("hats", hats, "hat", (button, index) => {
      if (index > 0) portraitTile(button, (tile) => drawPortrait(tile, type, 0, palette[0][1], 0, index, "hat"));
    });
  }
  drawPreview();
}

const tabs = document.querySelectorAll("[data-type]");
tabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    if (type === tab.dataset.type) return;
    type = tab.dataset.type;
    tabs.forEach((item) => item.setAttribute("aria-pressed", String(item === tab)));
    renderEditor();
  });
});
byId("character-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const name = byId("name").value.trim();
  if (name) byId("result").textContent = `${name} is ready. This preview does not store characters.`;
});
renderEditor();
