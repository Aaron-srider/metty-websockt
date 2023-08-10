<template>
    <div class=""></div>
</template>

<script lang="ts">
import {notify} from '@/ts/Notification';
import {Component, Vue} from 'vue-property-decorator';
import platform from 'platform';
import {tempIdentificationGenerator} from "@/ts/TempName";

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

            this.webSocketClient?.send(JSON.stringify({
                messageType: "HELLO",
                message: {
                    ua: platform.ua,
                    osFamily: platform.os?.family,
                    osArch: platform.os?.architecture,
                    tempName: tempIdentificationGenerator.generate()
                }
            }));
        };

        this.webSocketClient.onerror = (error) => {
            notify(
                null,
                "Error",
                'WebSocketClient error',
                null,
                true,
            )
            console.log('WebSocketClient error: ', error);
        };

        this.webSocketClient.onclose = () => {
            notify(
                null,
                null,
                'WebSocketClient closed',
                null,
                true,
            )
        };

        this.webSocketClient.onmessage = (message) => {
            notify(
                null,
                'Server',
                message.data,
                null,
                true,
            )
        };
    }
}
</script>
<style lang="scss" scoped></style>
