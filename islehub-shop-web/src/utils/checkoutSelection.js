const CHECKOUT_SELECTED_SKUS_KEY = 'islehub:checkout:selectedSkuIds'

export function saveCheckoutSelectedSkuIds(skuIds) {
  sessionStorage.setItem(CHECKOUT_SELECTED_SKUS_KEY, JSON.stringify(skuIds))
}

export function readCheckoutSelectedSkuIds() {
  try {
    const value = JSON.parse(sessionStorage.getItem(CHECKOUT_SELECTED_SKUS_KEY) || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

export function clearCheckoutSelectedSkuIds() {
  sessionStorage.removeItem(CHECKOUT_SELECTED_SKUS_KEY)
}
