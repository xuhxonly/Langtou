<template>
  <div class="note-detail">
    <button @click="$router.back()">返回</button>
    <h1>{{ note.title }}</h1>
    <div class="content">{{ note.content }}</div>
    <div class="actions">
      <button @click="handleLike">点赞 ({{ likeCount }})</button>
      <button @click="handleCollect">收藏</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getNoteDetail, likeNote, collectNote } from "../api";

const route = useRoute();
const note = ref({});
const likeCount = ref(0);

onMounted(async () => {
  try {
    const res = await getNoteDetail(route.params.id);
    note.value = res.data || {};
    likeCount.value = note.value.likeCount || 0;
  } catch (e) {
    console.error("加载笔记详情失败:", e);
  }
});

async function handleLike() {
  try {
    await likeNote(route.params.id);
    likeCount.value++;
  } catch (e) {
    console.error("点赞失败:", e);
  }
}

async function handleCollect() {
  try {
    await collectNote(route.params.id);
    alert("收藏成功");
  } catch (e) {
    console.error("收藏失败:", e);
  }
}
</script>

<style scoped>
.note-detail { max-width: 800px; margin: 0 auto; padding: 20px; }
.content { margin: 20px 0; line-height: 1.6; }
.actions { display: flex; gap: 12px; }
button { padding: 8px 16px; border: 1px solid #ddd; border-radius: 4px; background: #fff; cursor: pointer; }
</style>
