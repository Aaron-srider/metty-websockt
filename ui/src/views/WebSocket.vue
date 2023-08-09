<template>
    <div class=""></div>
</template>

<script lang="ts">
import { Notification } from 'element-ui';
import { Component, Vue } from 'vue-property-decorator';
import platform from 'platform';
@Component({})
export default class WebSocketView extends Vue {
    webSocketClient: WebSocket | null = null;

    created() {
        this.webSocketClient = new WebSocket('ws://127.0.0.1:8080/chat');

        this.webSocketClient.onopen = () => {
            console.log('WebSocketClient connected');
            console.log('client os:', platform.os);
            console.log('client name:', platform.name);
            console.log('client description:', platform.description);
            console.log('client manufacturer:', platform.manufacturer);
            console.log('client layout:', platform.layout);
            console.log('client prerelease:', platform.prerelease);
            console.log('client product:', platform.product);
            console.log('client version:', platform.version);
            console.log('client ua:', platform.ua);
        };

        this.webSocketClient.onerror = (error) => {
            console.log('WebSocketClient error: ', error);
        };

        this.webSocketClient.onclose = () => {
            console.log('WebSocketClient closed');
        };

        this.webSocketClient.onmessage = (message) => {
            Notification.info('Server: ' + message.data);
        };
    }
}
</script>
<style lang="scss" scoped></style>
