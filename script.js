const CONFIG = {
  javaAddress: "loghorizon.enxada.host",
  bedrockAddress: "enx-cirion-84.enx.host",
  bedrockPort: "10167",
  discordUrl: "https://discord.gg/wCxTJY3W7G",
  apkUrl: "downloads/LogHorizonVoice-v1.0.0.apk",
  apkVersion: "1.0.0",
  voiceWebUrl: "http://enx-cirion-84.enx.host:10057",
  voiceTutorialUrl: "voz-pc.html"
};

const header = document.querySelector(".site-header");
const menuButton = document.querySelector(".menu-button");
const navLinks = document.querySelector(".nav-links");
const toast = document.querySelector(".toast");

async function configureDownload() {
  const downloadButton = document.querySelector(".download-button");
  const note = document.querySelector(".download-note");
  if (!CONFIG.apkUrl || !downloadButton) return;

  try {
    const response = await fetch(CONFIG.apkUrl, { method: "HEAD", cache: "no-store" });
    if (!response.ok) return;

    downloadButton.disabled = false;
    downloadButton.textContent = `BAIXAR V${CONFIG.apkVersion}`;
    downloadButton.title = "Baixar Log Horizon Voice";
    note.textContent = "Download oficial da versão release assinada para Android.";

    downloadButton.addEventListener("click", () => {
      const anchor = document.createElement("a");
      anchor.href = CONFIG.apkUrl;
      anchor.download = `LogHorizonVoice-v${CONFIG.apkVersion}.apk`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
    });
  } catch {
    // O botão permanece desativado até o APK existir no caminho configurado.
  }
}

function configurePcVoice() {
  if (!CONFIG.voiceTutorialUrl) return;

  const appNavLink = navLinks?.querySelector('a[href="#voz"]');
  if (appNavLink && !navLinks.querySelector('a[href="voz-pc.html"]')) {
    const pcLink = document.createElement("a");
    pcLink.href = CONFIG.voiceTutorialUrl;
    pcLink.textContent = "Voz no PC";
    appNavLink.insertAdjacentElement("afterend", pcLink);
  }

  const voiceCopy = document.querySelector(".voice-copy");
  const downloadNote = document.querySelector(".download-note");
  if (voiceCopy && downloadNote && !document.querySelector(".pc-voice-card")) {
    const intro = voiceCopy.querySelector(":scope > p");
    if (intro) {
      intro.textContent = "Use o aplicativo oficial no Android ou entre pela interface web no computador para participar da voz por proximidade.";
    }

    const pcCard = document.createElement("div");
    pcCard.className = "download-card pc-voice-card";
    pcCard.innerHTML = `
      <div class="android-mark" aria-hidden="true">PC</div>
      <div><strong>Voz pelo navegador</strong><span>Windows • Chrome, Edge ou Opera GX</span></div>
      <a class="button button-outline" href="${CONFIG.voiceTutorialUrl}">VER TUTORIAL</a>
    `;
    downloadNote.insertAdjacentElement("afterend", pcCard);

    const list = voiceCopy.querySelector(".check-list");
    if (list && !list.querySelector(".pc-voice-item")) {
      const item = document.createElement("li");
      item.className = "pc-voice-item";
      item.innerHTML = "<span>✓</span> Acesso pelo navegador no computador";
      list.appendChild(item);
    }
  }

  const featureVoice = document.querySelector(".feature-grid .feature-card:nth-child(2) p");
  if (featureVoice) {
    featureVoice.textContent = "Converse com quem está perto usando o aplicativo Android ou a interface web no PC.";
  }

  const faqAnswers = document.querySelectorAll(".accordion details p");
  if (faqAnswers[1]) {
    faqAnswers[1].textContent = "A voz é opcional. No Android, use o aplicativo oficial. No computador, também é possível usar a interface web seguindo o tutorial do site.";
  }
}

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
    if (discordButton) {
      discordButton.disabled = false;
      discordButton.textContent = "ENTRAR NA COMUNIDADE";
      discordButton.addEventListener("click", () => window.open(CONFIG.discordUrl, "_blank", "noopener"));
    }
  }

  configurePcVoice();
  configureDownload();
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