<template>
    <div class="">
        <div>
            id: {{me.id}} name: {{me.tempName}}
        </div>
        <div v-for="user in users">
            <div class="flex " style="width: 100%">
                <div class="flexg1"><span>{{ user.id }}</span></div>
                <div class="flexg1">
                    <el-input v-model=user.input></el-input>
                </div>
                <div class="flexg1">
                    <el-button @click="send(user.id)">send</el-button>
                </div>

            </div>

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
    me: any = {}

    send(id: string) {
        let t = this.users.find(user => user.id === id)
        if (t != null) {
            let i = t.input
            this.webSocketClient?.send(JSON.stringify({
                messageType: "MESSAGE",
                message: JSON.stringify({
                    fromId: this.me.id,
                    toId: id,
                    msg: i
                })
            }));
        }
    }

    created() {
        this.webSocketClient = new WebSocket('ws://127.0.0.1:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost1:8080/chat');
        // this.webSocketClient = new WebSocket('ws://172.27.128.180:8080/chat');
        // this.webSocketClient = new WebSocket('ws://49.232.155.160:40201/chat');

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


            let isjson = false
            let a
            try {
                a = JSON.parse(message.data)
                isjson = true
            } catch (e) {

            }

            if (isjson) {
                debugger
                let type = a.messageType
                if (type === "HELLO_BACK") {
                    let b = JSON.parse(a.message)
                    console.log("come into room, roommates: ")
                    b.user_list.forEach(
                        (user: any) => {
                            console.log({
                                id: user.id,
                                tempName: user.tempName
                            })
                        }
                    )
                    console.log("your identity: ", b.me)
                    this.users = b.user_list
                    this.me = b.me
                }
                else if (type === "ALIVE_PING") {
                    console.log("receive alive ping")
                    this.webSocketClient?.send(JSON.stringify({
                        messageType: "ALIVE_ACK"
                    }));
                }
                else if (type === "HELLO") {

                    let b = JSON.parse(a.message)
                    console.log("new user comes into room: ", b)
                    let newFri = b
                    let e = this.users.find((user) => {
                        return user.id === newFri.id
                    })
                    if (e == null) {
                        this.users.push(newFri)
                    }
                } else if(type === "LEAVE") {
                    let b = JSON.parse(a.message)
                    console.log("user leaves room: ", b)
                    this.users = this.users.filter((user) => {
                        return user.id !== b.id
                    })
                }
            }


        };
    }
}
</script>
<style lang="scss" scoped>

@import "~@/style/common-style.scss";
</style>
