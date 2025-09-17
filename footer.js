/* footer.js — reusable footer renderer */

export function renderFooter() {
  const footer = document.getElementById("footer");
  if (!footer) return; // page has no footer container

  const year = new Date().getFullYear();

  footer.innerHTML = `
    <footer class="footer" role="contentinfo">
      <div class="footer-logo">
        <img src="./assets/logo.svg" alt="MedPortal logo" width="28" height="28" />
        <p>© ${year} MedPortal. All rights reserved.</p>
      </div>

      <div class="footer-links">
        <div class="footer-column">
          <h4>Company</h4>
          <a href="/pages/about.html">About</a>
          <a href="/pages/careers.html">Careers</a>
          <a href="/pages/press.html">Press</a>
        </div>

        <div class="footer-column">
          <h4>Support</h4>
          <a href="/pages/account.html">Account</a>
          <a href="/pages/help.html">Help Center</a>
          <a href="/pages/contact.html">Contact</a>
        </div>

        <div class="footer-column">
          <h4>Legals</h4>
          <a href="/pages/terms.html">Terms</a>
          <a href="/pages/privacy.html">Privacy Policy</a>
          <a href="/pages/licensing.html">Licensing</a>
        </div>
      </div>
    </footer>
  `;
}

// Expose to window for non-module usage or inline calls
window.renderFooter = renderFooter;

// Auto-render when DOM is ready
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", renderFooter);
} else {
  renderFooter();
}
