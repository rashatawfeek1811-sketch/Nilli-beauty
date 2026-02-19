(function () {
  const root = document.documentElement;

  const searchInput = document.getElementById("searchInput");
  const clearSearchBtn = document.getElementById("clearSearchBtn");
  const expandAllBtn = document.getElementById("expandAllBtn");
  const collapseAllBtn = document.getElementById("collapseAllBtn");
  const themeToggleBtn = document.getElementById("themeToggleBtn");
  const backToTopBtn = document.getElementById("backToTopBtn");

  const tocLinks = Array.from(document.querySelectorAll(".toc__link"));
  const observedSections = Array.from(document.querySelectorAll(".observe"));
  const copyBtns = Array.from(document.querySelectorAll(".copylink"));

  // -------------------------
  // Theme
  // -------------------------
  const savedTheme = localStorage.getItem("theme");
  if (savedTheme === "light") root.setAttribute("data-theme", "light");
  setThemeLabel();

  themeToggleBtn.addEventListener("click", () => {
    const current = root.getAttribute("data-theme");
    if (current === "light") {
      root.removeAttribute("data-theme");
      localStorage.setItem("theme", "dark");
    } else {
      root.setAttribute("data-theme", "light");
      localStorage.setItem("theme", "light");
    }
    setThemeLabel();
  });

  function setThemeLabel() {
    const isLight = root.getAttribute("data-theme") === "light";
    themeToggleBtn.textContent = isLight ? "Dark mode" : "Light mode";
  }

  // -------------------------
  // Expand / Collapse all
  // -------------------------
  const allDetails = () => Array.from(document.querySelectorAll("details.block"));

  expandAllBtn.addEventListener("click", () => {
    allDetails().forEach(d => d.open = true);
  });
  collapseAllBtn.addEventListener("click", () => {
    allDetails().forEach(d => d.open = false);
  });

  // -------------------------
  // Copy link buttons
  // -------------------------
  copyBtns.forEach(btn => {
    btn.addEventListener("click", async () => {
      const hash = btn.getAttribute("data-copy");
      const url = new URL(window.location.href);
      url.hash = hash;
      try {
        await navigator.clipboard.writeText(url.toString());
        const old = btn.textContent;
        btn.textContent = "Copied";
        setTimeout(() => (btn.textContent = old), 900);
      } catch {
        // fallback
        window.location.hash = hash;
      }
    });
  });

  // -------------------------
  // Scrollspy (IntersectionObserver)
  // -------------------------
  const idToToc = new Map();
  tocLinks.forEach(a => {
    const href = a.getAttribute("href");
    if (href && href.startsWith("#")) idToToc.set(href.slice(1), a);
  });

  const spy = new IntersectionObserver((entries) => {
    // find the most visible intersecting section
    const visible = entries
      .filter(e => e.isIntersecting)
      .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

    if (!visible) return;

    const id = visible.target.id;
    tocLinks.forEach(l => l.classList.remove("is-active"));
    const link = idToToc.get(id);
    if (link) link.classList.add("is-active");
  }, {
    root: null,
    threshold: [0.2, 0.35, 0.5, 0.65, 0.8],
    rootMargin: "-10% 0px -70% 0px"
  });

  observedSections.forEach(sec => spy.observe(sec));

  // -------------------------
  // Back to top
  // -------------------------
  window.addEventListener("scroll", () => {
    const y = window.scrollY || document.documentElement.scrollTop;
    backToTopBtn.style.display = y > 900 ? "inline-flex" : "none";
  });

  backToTopBtn.addEventListener("click", () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  });

  // -------------------------
  // Score bars
  // -------------------------
  document.querySelectorAll(".bar").forEach((bar) => {
    const val = Number(bar.getAttribute("data-val") || "0");
    const pct = Math.max(0, Math.min(10, val)) * 10;
    bar.style.setProperty("--w", pct);
  });

  // -------------------------
  // Search: filter sections + highlight matches
  // -------------------------
  const searchableSections = observedSections.filter(s => s.id !== "entity-nelli"); // keep hero visible

  const originalHTML = new Map();
  searchableSections.forEach(sec => originalHTML.set(sec, sec.innerHTML));

  function escapeRegExp(str) {
    return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  function highlightInSection(sectionEl, query) {
    // Restore original before applying new highlight
    sectionEl.innerHTML = originalHTML.get(sectionEl);

    if (!query) return;

    const re = new RegExp(`(${escapeRegExp(query)})`, "gi");

    // Highlight in text nodes (simple approach: replace innerHTML for common blocks)
    // We avoid touching <script> and keep it conservative.
    const selectors = "p, li, td, th, pre, h1, h2, h3, h4, summary, .callout__body, .callout__title, .stat__value, .stat__note";
    sectionEl.querySelectorAll(selectors).forEach(node => {
      if (node.children.length > 0 && node.tagName.toLowerCase() === "pre") return;

      const html = node.innerHTML;
      // Skip if already contains mark
      if (html.includes("<mark")) return;

      const replaced = html.replace(re, "<mark>$1</mark>");
      if (replaced !== html) node.innerHTML = replaced;
    });
  }

  function sectionMatches(sectionEl, query) {
    if (!query) return true;
    const text = sectionEl.innerText.toLowerCase();
    return text.includes(query.toLowerCase());
  }

  function applySearch() {
    const q = (searchInput.value || "").trim();

    searchableSections.forEach(sec => {
      const match = sectionMatches(sec, q);
      sec.style.display = match ? "" : "none";
      if (match) highlightInSection(sec, q);
      else sec.innerHTML = originalHTML.get(sec);
    });

    // Update TOC link visibility
    tocLinks.forEach(link => {
      const id = link.getAttribute("href")?.slice(1);
      const sec = id ? document.getElementById(id) : null;
      if (!sec) return;
      const visible = sec.style.display !== "none";
      link.style.display = visible ? "" : "none";
    });
  }

  searchInput.addEventListener("input", applySearch);
  clearSearchBtn.addEventListener("click", () => {
    searchInput.value = "";
    applySearch();
    searchInput.focus();
  });

  // Focus search with "/"
  window.addEventListener("keydown", (e) => {
    if (e.key === "/" && document.activeElement !== searchInput) {
      e.preventDefault();
      searchInput.focus();
    }
    if (e.key === "Escape" && document.activeElement === searchInput) {
      searchInput.blur();
    }
  });

  // Initial run
  applySearch();
})();
