/**
 * SafeRoute AI - Frontend Entry Point
 * 
 * Initializes core application, navigation handlers, view switching, and system readiness status.
 */

import { CONFIG } from './config.js';
import { DashboardView } from './views/dashboardView.js';
import { RouteView } from './views/routeView.js';
import { Toast } from './components/toast.js';

class SafeRouteApp {
  constructor() {
    this.config = CONFIG;
    this.currentView = 'dashboard';
    this.dashboardView = new DashboardView();
    this.routeView = new RouteView();
  }

  /**
   * Bootstrap the application on DOM ready
   */
  async init() {
    console.group(`%c🚀 ${this.config.APP_NAME} v${this.config.VERSION}`, 'color: #10B981; font-weight: bold; font-size: 14px;');
    console.log(`Tagline: ${this.config.TAGLINE}`);
    console.log(`Mode: %c${this.config.DEMO_MODE ? 'DEMO MODE (Sample Data Active)' : 'LIVE BACKEND MODE'}`, 'color: #F59E0B; font-weight: bold;');
    console.log(`Target Spring Boot API: ${this.config.API.BASE_URL}`);
    console.log(`Leaflet CDN Loaded: ${typeof window.L !== 'undefined' ? 'Yes (v' + (window.L ? window.L.version : '') + ')' : 'Pending/No'}`);
    console.groupEnd();

    this.bindNavigationEvents();
    this.bindMobileMenu();
    this.updateStatusBadges();

    const initialHash = window.location.hash.replace('#', '');
    const initialView = (initialHash === 'route-explorer') ? 'route-explorer' : 'dashboard';
    await this.switchView(initialView);
    if (initialView !== 'dashboard') {
      const navLinks = document.querySelectorAll('.nav-link');
      navLinks.forEach((l) => l.classList.remove('active'));
      const activeLink = document.querySelector(`.nav-link[data-view="${initialView}"]`);
      if (activeLink) activeLink.classList.add('active');
    }
  }

  /**
   * Set up navigation tab click handlers
   */
  bindNavigationEvents() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach((link) => {
      link.addEventListener('click', async (event) => {
        event.preventDefault();
        const targetView = link.getAttribute('data-view');
        if (!targetView) return;

        const switched = await this.switchView(targetView);
        if (switched) {
          navLinks.forEach((l) => l.classList.remove('active'));
          link.classList.add('active');
        }

        // Close mobile nav drawer if open
        const navMenu = document.getElementById('nav-links');
        if (navMenu && navMenu.classList.contains('open')) {
          navMenu.classList.remove('open');
          const toggleBtn = document.getElementById('mobile-nav-toggle');
          if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
        }
      });
    });

    // Support brand logo click returning to dashboard
    const brand = document.querySelector('.brand');
    if (brand) {
      brand.addEventListener('click', async (event) => {
        event.preventDefault();
        await this.switchView('dashboard');
        navLinks.forEach((l) => l.classList.remove('active'));
        const dashLink = document.querySelector('.nav-link[data-view="dashboard"]');
        if (dashLink) dashLink.classList.add('active');
      });
    }

    // Support hash navigation if URL has #route-explorer or #dashboard
    window.addEventListener('hashchange', () => {
      const hash = window.location.hash.replace('#', '');
      if (hash && (hash === 'dashboard' || hash === 'route-explorer')) {
        this.switchView(hash).then((switched) => {
          if (switched) {
            navLinks.forEach((l) => l.classList.remove('active'));
            const activeLink = document.querySelector(`.nav-link[data-view="${hash}"]`);
            if (activeLink) activeLink.classList.add('active');
          }
        });
      }
    });
  }

  /**
   * Mobile drawer toggle behavior
   */
  bindMobileMenu() {
    const toggleBtn = document.getElementById('mobile-nav-toggle');
    const navLinks = document.getElementById('nav-links');

    if (toggleBtn && navLinks) {
      toggleBtn.addEventListener('click', () => {
        const isOpen = navLinks.classList.toggle('open');
        toggleBtn.setAttribute('aria-expanded', String(isOpen));
      });
    }
  }

  /**
   * Sync visual status badges with CONFIG settings
   */
  updateStatusBadges() {
    const demoBadge = document.getElementById('demo-mode-badge');
    if (demoBadge) {
      demoBadge.setAttribute('aria-label', this.config.DEMO_MODE ? 'Demo mode active' : 'Live mode configured');
    }

    const versionBadge = document.getElementById('app-version-badge');
    if (versionBadge) {
      versionBadge.textContent = `v${this.config.VERSION}`;
    }
  }

  /**
   * View switcher between Dashboard, Route Explorer, and future stages
   * @param {string} viewName
   * @returns {Promise<boolean>}
   */
  async switchView(viewName) {
    const viewDashboard = document.getElementById('view-dashboard');
    const viewRouteExplorer = document.getElementById('view-route-explorer');
    const status = document.getElementById('navigation-status');

    if (viewName === 'dashboard') {
      if (viewRouteExplorer) viewRouteExplorer.hidden = true;
      if (viewDashboard) viewDashboard.hidden = false;

      this.currentView = 'dashboard';
      await this.dashboardView.init();
      if (this.dashboardView.mapManager) {
        this.dashboardView.mapManager.invalidateSize();
      }

      if (status) status.textContent = 'Dashboard is active.';
      return true;
    }

    if (viewName === 'route-explorer') {
      if (viewDashboard) viewDashboard.hidden = true;
      if (viewRouteExplorer) viewRouteExplorer.hidden = false;

      this.currentView = 'route-explorer';
      await this.routeView.init();
      this.routeView.invalidateMap();

      if (status) status.textContent = 'Route Explorer is active.';
      return true;
    }

    // Future stages notifications
    const stageMap = {
      'safety-map': 'Stage 4 (Interactive Safety Map)',
      'hazard-report': 'Stage 5 (Hazard Reporting Interface)',
      'alerts': 'Stage 7 (Live Safety Alerts)'
    };
    const stageName = stageMap[viewName] || 'a future stage';
    Toast.show(`${viewName.replace('-', ' ').toUpperCase()} will be unlocked in ${stageName}. Currently exploring Route Explorer (Stage 3).`, 'info', 4000);

    if (status) {
      status.textContent = `${viewName.replace('-', ' ')} is scheduled for ${stageName}.`;
    }
    return false;
  }
}

// Global initialization
document.addEventListener('DOMContentLoaded', () => {
  const app = new SafeRouteApp();
  window.SafeRouteApp = app;
  app.init().catch((error) => {
    console.error('SafeRoute AI failed to initialize the application.', error);
    const status = document.getElementById('navigation-status');
    if (status) status.textContent = 'The application could not be initialized. Check the browser console for details.';
  });
});
