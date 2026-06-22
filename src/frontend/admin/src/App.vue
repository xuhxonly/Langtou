<script setup lang="ts">
// 根组件：仅承载路由出口
</script>

<template>
  <router-view />
</template>

<style lang="scss">
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background-color: var(--lt-bg-primary);
  color: var(--lt-text-primary);
}
</style>
