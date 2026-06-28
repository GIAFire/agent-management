export const asArray = (value) => {
  if (Array.isArray(value)) {
    return value
  }
  if (Array.isArray(value?.records)) {
    return value.records
  }
  if (Array.isArray(value?.data)) {
    return value.data
  }
  return []
}

export const formatTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export const statusText = (value, labels = {}) => {
  return labels[value] || labels[String(value)] || (value ? '启用' : '停用')
}

