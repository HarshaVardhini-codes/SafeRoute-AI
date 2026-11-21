/**
 * SafeRoute AI - Toast Notification Component
 */

export class Toast {
  static show(message, type = 'info', duration = 3500) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      container.setAttribute('aria-live', 'polite');
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icon = type === 'success' 
      ? '<span class="pulse-dot emerald"></span>' 
      : (type === 'warning' ? '<span class="pulse-dot amber"></span>' : '<span style="color:var(--color-info)">ℹ</span>');

    toast.innerHTML = `
      ${icon}
      <span class="toast-text">${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('toast-fade-out');
      setTimeout(() => {
        if (toast.parentNode) {
          toast.parentNode.removeChild(toast);
        }
      }, 300);
    }, duration);
  }
}

