import { createRouter, createWebHistory } from "vue-router";

const routes = [
  { path: "/", name: "feed", component: () => import("../views/Feed.vue") },
  { path: "/login", name: "login", component: () => import("../views/Login.vue") },
  { path: "/note/:id", name: "note-detail", component: () => import("../views/NoteDetail.vue") },
  { path: "/publish", name: "publish", component: () => import("../views/Publish.vue") },
  { path: "/user/:id", name: "user-profile", component: () => import("../views/UserProfile.vue") }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
