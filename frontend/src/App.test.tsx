import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function mockHealthResponse(status: number, body: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status })))
}

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the demo banner', () => {
    mockHealthResponse(200, { status: 'UP' })
    render(<App />)
    expect(screen.getByTestId('demo-banner')).toHaveTextContent(
      'Ambiente de demonstração, sem dinheiro real',
    )
  })

  it('shows the API as online when health is UP', async () => {
    mockHealthResponse(200, { status: 'UP' })
    render(<App />)
    expect(await screen.findByText('Online')).toBeInTheDocument()
  })

  it('shows the API as down when health is DOWN', async () => {
    mockHealthResponse(503, { status: 'DOWN' })
    render(<App />)
    expect(await screen.findByText('Fora do ar')).toBeInTheDocument()
  })
})
