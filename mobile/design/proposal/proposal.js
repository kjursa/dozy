const packs = {
  "3": { price: "9,99 zł" },
  "10": { price: "24,99 zł" },
  "25": { price: "49,99 zł" },
};

const packChoices = document.querySelectorAll('input[name="pack"]');
const checkoutLink = document.querySelector("#checkout");

function updateCheckoutLink() {
  const selectedPack = document.querySelector('input[name="pack"]:checked');
  if (!selectedPack || !checkoutLink) return;

  checkoutLink.href = `purchase.html?pack=${selectedPack.value}`;
  checkoutLink.innerHTML = `Continue with ${selectedPack.value} stories <span>→</span>`;
}

packChoices.forEach((choice) => choice.addEventListener("change", updateCheckoutLink));
updateCheckoutLink();

const purchaseCount = document.querySelector("#purchase-count");
const purchasePrice = document.querySelector("#purchase-price");
const selectedPurchase = new URLSearchParams(window.location.search).get("pack");
const purchase = packs[selectedPurchase] ? selectedPurchase : "10";

if (purchaseCount && purchasePrice) {
  purchaseCount.textContent = purchase;
  purchasePrice.textContent = packs[purchase].price;
}

document.querySelector("[data-start-purchase]")?.addEventListener("click", () => {
  document.querySelector("[data-purchase-dialog]")?.showModal();
});
