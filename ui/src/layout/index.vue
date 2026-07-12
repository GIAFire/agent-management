<script setup>
import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar/index.vue'
</script>

<template>
  <div class="app-wrapper">
    <Sidebar />
    <div class="main-container">
      <Navbar />
      <AppMain />
    </div>
  </div>
</template>
