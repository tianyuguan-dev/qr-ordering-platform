/**
 * Show a short-lived toast message (e.g. "Saved", "Deleted").
 * @param {string} message
 * @param { 'success' | 'error' } [type]
 */
export function toast(message, type = 'success') {
  const el = document.createElement('div')
  el.className = 'app-toast app-toast--' + type
  el.textContent = message
  el.style.cssText = 'position:fixed;top:16px;left:50%;transform:translateX(-50%);padding:10px 20px;border-radius:8px;font-size:14px;z-index:9999;box-shadow:0 4px 12px rgba(0,0,0,0.15);'
  el.style.background = type === 'error' ? '#dc3545' : '#28a745'
  el.style.color = '#fff'
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 2500)
}

/**
 * Prominent alert-style toast for important notifications (e.g. new order, ready to serve).
 * Larger, longer duration, higher z-index.
 * @param {string} message
 * @param { 'alert' | 'warning' } [variant] - alert = blue/primary, warning = orange
 */
export function toastAlert(message, variant = 'alert') {
  const el = document.createElement('div')
  el.className = 'app-toast-alert app-toast-alert--' + variant
  el.textContent = message
  const isWarning = variant === 'warning'
  el.style.cssText = [
    'position:fixed;top:24px;left:50%;transform:translateX(-50%);',
    'padding:16px 28px;border-radius:10px;font-size:16px;font-weight:600;',
    'z-index:10000;box-shadow:0 6px 20px rgba(0,0,0,0.25);',
    'border:3px solid rgba(255,255,255,0.9);',
  ].join('')
  el.style.background = isWarning ? '#e65100' : '#1565c0'
  el.style.color = '#fff'
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 6000)
}
