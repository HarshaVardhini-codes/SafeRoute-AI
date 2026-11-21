/**
 * SafeRoute AI - Route Explorer View Controller
 * 
 * Manages route search validation, location swapping, multi-route comparison card rendering,
 * route selection state, and Leaflet polyline rendering via MapManager.
 */

import { MapManager } from '../map/mapManager.js';
import { Toast } from '../components/toast.js';

export class RouteView {
  /**
   * @param {MapManager|null} mapManager - Optional existing MapManager instance to reuse
   */
  constructor(mapManager = null) {
    this.mapManager = mapManager || new MapManager();
    this.routesData = null;
    this.currentJourney = null;
    this.currentRoutes = [];
    this.selectedRouteId = null;
    this.initialized = false;
  }

  /**
   * Initialize the Route Explorer view lifecycle
   */
  async init() {
    if (this.initialized) {
      this.invalidateMap();
      return;
    }

    await this.loadRouteData();
    this.initRouteMap();
    this.bindSearchEvents();
    this.bindQuickQueryPills();
    this.bindPolylineMapCallback();

    // Load initial default journey (Indiranagar -> Cubbon Park) for immediate interactive display
    const defaultJourney = this.routesData?.journeys?.[0];
    if (defaultJourney) {
      this.displayJourney(defaultJourney);
      // Pre-fill inputs with default journey
      const startInput = document.getElementById('explorer-start');
      const destInput = document.getElementById('explorer-dest');
      if (startInput) startInput.value = defaultJourney.from;
      if (destInput) destInput.value = defaultJourney.to;
    }

    this.initialized = true;
  }

  /**
   * Load routes data from assets/data/sample-routes.json with robust fallback
   */
  async loadRouteData() {
    try {
      const response = await fetch('assets/data/sample-routes.json');
      if (response.ok) {
        this.routesData = await response.json();
      } else {
        throw new Error(`Failed to load sample-routes.json (HTTP ${response.status})`);
      }
    } catch (err) {
      console.warn('Using embedded fallback route dataset for RouteView.', err);
      this.routesData = this.getEmbeddedFallbackRoutes();
    }
  }

  /**
   * Initialize or attach the interactive Leaflet map canvas
   */
  initRouteMap() {
    const mapContainer = document.getElementById('route-explorer-map');
    if (!mapContainer) {
      console.warn('Map container #route-explorer-map not found in DOM.');
      return;
    }

    this.mapManager.initMap('route-explorer-map');
  }

  /**
   * Listen for user clicking directly on Leaflet map polylines
   */
  bindPolylineMapCallback() {
    this.mapManager.setOnRouteSelectCallback((routeId) => {
      this.selectRoute(routeId, false);
      Toast.show(`Selected route on map: ${this.getRouteNameById(routeId)}`, 'info', 2500);
    });
  }

  /**
   * Bind Location and Destination search form events
   */
  bindSearchEvents() {
    const searchForm = document.getElementById('explorer-search-form');
    const startInput = document.getElementById('explorer-start');
    const destInput = document.getElementById('explorer-dest');
    const swapBtn = document.getElementById('btn-swap-explorer');
    const validationMsg = document.getElementById('explorer-validation-msg');

    if (!startInput || !destInput) return;

    // Location swap button functionality
    if (swapBtn) {
      swapBtn.addEventListener('click', () => {
        const temp = startInput.value;
        startInput.value = destInput.value;
        destInput.value = temp;

        // Visual 180deg rotation animation
        swapBtn.style.transform = 'rotate(180deg)';
        setTimeout(() => {
          swapBtn.style.transform = '';
        }, 300);

        // Clear error highlights
        startInput.classList.remove('has-error');
        destInput.classList.remove('has-error');
        if (validationMsg) validationMsg.style.display = 'none';

        Toast.show('Swapped starting location and destination.', 'info', 2000);
      });
    }

    // Input error clearing on typing
    [startInput, destInput].forEach((input) => {
      input.addEventListener('input', () => {
        input.classList.remove('has-error');
        if (validationMsg) validationMsg.style.display = 'none';
      });
    });

    // Form submission / Find Routes
    if (searchForm) {
      searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleSearch();
      });
    }
  }

  /**
   * Bind quick sample query pills
   */
  bindQuickQueryPills() {
    const pills = document.querySelectorAll('.explorer-quick-pill');
    const startInput = document.getElementById('explorer-start');
    const destInput = document.getElementById('explorer-dest');

    pills.forEach((pill) => {
      pill.addEventListener('click', () => {
        const from = pill.getAttribute('data-from');
        const to = pill.getAttribute('data-to');

        if (startInput && destInput) {
          startInput.value = from || '';
          destInput.value = to || '';
          startInput.classList.remove('has-error');
          destInput.classList.remove('has-error');
          this.handleSearch();
        }
      });
    });
  }

  /**
   * Execute journey search and validation
   */
  handleSearch() {
    const startInput = document.getElementById('explorer-start');
    const destInput = document.getElementById('explorer-dest');
    const validationMsg = document.getElementById('explorer-validation-msg');

    if (!startInput || !destInput) return;

    const startVal = startInput.value.trim();
    const destVal = destInput.value.trim();

    // 1. Validate empty inputs
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
      this.showValidation(validationMsg, 'Please enter a destination.', 'error');
      destInput.classList.add('has-error');
      destInput.focus();
      return;
    }

    // 2. Match against supported demo journeys
    const match = this.matchJourney(startVal, destVal);

    if (!match) {
      this.showValidation(
        validationMsg,
        `Demo Mode: Arbitrary routing is not connected. SafeRoute AI currently supports two simulated journeys: "Indiranagar → Cubbon Park" and "MG Road → Lalbagh".`,
        'error'
      );
      Toast.show('Unsupported route in demo mode. Try one of the sample routes above.', 'warning', 4500);
      return;
    }

    if (match.isReverse) {
      this.showValidation(
        validationMsg,
        `Demo Notice: Simulated telemetry is currently mapped from "${match.supportedFrom}" to "${match.supportedTo}". Click the Swap button to view these routes.`,
        'error'
      );
      Toast.show(`Click the Swap button to navigate from ${match.supportedFrom} to ${match.supportedTo}.`, 'warning', 4000);
      return;
    }

    // 3. Render matched journey
    this.showValidation(
      validationMsg,
      `Evaluated 3 simulated safe route options for ${match.from} → ${match.to}.`,
      'success'
    );
    this.displayJourney(match);
    Toast.show(`Found 3 route options for ${match.name}.`, 'success', 3000);
  }

  /**
   * Match user text against known demo journeys
   * @param {string} startText
   * @param {string} destText
   */
  matchJourney(startText, destText) {
    const s = (startText || '').toLowerCase().trim();
    const d = (destText || '').toLowerCase().trim();

    // Journey 1: Indiranagar -> Cubbon Park
    const isJ1Start = s.includes('indira');
    const isJ1Dest = d.includes('cubbon');
    if (isJ1Start && isJ1Dest) {
      return this.routesData?.journeys?.find((j) => j.id === 'journey-indiranagar-cubbon') || null;
    }
    if (s.includes('cubbon') && d.includes('indira')) {
      return { isReverse: true, supportedFrom: 'Indiranagar', supportedTo: 'Cubbon Park' };
    }

    // Journey 2: MG Road -> Lalbagh
    const isJ2Start = s.includes('mg road') || s.includes('mg');
    const isJ2Dest = d.includes('lalbagh') || d.includes('lal bagh');
    if (isJ2Start && isJ2Dest) {
      return this.routesData?.journeys?.find((j) => j.id === 'journey-mgroad-lalbagh') || null;
    }
    if ((s.includes('lalbagh') || s.includes('lal bagh')) && (d.includes('mg road') || d.includes('mg'))) {
      return { isReverse: true, supportedFrom: 'MG Road', supportedTo: 'Lalbagh Botanical Garden' };
    }

    return null;
  }

  /**
   * Display a selected journey and render its routes
   * @param {Object} journey
   */
  displayJourney(journey) {
    if (!journey || !Array.isArray(journey.routes)) return;

    this.currentJourney = journey;
    this.currentRoutes = journey.routes;
    this.selectedRouteId = journey.routes[0]?.id || null;

    // Update section title if element exists
    const titleEl = document.getElementById('routes-section-title');
    if (titleEl) {
      titleEl.textContent = `${journey.name} (${journey.routes.length} options)`;
    }

    // Render cards and map
    this.renderRouteCards();
    this.mapManager.renderRoutes(this.currentRoutes, this.selectedRouteId);
  }

  /**
   * Render HTML comparison cards for all routes
   */
  renderRouteCards() {
    const container = document.getElementById('routes-cards-stack');
    if (!container) return;

    container.innerHTML = this.currentRoutes.map((route) => {
      const isSelected = route.id === this.selectedRouteId;
      const categoryModifier = this.getCategoryModifier(route.category);
      const scoreBadgeClass = this.getScoreBadgeClass(route.safetyScore);

      const hazardsHtml = route.hazards && route.hazards.length > 0
        ? route.hazards.map((h) => `<span class="route-tag-chip" title="${h.description}">⚠️ ${h.category}</span>`).join('')
        : `<span class="route-tag-chip" style="color:var(--accent-primary-light);">✓ Zero Active Hazards</span>`;

      const summaryTagsHtml = route.summaryTags && route.summaryTags.length > 0
        ? route.summaryTags.map((t) => `<span class="route-tag-chip">${t}</span>`).join('')
        : '';

      return `
        <article class="route-card ${categoryModifier} ${isSelected ? 'selected' : ''}" data-id="${route.id}">
          <div class="route-card-header">
            <span class="route-category-tag">
              ${this.getCategoryIconSvg(categoryModifier)}
              ${route.category}
            </span>
            <span class="badge badge-demo">SIMULATED DEMO</span>
          </div>

          <div class="route-card-main">
            <div class="route-title-group">
              <h3 class="route-card-name">${route.name}</h3>
              <div class="route-card-submetrics">
                <span>📍 <strong>${route.distanceKm} km</strong></span>
                <span>&bull;</span>
                <span>⏱️ <strong>${route.durationMinutes} mins</strong></span>
              </div>
            </div>

            <div class="route-score-box">
              <span class="score-badge ${scoreBadgeClass}">${route.safetyScore}/100</span>
              <span class="score-badge-label">${this.getSafetyLabel(route.safetyScore)}</span>
            </div>
          </div>

          <div class="route-details-row">
            <div class="route-detail-cell">
              <span class="route-detail-icon lighting">💡</span>
              <div>
                <div><strong>${route.lighting?.score || '—'}</strong> (${route.lighting?.rating || '—'})</div>
                <small style="color:var(--text-muted);font-size:0.68rem;">Street Lighting</small>
              </div>
            </div>

            <div class="route-detail-cell">
              <span class="route-detail-icon ${route.hazardCount > 0 ? 'hazards-warn' : 'hazards-safe'}">
                ${route.hazardCount > 0 ? '⚠️' : '🛡️'}
              </span>
              <div>
                <div><strong>${route.hazardCount} Hazard${route.hazardCount === 1 ? '' : 's'}</strong></div>
                <small style="color:var(--text-muted);font-size:0.68rem;">Reported Incidents</small>
              </div>
            </div>
          </div>

          <div class="route-tags-list">
            ${hazardsHtml}
            ${summaryTagsHtml}
          </div>

          <div class="route-card-action">
            <button class="btn-select-route" type="button" data-route-id="${route.id}">
              ${isSelected ? 'Active Route ✓' : 'Select This Route'}
            </button>
          </div>
        </article>
      `;
    }).join('');

    // Attach card click handlers
    const cards = container.querySelectorAll('.route-card');
    cards.forEach((card) => {
      card.addEventListener('click', (e) => {
        const routeId = card.getAttribute('data-id');
        if (routeId) {
          this.selectRoute(routeId, true);
        }
      });
    });
  }

  /**
   * Set active route and synchronize cards and map
   * @param {string} routeId
   * @param {boolean} updateMap - Whether to instruct MapManager to update polylines
   */
  selectRoute(routeId, updateMap = true) {
    if (!routeId) return;

    this.selectedRouteId = routeId;

    // Update card DOM styling
    const container = document.getElementById('routes-cards-stack');
    if (container) {
      const cards = container.querySelectorAll('.route-card');
      cards.forEach((card) => {
        const cardId = card.getAttribute('data-id');
        const isSelected = cardId === routeId;
        card.classList.toggle('selected', isSelected);

        const btn = card.querySelector('.btn-select-route');
        if (btn) {
          btn.textContent = isSelected ? 'Active Route ✓' : 'Select This Route';
        }
      });
    }

    // Update map polylines
    if (updateMap && this.mapManager) {
      this.mapManager.selectRoute(routeId);
    }
  }

  /**
   * Invalidate map sizing when switching to this view
   */
  invalidateMap() {
    if (this.mapManager) {
      this.mapManager.invalidateSize();
    }
  }

  /**
   * Helper: Category modifier class
   */
  getCategoryModifier(category = '') {
    const c = category.toLowerCase();
    if (c.includes('safe') || c.includes('recommended')) return 'recommended';
    if (c.includes('fast') || c.includes('balanced')) return 'balanced';
    return 'caution';
  }

  /**
   * Helper: Score badge class
   */
  getScoreBadgeClass(score) {
    if (score >= 80) return 'safe';
    if (score >= 65) return 'moderate';
    return 'caution';
  }

  /**
   * Helper: Safety description label
   */
  getSafetyLabel(score) {
    if (score >= 85) return 'Optimal Safe';
    if (score >= 70) return 'Moderate Safety';
    return 'High Caution';
  }

  /**
   * Helper: Category icon SVG
   */
  getCategoryIconSvg(categoryModifier) {
    if (categoryModifier === 'recommended') {
      return `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/></svg>`;
    }
    if (categoryModifier === 'balanced') {
      return `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`;
    }
    return `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;
  }

  /**
   * Helper: Get route name by ID
   */
  getRouteNameById(routeId) {
    const route = this.currentRoutes.find((r) => r.id === routeId);
    return route ? route.name : routeId;
  }

  /**
   * Show inline validation message
   */
  showValidation(element, message, type = 'error') {
    if (!element) return;
    element.className = `explorer-validation-msg ${type}`;
    const icon = type === 'error'
      ? '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>'
      : '<path d="m9 12 2 2 4-4"/><circle cx="12" cy="12" r="10"/>';
    element.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icon}</svg><span>${message}</span>`;
    element.style.display = 'flex';
  }

  /**
   * Embedded fallback dataset guaranteeing zero-crash operation
   */
  getEmbeddedFallbackRoutes() {
    return {
      _meta: { isDemoData: true, notice: 'Simulated fallback demo dataset.' },
      journeys: [
        {
          id: 'journey-indiranagar-cubbon',
          name: 'Indiranagar to Cubbon Park',
          from: 'Indiranagar, Bengaluru',
          to: 'Cubbon Park, Bengaluru',
          startCoordinates: [12.9784, 77.6408],
          destinationCoordinates: [12.9763, 77.5929],
          routes: [
            {
              id: 'route-ic-01',
              name: 'Primary Safe Corridor',
              category: 'Recommended Safe Route',
              distanceKm: 5.6,
              durationMinutes: 19,
              safetyScore: 94,
              safetyLevel: 'safe',
              lighting: { score: '96%', rating: 'Excellent' },
              hazardCount: 0,
              hazards: [],
              summaryTags: ['Well-lit Arterial', 'CCTV Monitored', 'Zero Hazards'],
              coordinates: [
                [12.9784, 77.6408], [12.9768, 77.6365], [12.9752, 77.6312],
                [12.9739, 77.6258], [12.9728, 77.6201], [12.9721, 77.6148],
                [12.9735, 77.6085], [12.9749, 77.6026], [12.9758, 77.5975], [12.9763, 77.5929]
              ]
            },
            {
              id: 'route-ic-02',
              name: 'Transit Arterial via Ulsoor',
              category: 'Fastest Balanced Route',
              distanceKm: 5.1,
              durationMinutes: 16,
              safetyScore: 81,
              safetyLevel: 'safe',
              lighting: { score: '82%', rating: 'Adequate' },
              hazardCount: 1,
              hazards: [{ category: 'Heavy Traffic', description: 'Peak evening bottleneck.' }],
              summaryTags: ['Fastest Route', '1 Minor Hazard'],
              coordinates: [
                [12.9784, 77.6408], [12.9802, 77.6341], [12.9818, 77.6272],
                [12.9831, 77.6205], [12.9825, 77.6136], [12.9808, 77.6074],
                [12.9792, 77.6015], [12.9774, 77.5968], [12.9763, 77.5929]
              ]
            },
            {
              id: 'route-ic-03',
              name: 'Inner Bypass via Domlur Backway',
              category: 'Direct / High Caution Route',
              distanceKm: 4.8,
              durationMinutes: 17,
              safetyScore: 62,
              safetyLevel: 'caution',
              lighting: { score: '54%', rating: 'Poor' },
              hazardCount: 3,
              hazards: [{ category: 'Poor Street Lighting', description: '4 dark poles.' }],
              summaryTags: ['Shortest Distance', '3 Hazards', 'High Caution'],
              coordinates: [
                [12.9784, 77.6408], [12.9742, 77.6382], [12.9698, 77.6315],
                [12.9682, 77.6231], [12.9695, 77.6145], [12.9712, 77.6062],
                [12.9734, 77.5998], [12.9763, 77.5929]
              ]
            }
          ]
        },
        {
          id: 'journey-mgroad-lalbagh',
          name: 'MG Road to Lalbagh Botanical Garden',
          from: 'MG Road, Bengaluru',
          to: 'Lalbagh Botanical Garden, Bengaluru',
          startCoordinates: [12.9756, 77.6066],
          destinationCoordinates: [12.9507, 77.5848],
          routes: [
            {
              id: 'route-ml-01',
              name: 'Boulevard Safe Route',
              category: 'Recommended Safe Route',
              distanceKm: 4.6,
              durationMinutes: 16,
              safetyScore: 92,
              safetyLevel: 'safe',
              lighting: { score: '94%', rating: 'Excellent' },
              hazardCount: 0,
              hazards: [],
              summaryTags: ['Well-lit Arterial', 'Zero Hazards'],
              coordinates: [
                [12.9756, 77.6066], [12.9728, 77.6045], [12.9692, 77.6015],
                [12.9660, 77.5982], [12.9625, 77.5948], [12.9585, 77.5915],
                [12.9548, 77.5880], [12.9507, 77.5848]
              ]
            },
            {
              id: 'route-ml-02',
              name: 'Double Road Arterial',
              category: 'Fastest Balanced Route',
              distanceKm: 4.1,
              durationMinutes: 14,
              safetyScore: 79,
              safetyLevel: 'safe',
              lighting: { score: '76%', rating: 'Moderate' },
              hazardCount: 1,
              hazards: [{ category: 'Waterlogging', description: 'Under flyover ramp.' }],
              summaryTags: ['Fastest Travel Time', '1 Hazard'],
              coordinates: [
                [12.9756, 77.6066], [12.9705, 77.6035], [12.9658, 77.5998],
                [12.9610, 77.5952], [12.9562, 77.5901], [12.9507, 77.5848]
              ]
            },
            {
              id: 'route-ml-03',
              name: 'Market Direct Cut-Through',
              category: 'Direct / High Caution Route',
              distanceKm: 3.7,
              durationMinutes: 18,
              safetyScore: 58,
              safetyLevel: 'caution',
              lighting: { score: '46%', rating: 'Poor' },
              hazardCount: 3,
              hazards: [{ category: 'Unsafe Pedestrian Crossing', description: 'Market cross.' }],
              summaryTags: ['Shortest Distance', '3 Active Hazards', 'High Caution'],
              coordinates: [
                [12.9756, 77.6066], [12.9715, 77.5995], [12.9672, 77.5938],
                [12.9620, 77.5892], [12.9568, 77.5865], [12.9507, 77.5848]
              ]
            }
          ]
        }
      ]
    };
  }
}

