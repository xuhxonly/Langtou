<template>
  <div class="publish">
    <h1>发布笔记</h1>
    <form @submit.prevent="handlePublish">
      <input v-model="form.title" placeholder="标题" maxlength="20" required />
      <textarea v-model="form.content" placeholder="分享你的想法..." required></textarea>
      <button type="submit">发布</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { publishNote } from "../api";

const router = useRouter();
const form = ref({ title: "", content: "" });

async function handlePublish() {
  try {
    await publishNote(form.value);
    alert("发布成功");
    router.push("/");
  } catch (e) {
    alert("发布失败");
  }
}
</script>

<style scoped>
.publish { max-width: 600px; margin: 40px auto; padding: 20px; }
form { display: flex; flex-direction: column; gap: 16px; }
input, textarea { padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
textarea { min-height: 200px; resize: vertical; }
button { padding: 12px; background: #ff2442; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; }
</style>
