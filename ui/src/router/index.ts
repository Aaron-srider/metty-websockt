import Vue from 'vue';
import VueRouter, { RouteConfig } from 'vue-router';
import WebSocketView from "@/views/WebSocket.vue";
Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
    {
        path: '/',
        redirect: '/websocket',
    },
   {
        path: '/websocket',
        name: 'websocket',
        component: WebSocketView,
    },
];

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes,
});

export default router;
