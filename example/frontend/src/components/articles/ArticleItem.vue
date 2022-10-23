<template>
  <div class="article-item">
    <div class="article-item__title">
      {{ article.title }}
    </div>
    <div class="article-item__date">
      {{ formatDate(article.created) }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ArticleBriefFragment } from 'src/gql/graphql'

const props = defineProps<{
  article: ArticleBriefFragment
}>()

// const value = computed(() => useFragment(ArticleBriefFragment, props.article))

function formatDate(date: string) {
  return new Date(date).toLocaleString('ru-RU', {
    dateStyle: 'long',
    timeStyle: 'medium',
  })
}

</script>

<style lang="scss" scoped>
.article-item {
  display: grid;

  padding: 8px 16px;

  text-align: start;

  & &__title {

  }

  & &__date {
    color: grey;
    font-size: 12px;
  }
}
</style>
