import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import urql, { ClientOptions, dedupExchange, fetchExchange, cacheExchange, subscriptionExchange } from '@urql/vue'
import { devtoolsExchange } from '@urql/devtools'
import { persistedFetchExchange } from '@urql/exchange-persisted-fetch'
import { createClient as createWSClient } from 'graphql-ws';

const app = createApp(App)

const wsUrl = `${window.location.origin.replace('http', 'ws')}/subscriptions`
console.log({ wsUrl })

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
  requestPolicy: 'cache-and-network',
  exchanges: [
    devtoolsExchange,
    dedupExchange,
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

// provideClient(client)
app.mount('#app')
