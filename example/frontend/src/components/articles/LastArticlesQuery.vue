<template>
  <div>
    <h5>Query Articles</h5>
    <button style="margin-bottom: 8px" @click="fetchArticles">Fetch</button>
    <article-item v-for="article in articles" :key="article.id" :article="article"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import ArticleItem from 'src/components/articles/ArticleItem.vue'
import { useArticlesQuery } from 'src/gql/operations'
import { useFragment } from 'src/gql'
import { ArticleBriefFragment } from 'src/api/articles'

// const { data, executeQuery } = useQuery({
//   query: LastArticlesQueryDocument,
// })
const { data, executeQuery } = useArticlesQuery()


const fetchArticles = () => executeQuery()

const articles = computed(() => data.value?.articles?.map((x) => useFragment(ArticleBriefFragment, x)) ?? [])

</script>

<style lang="scss" scoped>

</style>
