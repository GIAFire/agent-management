<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const visitedViews = ref([])

const activePath = computed(() => route.path)

const addVisitedView = (targetRoute) => {
  if (!targetRoute.meta?.title) {
    return
  }

  const exists = visitedViews.value.some((item) => item.path === targetRoute.path)
  if (exists) {
    return
  }

  visitedViews.value.push({
    path: targetRoute.path,
    fullPath: targetRoute.fullPath,
    title: targetRoute.meta.title
  })
}

const toView = (view) => {
  if (view.path !== route.path) {
    router.push(view.fullPath || view.path)
  }
}

const closeView = (view) => {
  if (visitedViews.value.length === 1) {
    return
  }

  const index = visitedViews.value.findIndex((item) => item.path === view.path)
  if (index < 0) {
    return
  }

  visitedViews.value.splice(index, 1)

  if (view.path !== route.path) {
    return
  }

  const nextView = visitedViews.value[index] || visitedViews.value[index - 1] || visitedViews.value[0]
  router.push(nextView?.fullPath || '/agent/manage')
}

watch(
  () => route.fullPath,
  () => addVisitedView(route),
  { immediate: true }
)
</script>

<template>
  <nav class="tags-view">
    <div class="tags-scroll">
      <el-tag
        v-for="view in visitedViews"
        :key="view.path"
        class="tags-item"
        :class="{ active: view.path === activePath }"
        :closable="visitedViews.length > 1"
        :effect="view.path === activePath ? 'dark' : 'plain'"
        @click="toView(view)"
        @close="closeView(view)"
      >
        {{ view.title }}
      </el-tag>
    </div>
  </nav>
</template>
