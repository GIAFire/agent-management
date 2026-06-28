<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppLogo from '@/layout/components/AppLogo.vue'
import AppMenu from '@/layout/components/AppMenu.vue'
import UserSection from '@/layout/components/UserSection.vue'

const route = useRoute()
const pageTitle = computed(() => route.meta?.title || '智能体平台')
</script>

<template>
  <div class="app-layout">
    <header class="layout-header">
      <AppLogo />
      <AppMenu />
      <UserSection />
    </header>

    <main class="layout-main" :aria-label="pageTitle">
      <RouterView v-slot="{ Component }">
        <Transition name="slide-right" mode="out-in" appear>
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

