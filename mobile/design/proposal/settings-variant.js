const noCredits = new URLSearchParams(window.location.search).get("balance") === "empty";

if (noCredits) {
  const card = document.querySelector("[data-credit-card]");
  card?.classList.add("is-empty");

  const count = document.querySelector("[data-credit-count]");
  const label = document.querySelector("[data-credit-label]");
  const title = document.querySelector("[data-credit-title]");
  const message = document.querySelector("[data-credit-message]");

  if (count) count.textContent = "0";
  if (label) label.textContent = "credits left";
  if (title) title.textContent = "Your next story is waiting";
  if (message) message.textContent = "Choose a story bundle to create another bedtime adventure. Your saved stories are always yours to read.";
}
