export function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function addDays(date, days) {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

export function formatShortDay(value) {
  return new Date(`${value}T00:00:00`).toLocaleDateString([], {
    day: 'numeric',
    month: 'short',
  })
}

export function formatDateTime(value) {
  return new Date(value).toLocaleString([], {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}