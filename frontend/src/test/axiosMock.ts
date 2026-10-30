import {
  AxiosError,
  type AxiosAdapter,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

export type Route = (
  config: InternalAxiosRequestConfig,
) => { status: number; data?: unknown } | undefined

/** Builds a response the way axios' own adapters do, rejecting statuses refused by `validateStatus`. */
function settle(
  config: InternalAxiosRequestConfig,
  status: number,
  data: unknown,
): Promise<AxiosResponse> {
  const response: AxiosResponse = { data, status, statusText: String(status), headers: {}, config }
  if (config.validateStatus && !config.validateStatus(status)) {
    return Promise.reject(
      new AxiosError(
        `Request failed with status ${status}`,
        AxiosError.ERR_BAD_RESPONSE,
        config,
        null,
        response,
      ),
    )
  }
  return Promise.resolve(response)
}

/**
 * Replaces the transport of an axios instance. The route returns the response for each request; `undefined`
 * simulates a network failure.
 */
export function mockAdapter(instance: AxiosInstance, route: Route): AxiosAdapter {
  const adapter: AxiosAdapter = (config) => {
    const result = route(config)
    if (!result) {
      return Promise.reject(new AxiosError('Network Error', AxiosError.ERR_NETWORK, config))
    }
    return settle(config, result.status, result.data)
  }
  instance.defaults.adapter = adapter
  return adapter
}
