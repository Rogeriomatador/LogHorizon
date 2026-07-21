const CONFIG = {
  javaAddress: "",
  bedrockAddress: "",
  bedrockPort: "",
  discordUrl: "",
  apkUrl: "",
  apkVersion: "1.0.0"
};

const header = document.querySelector(".site-header");
const menuButton = document.querySelector(".menu-button");
const navLinks = document.querySelector(".nav-links");
const toast = document.querySelector(".toast");

function applyConfig() {
  const mappings = [
    ["java-address", CONFIG.javaAddress],
    ["bedrock-address", CONFIG.bedrockAddress],
    ["bedrock-port", CONFIG.bedrockPort]
  ];

  mappings.forEach(([id, value]) => {
    const element = document.getElementById(id);
    if (!element || !value) return;
    element.textContent = value;
    const button = document.querySelector(`[data-copy-target="${id}"]`);
    if (button) button.disabled = false;
  });

  if (CONFIG.discordUrl) {
    document.querySelectorAll(".discord-link").forEach((link) => {
      link.href = CONFIG.discordUrl;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
    });
    const discordButton = document.querySelector(".discord-button");
    discordButton.disabled = false;
    discordButton.textContent = "ENTRAR NA COMUNIDADE";
    discordButton.addEventListener("click", () => window.open(CONFIG.discordUrl, "_blank", "noopener"));
  }

  if (CONFIG.apkUrl) {
    const downloadButton = document.querySelector(".download-button");
    downloadButton.disabled = false;
    downloadButton.textContent = `BAIXAR V${CONFIG.apkVersion}`;
    downloadButton.title = "Baixar Log Horizon Voice";
    downloadButton.addEventListener("click", () => {
      const anchor = document.createElement("a");
      anchor.href = CONFIG.apkUrl;
      anchor.download = `LogHorizonVoice-v${CONFIG.apkVersion}.apk`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    });

    const note = document.querySelector(".download-note");
    note.textContent = "Download oficial da versão release assinada para Android.";
  }
}

function showToast(message = "Copiado!") {
  toast.textContent = message;
  toast.classList.add("visible");
  clearTimeout(showToast.timeout);
  showToast.timeout = setTimeout(() => toast.classList.remove("visible"), 1800);
}

window.addEventListener("scroll", () => {
  header.classList.toggle("scrolled", window.scrollY > 20);
});

window.addEventListener("pointermove", (event) => {
  document.documentElement.style.setProperty("--mouse-x", `${event.clientX}px`);
  document.documentElement.style.setProperty("--mouse-y", `${event.clientY}px`);
});

menuButton.addEventListener("click", () => {
  const isOpen = navLinks.classList.toggle("open");
  menuButton.classList.toggle("active", isOpen);
  menuButton.setAttribute("aria-expanded", String(isOpen));
  document.body.style.overflow = isOpen ? "hidden" : "";
});

navLinks.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", () => {
    navLinks.classList.remove("open");
    menuButton.classList.remove("active");
    menuButton.setAttribute("aria-expanded", "false");
    document.body.style.overflow = "";
  });
});

document.querySelectorAll(".copy-button").forEach((button) => {
  button.addEventListener("click", async () => {
    const target = document.getElementById(button.dataset.copyTarget);
    if (!target || button.disabled) return;
    try {
      await navigator.clipboard.writeText(target.textContent.trim());
      showToast("Endereço copiado!");
    } catch {
      showToast("Não foi possível copiar");
    }
  });
});

const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add("visible");
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.13 });

document.querySelectorAll(".reveal").forEach((element) => observer.observe(element));

document.getElementById("current-year").textContent = new Date().getFullYear();
applyConfig();
