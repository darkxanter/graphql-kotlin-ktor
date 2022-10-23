import { App } from '@vue/runtime-core'
import urql, {
  cacheExchange,
  ClientOptions,
  dedupExchange,
  fetchExchange,
  getOperationName,
  subscriptionExchange,
} from '@urql/vue'
import { devtoolsExchange } from '@urql/devtools'
import { persistedFetchExchange } from '@urql/exchange-persisted-fetch'
import { requestPolicyExchange } from '@urql/exchange-request-policy'
import { createClient as createWSClient } from 'graphql-ws'
import { Exchange } from '@urql/core/dist/types/types'
import { map, pipe } from 'wonka'

export function setupGraphql(app: App) {
  const wsUrl = `${window.location.origin.replace('http', 'ws')}/subscriptions`

  const wsClient = createWSClient({
    url: wsUrl,
  })

  app.use(urql, {
    url: '/graphql',
    fetchOptions() {
      const userId = 3
      return {
        headers: { 'x-user-id': `${userId}` },
      }
    },
    exchanges: [
      devtoolsExchange,
      addOperationName,
      dedupExchange,
      requestPolicyExchange({
        // after 1 minute upgrade to cache-and-network
        ttl: 60 * 1000,
      }),
      cacheExchange,
      persistedFetchExchange({
        preferGetForPersistedQueries: false,
      }),
      fetchExchange,
      subscriptionExchange({
        forwardSubscription: (operation) => ({
          subscribe: (sink) => ({
            unsubscribe: wsClient.subscribe(operation, sink),
          }),
        }),
      }),
    ],
  } as ClientOptions)
}

const addOperationName: Exchange = ({ client, forward }) => {
  return operations$ => {
    return pipe(operations$, map(op => {
      if (op.context.preferGetMethod) {
        return op
      }
      if (op.kind === 'query' || op.kind === 'mutation') {
        const operationName = getOperationName(op.query)
        if (operationName) {
          const url = new URL(op.context.url, op.context.url.startsWith('http') ? undefined : window.location.origin)
          url.searchParams.append('operationName', operationName)
          op.context.url = url.toString()
        }
      }
      return op
    }), forward)
  }
}
