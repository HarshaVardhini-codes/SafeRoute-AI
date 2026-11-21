/**
 * SafeRoute AI - Leaflet Map Manager
 * 
 * Manages Leaflet map instances, OpenStreetMap tile layers, custom hazard markers, 
 * route polylines, popups, and layer toggles.
 */

import { CONFIG } from '../config.js';

export class MapManager {
  constructor() {
    this.map = null;
    this.markersLayer = null;
    this.routeLayer = null;
    this.routeEndpointsLayer = null;
    this.config = CONFIG.MAP;
    this.containerId = null;
    this.mapElement = null;
    this.resizeObserver = null;
    this.resizeHandler = null;
    this.pendingHazards = null;
    this.currentRoutes = [];
    this.selectedRouteId = null;
    this.routePolylines = new Map();
    this.onRouteSelectCallback = null;
  }

  /**
   * Initialize map inside the specified DOM container
   * @param {string} containerId - DOM ID of map container
   * @param {Array<number>} center - [lat, lng]
   * @param {number} zoom - Zoom level
   */
  initMap(containerId = 'dashboard-map', center = null, zoom = null) {
    const mapElement = document.getElementById(containerId);
    if (!mapElement) {
      console.warn(`Map container #${containerId} not found in DOM.`);
      return null;
    }

    const leaflet = window.L;
    if (!leaflet) {
      console.warn('Leaflet.js is loading... scheduling retry.');
      setTimeout(() => {
        if (window.L && !this.map) {
          this.initMap(containerId, center, zoom);
          if (this.pendingHazards) {
            this.renderHazardMarkers(this.pendingHazards);
          }
        }
      }, 300);
      return null;
    }

    if (this.map) {
      if (this.containerId === containerId && this.mapElement === mapElement) {
        this.invalidateSize();
        return this.map;
      }

      if (this.resizeObserver) {
        this.resizeObserver.disconnect();
        this.resizeObserver = null;
      }
      if (this.resizeHandler) {
        window.removeEventListener('resize', this.resizeHandler);
        this.resizeHandler = null;
      }
      this.map.remove();
      this.map = null;
      this.markersLayer = null;
      this.routeLayer = null;
      this.routeEndpointsLayer = null;
      this.routePolylines.clear();
    }

    const defaultCenter = center || this.config.DEFAULT_CENTER;
    const defaultZoom = zoom || this.config.DEFAULT_ZOOM;

    this.map = leaflet.map(mapElement, {
      center: defaultCenter,
      zoom: defaultZoom,
      minZoom: this.config.MIN_ZOOM,
      maxZoom: this.config.MAX_ZOOM,
      zoomControl: false
    });
    this.containerId = containerId;
    this.mapElement = mapElement;

    // Standard OpenStreetMap Tile Layer (No API Key Required)
    leaflet.tileLayer(this.config.TILE_LAYER_URL, {
      attribution: this.config.TILE_ATTRIBUTION,
      maxZoom: this.config.MAX_ZOOM
    }).addTo(this.map);

    leaflet.control.zoom({ position: 'topright' }).addTo(this.map);

    // Layer Groups: routes on bottom, hazard markers above, endpoints on top
    this.routeLayer = leaflet.layerGroup().addTo(this.map);
    this.markersLayer = leaflet.layerGroup().addTo(this.map);
    this.routeEndpointsLayer = leaflet.layerGroup().addTo(this.map);

    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => this.invalidateSize());
      this.resizeObserver.observe(mapElement);
    } else {
      this.resizeHandler = () => this.invalidateSize();
      window.addEventListener('resize', this.resizeHandler);
    }

    this.invalidateSize();

    return this.map;
  }

  /**
   * Invalidate map sizing to avoid tile clipping
   */
  invalidateSize() {
    if (!this.map) return;
    requestAnimationFrame(() => {
      if (this.map) this.map.invalidateSize({ pan: false });
    });
  }

  /**
   * Render hazard markers on the map
   * @param {Array<Object>} hazards - List of hazard objects
   */
  renderHazardMarkers(hazards = []) {
    this.pendingHazards = hazards;
    if (!this.map || !this.markersLayer || !window.L) return;

    this.markersLayer.clearLayers();

    hazards.forEach((hazard) => {
      if (!hazard.coordinates || hazard.coordinates.length !== 2) return;

      const isCritical = hazard.severity === 'high' || hazard.severity === 'critical';
      const markerColorClass = isCritical ? 'marker-danger' : 'marker-caution';
      const iconSvg = this.getHazardIconSvg(hazard.typeId);

      // Custom DivIcon for modern glowing marker
      const customIcon = window.L.divIcon({
        className: 'custom-hazard-marker-container',
        html: `
          <div class="custom-hazard-pin ${markerColorClass}" title="${hazard.title}">
            <div class="pin-icon">${iconSvg}</div>
            <div class="pin-pulse"></div>
          </div>
        `,
        iconSize: [34, 34],
        iconAnchor: [17, 34],
        popupAnchor: [0, -32]
      });

      const marker = window.L.marker(hazard.coordinates, { icon: customIcon });

      // Build modern dark popup content
      const popupHtml = `
        <div class="map-popup-card">
          <div class="map-popup-header">
            <span class="badge ${isCritical ? 'badge-red' : 'badge-amber'}">
              ${hazard.category}
            </span>
            <span class="badge badge-demo">DEMO</span>
          </div>
          <h4 class="map-popup-title">${hazard.title}</h4>
          <p class="map-popup-location">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/><circle cx="12" cy="10" r="3"/></svg>
            ${hazard.location}
          </p>
          <div class="map-popup-footer">
            <span class="time-ago">${hazard.reportTime || 'Recently'}</span>
            <span class="confirm-count">${hazard.confirmations || 0} confirmations</span>
          </div>
        </div>
      `;

      marker.bindPopup(popupHtml, {
        className: 'dark-leaflet-popup',
        maxWidth: 280
      });

      this.markersLayer.addLayer(marker);
    });
  }

  /**
   * Render route polylines, origin/destination markers, and auto-frame bounds
   * @param {Array<Object>} routes - Array of route objects from sample-routes.json
   * @param {string|null} selectedRouteId - ID of currently selected route
   */
  renderRoutes(routes = [], selectedRouteId = null) {
    if (!this.map || !window.L) return;

    this.clearRoutes();

    if (!Array.isArray(routes) || routes.length === 0) return;

    this.currentRoutes = routes;
    this.selectedRouteId = selectedRouteId || routes[0].id;

    const allPoints = [];

    // Draw polylines for all routes
    routes.forEach((route) => {
      if (!Array.isArray(route.coordinates) || route.coordinates.length < 2) return;

      const isSelected = route.id === this.selectedRouteId;
      const style = this.getRouteStyle(route, isSelected);

      const polyline = window.L.polyline(route.coordinates, style);

      // Tooltip on hover
      const tooltipContent = `
        <div style="font-family:sans-serif;font-size:12px;color:#F8FAFC;background:rgba(19,27,46,0.95);padding:6px 10px;border-radius:6px;border:1px solid rgba(255,255,255,0.15);box-shadow:0 4px 12px rgba(0,0,0,0.5);">
          <div style="font-weight:600;color:${isSelected ? '#34D399' : '#CBD5E1'}">${route.name}</div>
          <div style="color:#94A3B8;margin-top:2px;">Safety: <strong style="color:#F8FAFC;">${route.safetyScore}/100</strong> &bull; ${route.distanceKm} km &bull; ${route.durationMinutes} min</div>
          <div style="font-size:10px;color:#F59E0B;margin-top:2px;font-weight:600;">SIMULATED DEMO ROUTE</div>
        </div>
      `;
      polyline.bindTooltip(tooltipContent, { sticky: true, opacity: 1, className: 'leaflet-dark-tooltip' });

      // Click to select route
      polyline.on('click', () => {
        this.selectRoute(route.id);
        if (typeof this.onRouteSelectCallback === 'function') {
          this.onRouteSelectCallback(route.id);
        }
      });

      this.routeLayer.addLayer(polyline);
      this.routePolylines.set(route.id, polyline);

      route.coordinates.forEach((pt) => allPoints.push(pt));
    });

    // Bring selected route to front
    const selectedPolyline = this.routePolylines.get(this.selectedRouteId);
    if (selectedPolyline) {
      selectedPolyline.bringToFront();
    }

    // Render Origin (A) and Destination (B) endpoint markers
    const activeRoute = routes.find((r) => r.id === this.selectedRouteId) || routes[0];
    if (activeRoute && Array.isArray(activeRoute.coordinates) && activeRoute.coordinates.length >= 2) {
      const startCoord = activeRoute.coordinates[0];
      const endCoord = activeRoute.coordinates[activeRoute.coordinates.length - 1];

      // Origin Marker (A - Green)
      const startIcon = this.createEndpointIcon('start', 'A');
      const startMarker = window.L.marker(startCoord, { icon: startIcon, zIndexOffset: 1000 });
      startMarker.bindPopup(`
        <div style="font-family:sans-serif;font-size:12px;color:#F8FAFC;padding:4px 6px;">
          <div style="font-weight:bold;color:#34D399;">Starting Location (A)</div>
          <div style="color:#CBD5E1;margin-top:2px;">${activeRoute.from || 'Origin'}</div>
          <div style="font-size:10px;color:#F59E0B;margin-top:4px;font-weight:600;">SIMULATED DEMO DATA</div>
        </div>
      `, { className: 'dark-leaflet-popup' });
      this.routeEndpointsLayer.addLayer(startMarker);

      // Destination Marker (B - Red)
      const endIcon = this.createEndpointIcon('destination', 'B');
      const endMarker = window.L.marker(endCoord, { icon: endIcon, zIndexOffset: 1000 });
      endMarker.bindPopup(`
        <div style="font-family:sans-serif;font-size:12px;color:#F8FAFC;padding:4px 6px;">
          <div style="font-weight:bold;color:#F87171;">Destination (B)</div>
          <div style="color:#CBD5E1;margin-top:2px;">${activeRoute.to || 'Destination'}</div>
          <div style="font-size:10px;color:#F59E0B;margin-top:4px;font-weight:600;">SIMULATED DEMO DATA</div>
        </div>
      `, { className: 'dark-leaflet-popup' });
      this.routeEndpointsLayer.addLayer(endMarker);
    }

    // Auto-fit bounds to frame all route alternatives
    if (allPoints.length > 0) {
      const bounds = window.L.latLngBounds(allPoints);
      this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
    }
  }

  /**
   * Highlight a specific route by ID and mute alternatives
   * @param {string} routeId - ID of route to highlight
   */
  selectRoute(routeId) {
    if (!this.map || !this.routePolylines || !this.routePolylines.has(routeId)) return;

    this.selectedRouteId = routeId;

    this.currentRoutes.forEach((route) => {
      const polyline = this.routePolylines.get(route.id);
      if (!polyline) return;

      const isSelected = route.id === routeId;
      const style = this.getRouteStyle(route, isSelected);
      polyline.setStyle(style);

      if (isSelected) {
        polyline.bringToFront();
      }
    });
  }

  /**
   * Clear all route polylines and origin/destination markers from the map
   */
  clearRoutes() {
    if (this.routeLayer) {
      this.routeLayer.clearLayers();
    }
    if (this.routeEndpointsLayer) {
      this.routeEndpointsLayer.clearLayers();
    }
    if (this.routePolylines) {
      this.routePolylines.clear();
    }
    this.currentRoutes = [];
    this.selectedRouteId = null;
  }

  /**
   * Get polyline styling based on selection state and route safety characteristics
   * @param {Object} route - Route data object
   * @param {boolean} isSelected - Whether this route is currently active
   * @returns {Object} Leaflet polyline style object
   */
  getRouteStyle(route, isSelected = false) {
    if (isSelected) {
      return {
        color: '#10B981', // Emerald for selected route
        weight: 6,
        opacity: 0.95,
        lineCap: 'round',
        lineJoin: 'round'
      };
    }

    const isCaution = route.safetyLevel === 'caution' || (route.safetyScore && route.safetyScore < 70);
    if (isCaution) {
      return {
        color: '#F59E0B', // Amber for caution routes
        weight: 4,
        opacity: 0.75,
        lineCap: 'round',
        lineJoin: 'round'
      };
    }

    return {
      color: '#64748B', // Muted slate for alternative routes
      weight: 4,
      opacity: 0.7,
      lineCap: 'round',
      lineJoin: 'round'
    };
  }

  /**
   * Create custom DivIcon for origin (A) and destination (B) markers
   * @param {string} type - 'start' or 'destination'
   * @param {string} letter - 'A' or 'B'
   */
  createEndpointIcon(type = 'start', letter = 'A') {
    const isStart = type === 'start';
    const bg = isStart ? '#10B981' : '#EF4444';
    const shadow = isStart ? 'rgba(16, 185, 129, 0.5)' : 'rgba(239, 68, 68, 0.5)';
    const textColor = isStart ? '#0B0F19' : '#FFFFFF';

    return window.L.divIcon({
      className: 'custom-endpoint-marker-container',
      html: `
        <div style="
          width: 30px;
          height: 30px;
          background: ${bg};
          color: ${textColor};
          border: 2px solid #FFFFFF;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          font-size: 13px;
          box-shadow: 0 0 12px ${shadow};
          cursor: pointer;
        ">${letter}</div>
      `,
      iconSize: [30, 30],
      iconAnchor: [15, 15],
      popupAnchor: [0, -18]
    });
  }

  /**
   * Register a callback listener when a user clicks a route polyline directly
   * @param {Function} callback - fn(routeId)
   */
  setOnRouteSelectCallback(callback) {
    this.onRouteSelectCallback = callback;
  }

  /**
   * SVG Icon helper based on hazard type
   */
  getHazardIconSvg(typeId) {
    switch (typeId) {
      case 'poor_lighting':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2v1"/><path d="M12 7a5 5 0 0 0-5 5c0 1.5.8 2.8 2 3.6V17h6v-1.4c1.2-.8 2-2.1 2-3.6a5 5 0 0 0-5-5z"/></svg>`;
      case 'road_accident':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;
      case 'waterlogging':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>`;
      case 'broken_sidewalk':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="4" y1="4" x2="20" y2="20"/><path d="m4 12 16 8"/><path d="m12 4 8 16"/></svg>`;
      case 'unsafe_crossing':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 15h16"/><path d="M4 9h16"/><path d="M10 3v18"/><path d="M14 3v18"/></svg>`;
      case 'isolated_area':
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m18 18-6-6m0 0L6 6m6 6 6-6m-6 6-6 6"/></svg>`;
      default:
        return `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;
    }
  }

  /**
   * Center map on given coordinates
   */
  centerOn(lat, lng, zoom = 14) {
    if (this.map) {
      this.map.flyTo([lat, lng], zoom, { duration: 1.2 });
    }
  }
}
