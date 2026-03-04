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
