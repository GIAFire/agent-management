<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  eyebrow: {
    type: String,
    default: 'AgentScope Console'
  }
})
</script>

<template>
  <div class="module-page">
    <section class="intro-section">
      <div>
        <div class="intro-eyebrow">{{ eyebrow }}</div>
        <h1 class="intro-title">{{ title }}</h1>
        <p class="intro-desc">{{ description }}</p>
      </div>
      <div class="intro-actions">
        <slot name="actions" />
      </div>
    </section>

    <section v-if="$slots.filters" class="filter-section">
      <slot name="filters" />
    </section>

    <slot />
  </div>
</template>

