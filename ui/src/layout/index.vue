<script setup>
import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar/index.vue'
import TagsView from './components/TagsView.vue'
</script>

<template>
  <div class="app-wrapper">
    <Navbar />
    <div class="app-body">
      <Sidebar />
      <div class="main-container">
        <TagsView />
        <AppMain />
      </div>
    </div>
  </div>
</template>
