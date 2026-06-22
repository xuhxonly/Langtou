<template>
  <div class="user-profile">
    <div class="header">
      <img :src="user.avatar || '/default-avatar.png'" :alt="user.nickname" class="avatar" />
      <div class="info">
        <h1>{{ user.nickname || "用户" }}</h1>
        <p>{{ user.bio || "这个人很懒，什么都没留下" }}</p>
      </div>
    </div>
    <div class="stats">
      <span>关注 {{ user.followingCount || 0 }}</span>
      <span>粉丝 {{ user.followerCount || 0 }}</span>
      <span>获赞 {{ user.likeCount || 0 }}</span>
    </div>
    <div class="notes">
      <h3>发布的笔记</h3>
      <div v-for="note in notes" :key="note.id" class="note-item">
        {{ note.title }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getUserProfile } from "../api";

const route = useRoute();
const user = ref({});
const notes = ref([]);

onMounted(async () => {
  try {
    const res = await getUserProfile(route.params.id);
    user.value = res.data?.user || {};
    notes.value = res.data?.notes || [];
  } catch (e) {
    console.error("加载用户信息失败:", e);
  }
});
</script>

<style scoped>
.user-profile { max-width: 600px; margin: 0 auto; padding: 20px; }
.header { display: flex; gap: 16px; align-items: center; }
.avatar { width: 80px; height: 80px; border-radius: 50%; }
.stats { display: flex; gap: 24px; margin: 20px 0; }
.note-item { padding: 12px; border: 1px solid #eee; border-radius: 8px; margin-bottom: 8px; }
</style>
