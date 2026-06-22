<template>
  <div class="feed">
    <h1>发现</h1>
    <div class="feed-list">
      <div v-for="note in notes" :key="note.id" class="note-card">
        <h3>{{ note.title }}</h3>
        <p>{{ note.content }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getFeed } from "../api";

const notes = ref([]);

onMounted(async () => {
  try {
    const res = await getFeed();
    notes.value = res.data || [];
  } catch (e) {
    console.error("加载 Feed 失败:", e);
  }
});
</script>

<style scoped>
.feed { max-width: 800px; margin: 0 auto; padding: 20px; }
.feed-list { display: grid; gap: 16px; }
.note-card { padding: 16px; border: 1px solid #eee; border-radius: 8px; }
</style>
