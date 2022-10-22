<template>
  <div v-for="article in articles">
    {{ article.title }} - {{ formatDate(article.created)}}
  </div>
</template>

<script lang="ts" setup>
import { useQuery, useSubscription } from '@urql/vue'
import { computed } from 'vue'
import {
  ArticleBriefFragment,
  LastArticlesQueryDocument,
  LastArticlesSubscriptionDocument,
} from 'src/components/articles/api/queries'
import { useFragment } from 'src/gql'

// const { data, executeQuery } = useQuery({
//   query: LastArticlesQueryDocument,
// })

const { data, executeSubscription } = useSubscription({
  query: LastArticlesSubscriptionDocument,
})

const articles = computed(() => data.value?.articles?.map((x) => {
  return useFragment(ArticleBriefFragment, x)
}) ?? [])

function formatDate(date: string) {
  return new Date(date).toLocaleString('ru-RU', {
    dateStyle: 'long',
    timeStyle: 'medium',
  })
}

</script>

<style lang="scss" scoped>

</style>
