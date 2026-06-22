<template>
  <div class="login-page">
    <h1>登录</h1>
    <form @submit.prevent="handleLogin">
      <input v-model="form.phone" placeholder="手机号" />
      <input v-model="form.code" placeholder="验证码" />
      <button type="submit">登录</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { login } from "../api";

const router = useRouter();
const form = ref({ phone: "", code: "" });

async function handleLogin() {
  try {
    const res = await login(form.value);
    localStorage.setItem("token", res.data.token);
    router.push("/");
  } catch (e) {
    alert("登录失败");
  }
}
</script>

<style scoped>
.login-page { max-width: 400px; margin: 100px auto; padding: 20px; }
form { display: flex; flex-direction: column; gap: 12px; }
input { padding: 10px; border: 1px solid #ddd; border-radius: 4px; }
button { padding: 10px; background: #ff2442; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
</style>
