<template>
  <div class="new-article-form">
    <div class="new-article-form__form">
      <label>
        Title
      </label>
      <input v-model.trim="title"/>
      <label>
        Content
      </label>
      <textarea v-model.trim="content" rows="10"/>
    </div>
    <button @click="submit">Submit</button>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useNewArticleMutation } from 'src/gql/operations'

const title = ref('')
const content = ref('')

const { executeMutation: newArticle } = useNewArticleMutation()

function submit() {
  if (title.value && content.value) {
    newArticle({
      input: {
        title: title.value,
        content: content.value,
      },
    })
  }
}

</script>

<style lang="scss" scoped>
.new-article-form {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;

  & &__form {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: 16px;
  }
}
</style>
