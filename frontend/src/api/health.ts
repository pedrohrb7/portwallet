import axios from 'axios'

export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN'

export async function fetchHealth(signal?: AbortSignal): Promise<HealthStatus> {
  const response = await axios.get<{ status?: string }>('/actuator/health', {
    signal,
    validateStatus: (status) => status === 200 || status === 503,
  })
  return response.data.status === 'UP' ? 'UP' : 'DOWN'
}
