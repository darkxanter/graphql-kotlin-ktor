<template>
  <div>
    <h5>Subscription Articles</h5>
    <article-item v-for="article in articles" :key="article.id" :article="article"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import ArticleItem from 'src/components/articles/ArticleItem.vue'
import { useLastArticlesSubscription } from 'src/gql/operations'

// const { data } = useSubscription({
//   query: LastArticlesSubscriptionDocument,
// })

const { data } = useLastArticlesSubscription()

const articles = computed(() => data.value?.articles ?? [])

</script>

<style lang="scss" scoped>

</style>
