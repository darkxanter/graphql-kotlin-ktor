import { App } from '@vue/runtime-core'
import urql, { ClientOptions, dedupExchange, fetchExchange, cacheExchange, subscriptionExchange } from '@urql/vue'
import { devtoolsExchange } from '@urql/devtools'
import { persistedFetchExchange } from '@urql/exchange-persisted-fetch'
import { requestPolicyExchange } from '@urql/exchange-request-policy'
import { createClient as createWSClient } from 'graphql-ws';

export function setupGraphql(app: App) {
  const wsUrl = `${window.location.origin.replace('http', 'ws')}/subscriptions`

  const wsClient = createWSClient({
    url: wsUrl,
  });

  app.use(urql, {
    url: '/graphql',
    fetchOptions: () => {
      const userId = 3
      return {
        headers: { 'x-user-id': `${userId}` },
      }
    },
    exchanges: [
      devtoolsExchange,
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
