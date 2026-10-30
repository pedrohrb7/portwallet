export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN'

export async function fetchHealth(signal?: AbortSignal): Promise<HealthStatus> {
  const response = await fetch('/actuator/health', { signal })
  if (!response.ok && response.status !== 503) {
    return 'UNKNOWN'
  }
  const body: { status?: string } = await response.json()
  return body.status === 'UP' ? 'UP' : 'DOWN'
}
