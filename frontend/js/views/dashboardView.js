/**
 * SafeRoute AI - Home Dashboard View Controller
 * 
 * Handles Dashboard rendering, search input validation, location swap,
 * sample data presentation, and Leaflet map preview initialization.
 */

import { MapManager } from '../map/mapManager.js';

export class DashboardView {
  constructor() {
    this.mapManager = new MapManager();
    this.dashboardData = null;
    this.initialized = false;
  }

  /**
   * Initialize Dashboard view
   */
  async init() {
    if (this.initialized) {
      this.mapManager.invalidateSize();
      return;
    }

    await this.loadData();
    this.renderOverviewCards();
    this.renderNearbyHazards();
    this.renderCommunityReports();
    this.renderRecentRoutes();
    this.initMapPreview();
    this.bindSearchEvents();
    this.bindQuickQueryPills();
    this.initialized = true;
  }

  /**
   * Load dashboard data from JSON or embedded fallback
   */
  async loadData() {
    try {
      const response = await fetch('assets/data/sample-dashboard.json');
      if (response.ok) {
        this.dashboardData = await response.json();
      } else {
        throw new Error(`Local demo data request failed with status ${response.status}.`);
      }
    } catch (err) {
      console.warn('Using embedded demo fallback data because sample-dashboard.json could not be loaded directly.', err);
      this.dashboardData = this.getEmbeddedFallbackData();
    }
  }

  /**
   * Render 4 Quick Safety Overview Cards
   */
  renderOverviewCards() {
    const data = this.dashboardData?.overviewCards;
    if (!data) return;

    // 1. Overall Safety Overview Card
    const elSafety = document.getElementById('card-safety-score');
    if (elSafety && data.safetyScore) {
      const valEl = elSafety.querySelector('.overview-value');
      const titleEl = elSafety.querySelector('.overview-title');
      const trendEl = elSafety.querySelector('.overview-trend');
      if (valEl) valEl.textContent = data.safetyScore.value;
      if (titleEl) titleEl.textContent = data.safetyScore.status;
      if (trendEl) {
        trendEl.className = 'overview-trend positive';
        trendEl.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>
          <span>${data.safetyScore.trend}</span>
        `;
      }
    }

    // 2. Nearby Hazards Card
    const elHazards = document.getElementById('card-nearby-hazards');
    if (elHazards && data.nearbyHazards) {
      const valEl = elHazards.querySelector('.overview-value');
      const titleEl = elHazards.querySelector('.overview-title');
      const trendEl = elHazards.querySelector('.overview-trend');
      if (valEl) valEl.textContent = data.nearbyHazards.value;
      if (titleEl) titleEl.textContent = data.nearbyHazards.status;
      if (trendEl) {
        trendEl.className = 'overview-trend';
        trendEl.innerHTML = `
          <span class="pulse-dot amber"></span>
          <span>${data.nearbyHazards.trend}</span>
        `;
      }
    }

    // 3. Recent Community Reports Card
    const elReports = document.getElementById('card-community-reports');
    if (elReports && data.communityReports) {
      const valEl = elReports.querySelector('.overview-value');
      const titleEl = elReports.querySelector('.overview-title');
      const trendEl = elReports.querySelector('.overview-trend');
      if (valEl) valEl.textContent = data.communityReports.value;
      if (titleEl) titleEl.textContent = data.communityReports.status;
      if (trendEl) {
        trendEl.className = 'overview-trend';
        trendEl.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 12 2 2 4-4"/><circle cx="12" cy="12" r="10"/></svg>
          <span>${data.communityReports.trend}</span>
        `;
      }
    }

    // 4. Safe Routes Available Card
    const elRoutes = document.getElementById('card-safe-routes');
    if (elRoutes && data.safeRoutes) {
      const valEl = elRoutes.querySelector('.overview-value');
      const titleEl = elRoutes.querySelector('.overview-title');
      const trendEl = elRoutes.querySelector('.overview-trend');
      if (valEl) valEl.textContent = data.safeRoutes.value;
      if (titleEl) titleEl.textContent = data.safeRoutes.status;
      if (trendEl) {
        trendEl.className = 'overview-trend positive';
        trendEl.innerHTML = `
          <span class="pulse-dot emerald"></span>
          <span>${data.safeRoutes.trend}</span>
        `;
      }
    }
  }

  /**
   * Render list of nearby hazard reports
   */
  renderNearbyHazards() {
    const container = document.getElementById('nearby-hazards-list');
    if (!container) return;

    const hazards = this.dashboardData?.nearbyHazards || [];
    container.innerHTML = hazards.map((h) => {
      const isCritical = h.severity === 'high' || h.severity === 'critical';
      const severityBadgeClass = isCritical ? 'badge-red' : (h.severity === 'medium' ? 'badge-amber' : 'badge-info');
      const iconClass = h.severity || 'medium';

      return `
        <div class="hazard-feed-item" data-id="${h.id}">
          <div class="hazard-feed-icon ${iconClass}">
            ${this.getHazardIconSvg(h.typeId)}
          </div>
          <div class="hazard-feed-body">
            <div class="hazard-feed-top">
              <span class="hazard-feed-category">${h.category}</span>
              <span class="badge ${severityBadgeClass}">${h.severityLabel || h.severity}</span>
            </div>
            <div class="hazard-feed-name">${h.title}</div>
            <div class="hazard-feed-meta">
              <span>📍 ${h.location}</span>
              <span>🕒 ${h.reportTime}</span>
              <span class="badge badge-demo">Demo</span>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  /**
   * Render list of recent community reports
   */
  renderCommunityReports() {
    const container = document.getElementById('community-reports-list');
    if (!container) return;

    const reports = this.dashboardData?.recentCommunityReports || [];
    container.innerHTML = reports.map((r) => {
      const verifyBadge = r.verified
        ? `<span class="badge badge-emerald">Verified (Demo)</span>`
        : `<span class="badge badge-amber">Pending (Demo)</span>`;

      return `
        <div class="report-feed-item" data-id="${r.id}">
          <div class="report-feed-header">
            <span class="badge badge-info">${r.category}</span>
            ${verifyBadge}
          </div>
          <p class="report-feed-text">${r.description}</p>
          <div class="report-feed-footer">
            <span>📍 ${r.location} &bull; ${r.timestamp}</span>
            <span title="Community confirmations">👍 ${r.votes} votes</span>
          </div>
        </div>
      `;
    }).join('');
  }

  /**
   * Render illustrative route history from the local demo dataset
   */
  renderRecentRoutes() {
    const container = document.getElementById('recent-routes-list');
    if (!container) return;

    const routes = this.dashboardData?.recentRoutes || [];
    container.innerHTML = routes.map((route) => `
      <article class="route-feed-item" data-id="${route.id}">
        <div class="route-feed-heading">
          <span class="route-feed-name">${route.from} <span aria-hidden="true">→</span> ${route.to}</span>
          <span class="badge badge-demo">Demo</span>
        </div>
        <div class="route-feed-meta">
          <span class="route-feed-score">Safety: ${route.safetyScore}/100</span>
          <span>&bull; ${route.distance}</span>
          <span>&bull; ${route.duration}</span>
        </div>
        <p class="route-feed-time">${route.timestamp}</p>
      </article>
    `).join('');
  }

  /**
   * Initialize Leaflet map preview with hazard pins
   */
  initMapPreview() {
    const mapContainer = document.getElementById('dashboard-map');
    const mapStatus = document.getElementById('dashboard-map-status');
    if (!mapContainer) {
      console.error('Dashboard map container #dashboard-map is missing.');
      return;
    }

    const map = this.mapManager.initMap('dashboard-map');
    if (!map) {
      if (mapStatus) {
        mapStatus.textContent = 'Map is loading... demo hazard markers are listed on the right.';
        mapStatus.hidden = false;
      }
      return;
    }

    if (mapStatus) {
      mapStatus.textContent = '';
      mapStatus.hidden = true;
    }
    this.mapManager.renderHazardMarkers(this.dashboardData?.nearbyHazards || []);
  }

  /**
   * Bind Location and Destination Search events
   */
  bindSearchEvents() {
    const startInput = document.getElementById('search-start');
    const destInput = document.getElementById('search-dest');
    const swapBtn = document.getElementById('btn-swap-locations');
    const findRouteBtn = document.getElementById('btn-find-safe-route');
    const validationMsg = document.getElementById('search-validation-msg');
    const searchForm = document.getElementById('route-search-form');

    if (!startInput || !destInput || !findRouteBtn || !searchForm) return;

    // Swap button functionality
    if (swapBtn) {
      swapBtn.addEventListener('click', () => {
        const temp = startInput.value;
        startInput.value = destInput.value;
        destInput.value = temp;

        // Visual rotation feedback
        swapBtn.style.transform = 'rotate(180deg)';
        setTimeout(() => {
          swapBtn.style.transform = '';
        }, 300);

        // Clear error highlights
        startInput.classList.remove('has-error');
        destInput.classList.remove('has-error');
        if (validationMsg) validationMsg.style.display = 'none';
      });
    }

    // Input clearing on typing
    [startInput, destInput].forEach((input) => {
      input.addEventListener('input', () => {
        input.classList.remove('has-error');
        if (validationMsg) validationMsg.style.display = 'none';
      });
    });

    // Find Safe Route button submit handler
    searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const startVal = startInput.value.trim();
      const destVal = destInput.value.trim();

      // Empty input validation
      if (!startVal && !destVal) {
        this.showValidation(validationMsg, 'Please enter both starting location and destination.', 'error');
        startInput.classList.add('has-error');
        destInput.classList.add('has-error');
        startInput.focus();
        return;
      }

      if (!startVal) {
        this.showValidation(validationMsg, 'Please enter a starting location.', 'error');
        startInput.classList.add('has-error');
        startInput.focus();
        return;
      }

      if (!destVal) {
        this.showValidation(validationMsg, 'Please enter a destination to navigate.', 'error');
        destInput.classList.add('has-error');
        destInput.focus();
        return;
      }

      // Valid search simulation with realistic feedback
      this.showValidation(
        validationMsg,
        `Demo Mode: Evaluated 3 safe corridors between "${startVal}" and "${destVal}". Recommended path safety score: 94/100 (well-lit, 0 reported accidents).`,
        'success'
      );

      // Re-center map to showcase area and invalidate size
      this.mapManager.centerOn(12.9750, 77.5980, 14);
      this.mapManager.invalidateSize();
    });
  }

  /**
   * Bind quick search query chips
   */
  bindQuickQueryPills() {
    const pills = document.querySelectorAll('.quick-query-pill');
    const startInput = document.getElementById('search-start');
    const destInput = document.getElementById('search-dest');
    const findRouteBtn = document.getElementById('btn-find-safe-route');

    pills.forEach((pill) => {
      pill.addEventListener('click', () => {
        const from = pill.getAttribute('data-from');
        const to = pill.getAttribute('data-to');

        if (startInput && destInput) {
          startInput.value = from || '';
          destInput.value = to || '';
          startInput.classList.remove('has-error');
          destInput.classList.remove('has-error');

          if (findRouteBtn) {
            findRouteBtn.click();
          }
        }
      });
    });
  }

  /**
   * Helper to display validation message
   */
  showValidation(element, message, type = 'error') {
    if (!element) return;
    element.className = `validation-message ${type}`;
    const icon = type === 'error'
      ? '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>'
      : '<path d="m9 12 2 2 4-4"/><circle cx="12" cy="12" r="10"/>';
    element.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icon}</svg><span></span>`;
    element.querySelector('span').textContent = message;
    element.style.display = 'flex';
  }

  /**
   * SVG Icon helper
   */
  getHazardIconSvg(typeId) {
    switch (typeId) {
      case 'poor_lighting':
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2v1"/><path d="M12 7a5 5 0 0 0-5 5c0 1.5.8 2.8 2 3.6V17h6v-1.4c1.2-.8 2-2.1 2-3.6a5 5 0 0 0-5-5z"/></svg>`;
      case 'road_accident':
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;
      case 'waterlogging':
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>`;
      case 'broken_sidewalk':
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="4" y1="4" x2="20" y2="20"/><path d="m4 12 16 8"/><path d="m12 4 8 16"/></svg>`;
      case 'unsafe_crossing':
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 15h16"/><path d="M4 9h16"/><path d="M10 3v18"/><path d="M14 3v18"/></svg>`;
      default:
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;
    }
  }

  /**
   * Embedded fallback data for guaranteed zero-dependency offline rendering
   */
  getEmbeddedFallbackData() {
    return {
      _meta: {
        isDemoData: true,
        notice: 'Illustrative fallback data only; not live or verified real-world information.'
      },
      overviewCards: {
        safetyScore: { value: "84/100", status: "Overall Safety Overview", trend: "+3.2% vs last week" },
        nearbyHazards: { value: "14", status: "Nearby Hazards", trend: "5 resolved in last 6 hrs" },
        communityReports: { value: "48", status: "Recent Community Reports", trend: "92% verified by commuters" },
        safeRoutes: { value: "8", status: "Safe Routes Available", trend: "100% well-lit corridors" }
      },
      nearbyHazards: [
        { id: "h1", category: "Poor Street Lighting", typeId: "poor_lighting", title: "4 Consecutive Streetlights Non-functional", location: "8th Main Road, 4th Block", reportTime: "15 mins ago", severity: "medium", severityLabel: "Moderate Caution", coordinates: [12.9750, 77.5980], confirmations: 8 },
        { id: "h2", category: "Road Accident", typeId: "road_accident", title: "Collision Cleared, Debris on Left Lane", location: "Outer Ring Road Junction 4", reportTime: "28 mins ago", severity: "high", severityLabel: "High Risk", coordinates: [12.9810, 77.6020], confirmations: 14 },
        { id: "h3", category: "Waterlogging", typeId: "waterlogging", title: "Rainwater Stagnation (Approx 6-8 inches)", location: "Subway Underpass, 12th Cross", reportTime: "45 mins ago", severity: "medium", severityLabel: "Moderate Caution", coordinates: [12.9680, 77.5890], confirmations: 19 },
        { id: "h4", category: "Broken Sidewalk", typeId: "broken_sidewalk", title: "Exposed Rebar and Missing Pavement Slabs", location: "Hospital Road, West Pedestrian Lane", reportTime: "1 hour ago", severity: "low", severityLabel: "Pedestrian Hazard", coordinates: [12.9640, 77.6010], confirmations: 6 },
        { id: "h5", category: "Unsafe Pedestrian Crossing", typeId: "unsafe_crossing", title: "Faded Zebra Markings & Faulty Walk Signal", location: "Central Boulevard Crossing", reportTime: "2 hours ago", severity: "medium", severityLabel: "Moderate Caution", coordinates: [12.9730, 77.5850], confirmations: 11 }
      ],
      recentCommunityReports: [
        { id: "r1", category: "Poor Street Lighting", description: "Reported entire lane pitch dark after 7 PM. Recommended rerouting via main avenue.", location: "5th Cross, Indiranagar", timestamp: "8 mins ago", verified: true, votes: 12 },
        { id: "r2", category: "Isolated Area", description: "Deserted construction stretch with poor visibility. Avoid late night walking.", location: "Near Metro Pillar 142", timestamp: "22 mins ago", verified: false, votes: 4 },
        { id: "r3", category: "Waterlogging", description: "Drainage overflow causing pedestrian walkway blockage. Two-wheelers skidding.", location: "100ft Road Underpass", timestamp: "40 mins ago", verified: true, votes: 23 },
        { id: "r4", category: "Heavy Traffic", description: "Signal failure causing severe bottleneck and pedestrian cross obstruction.", location: "Silk Board Junction", timestamp: "1 hour ago", verified: true, votes: 31 }
      ],
      recentRoutes: [
        { id: 'route-001', from: 'Indiranagar', to: 'Cubbon Park', safetyScore: 91, distance: '5.2 km', duration: '18 min', timestamp: 'Demo route · Today' },
        { id: 'route-002', from: 'MG Road', to: 'Lalbagh Botanical Garden', safetyScore: 87, distance: '4.1 km', duration: '16 min', timestamp: 'Demo route · Yesterday' },
        { id: 'route-003', from: 'Jayanagar', to: 'Cubbon Park', safetyScore: 89, distance: '7.4 km', duration: '24 min', timestamp: 'Demo route · 2 days ago' }
      ]
    };
  }
}
