<template>
    <div class="">
        <div v-for="user in users">
            <el-input v-model=user.input></el-input>
            <el-button @click="send(user.id)"></el-button>
        </div>
    </div>
</template>

<script lang="ts">
import {notify} from '@/ts/Notification';
import {Component, Vue} from 'vue-property-decorator';
import platform from 'platform';
import {tempIdentificationGenerator} from "@/ts/TempName";

@Component({})
export default class WebSocketView extends Vue {
    webSocketClient: WebSocket | null = null;
    users: any[] = []
    me : any = {}
    send(id: string) {
        let t = this.users.find(user => user.id === id)
        if(t != null) {
            let i = t.input
            this.webSocketClient?.send(JSON.stringify({
                messageType: "MESSAGE",
                message: JSON.stringify({
                    fromId: this.me.id,
                    toId: id,
                    msg :i
                })
            }));
        }
    }
    created() {
        // this.webSocketClient = new WebSocket('ws://127.0.0.1:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost1:8080/chat');
        // this.webSocketClient = new WebSocket('ws://172.27.128.180:8080/chat');
        this.webSocketClient = new WebSocket('ws://49.232.155.160:40201/chat');

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
                message: JSON.stringify({
                    ua: platform.ua,
                    osFamily: platform.os?.family,
                    osArch: platform.os?.architecture,
                    tempName: tempIdentificationGenerator.generate()
                })
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

            let isjson = false
            let a
            try {
                a = JSON.parse(message.data)
                isjson = true
            }catch (e) {

            }

            if(isjson) {
                let type = a.messageType
                if(type === "HELLO_BACK") {
                    let b = JSON.parse(a.message)
                    this.users = b
                }
            } else {
                console.log()
            }


        };
    }
}
</script>
<style lang="scss" scoped></style>
