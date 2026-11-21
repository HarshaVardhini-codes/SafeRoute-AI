/**
 * SafeRoute AI - Frontend Application Configuration
 * 
 * Central configuration for API endpoints, Map defaults, and demo toggles.
 * Prepared for future Java Spring Boot REST API integration.
 */

export const CONFIG = {
  // Application Meta
  APP_NAME: 'SafeRoute AI',
  TAGLINE: "Don't just find the shortest route. Find a safer way.",
  VERSION: '1.0.0-dev',
  
  // Demonstration Mode Flag (Rule 8 Compliance: Distinguish Demo from Verified Data)
  DEMO_MODE: true,

  // Java Spring Boot REST Backend Configuration
  // When Member 1 & 3 deploy the backend, set DEMO_MODE: false and verify the port below.
  API: {
    BASE_URL: 'http://localhost:8080/api/v1',
    TIMEOUT_MS: 8000,
    ENDPOINTS: {
      ROUTES: '/routes/evaluate',
      HAZARDS: '/hazards',
      ALERTS: '/alerts/active',
      SAFETY_STATS: '/stats/overview',
      USER_PROFILE: '/user/profile'
    }
  },

  // Leaflet.js & OpenStreetMap Configuration
  MAP: {
    // Default City Center Coordinates [Lat, Lng] (Bengaluru)
    DEFAULT_CENTER: [12.9716, 77.5946],
    DEFAULT_ZOOM: 13,
    MIN_ZOOM: 5,
    MAX_ZOOM: 19,
    // Standard OpenStreetMap Tile Layer (No API Key Required)
    TILE_LAYER_URL: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    TILE_ATTRIBUTION: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors'
  },

  // Supported Safety Hazard Categories
  HAZARD_CATEGORIES: [
    { id: 'poor_lighting', label: 'Poor Street Lighting', icon: 'lightbulb-off', defaultSeverity: 'medium' },
    { id: 'isolated_area', label: 'Isolated Area', icon: 'map-pin-off', defaultSeverity: 'high' },
    { id: 'road_accident', label: 'Road Accident', icon: 'alert-triangle', defaultSeverity: 'critical' },
    { id: 'waterlogging', label: 'Waterlogging', icon: 'droplet', defaultSeverity: 'medium' },
    { id: 'broken_sidewalk', label: 'Broken Sidewalk', icon: 'slash', defaultSeverity: 'low' },
    { id: 'unsafe_crossing', label: 'Unsafe Pedestrian Crossing', icon: 'footprints', defaultSeverity: 'medium' },
    { id: 'heavy_traffic', label: 'Heavy Traffic', icon: 'truck', defaultSeverity: 'low' }
  ],

  // Safety Score Classification Thresholds (0 - 100)
  SAFETY_THRESHOLDS: {
    SAFE_MIN: 80,      // 80 - 100: Emerald (Recommended)
    MODERATE_MIN: 60,  // 60 - 79: Amber (Caution)
    RISKY_MAX: 59      // 0 - 59: Red (High Risk / Avoid)
  }
};
