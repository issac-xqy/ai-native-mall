<template>
  <div class="skeleton-wrapper">
    <div class="skeleton-card" v-for="n in count" :key="n">
      <div class="skeleton-image pulse"></div>
      <div class="skeleton-text pulse" :style="{ width: '70%' }"></div>
      <div class="skeleton-text pulse" :style="{ width: '45%' }"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ count?: number }>()
</script>

<style scoped>
.skeleton-wrapper { display: flex; flex-wrap: wrap; gap: 20px; justify-content: center; padding: 20px; }
.skeleton-card { width: 240px; padding: 12px; border-radius: 8px; background: #f5f5f5; }
.skeleton-image { width: 100%; height: 180px; border-radius: 6px; background: #e0e0e0; margin-bottom: 10px; }
.skeleton-text { height: 16px; border-radius: 4px; background: #e0e0e0; margin-bottom: 8px; }
.skeleton-text:last-child { margin-bottom: 0; }
.pulse { animation: skeleton-pulse 1.5s ease-in-out infinite; }
@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
