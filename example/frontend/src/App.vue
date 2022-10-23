<script setup lang="ts">
import NewArticleForm from 'src/components/articles/NewArticleForm.vue'
import LastArticlesQuery from 'src/components/articles/LastArticlesQuery.vue'
import LastArticlesSubscription from 'src/components/articles/LastArticlesSubscription.vue'
</script>

<template>
  <div class="container">
    <new-article-form class="new-article"/>
    <last-articles-query class="last-articles"/>
    <last-articles-subscription class="last-articles"/>
  </div>
</template>

<style scoped lang="scss">
.container {
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr 1fr;
}

.new-article {
  grid-row: span 2;
  display: flex;
  align-content: center;
  place-content: center;
}

.last-articles {
  min-height: 200px;
  width: 350px;
}

</style>
