<template>
    <div class="">
        <div class="mgb20">
            <div>
                ME: {{ me.device }}-{{ me.tempName }}-{{ me.id }}
            </div>
        </div>
        <template v-for="user in users">
            <div class="border" style="margin-bottom: 20px;">
                <div class="pd10">PEER: {{ user.device }}-{{ user.tempName }}-{{ user.id }}</div>
                <div id="test" style="height: 120px; overflow-y: scroll; border: 1px solid black">
                    <div v-for="msg in user.msgs">
                        <div v-if="msg.type==='send'" style="background-color: #0aa858">
                            <div style="white-space: pre-line">
                                <div></div>
                                {{ msg.msg }}
                            </div>
                        </div>
                        <div v-else style="background-color: gray">
                            <div style="white-space: pre-line">
                                <div></div>
                                {{ msg.msg }}
                            </div>
                        </div>
                    </div>
                </div>
                <div class="flexg1">
                    <el-input type="textarea" v-model="user.input"></el-input>
                </div>
                <div class="flexg1">
                    <el-button @click="send(user.id)">send</el-button>
                </div>
            </div>

        </template>
    </div>
</template>

<script lang="ts">
import {notify} from '@/ts/Notification';
import {Component, Vue} from 'vue-property-decorator';
import platform from 'platform';
import {tempIdentificationGenerator} from "@/ts/TempName";
import {PageLocation} from "@/ts/dynamicLocation";

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
            t.msgs.push({
                peerId: id,
                msg: i,
                type: "send"
            })
            this.$nextTick(() => {
                this.scrollToBottom(1)
            })
        }
    }

    scrollToBottom(userId: any) {
        // let o = getVueEl(this, `output-${userId}`)
        // let t = o.querySelector('textarea') as HTMLTextAreaElement
        // t.scrollTop = t.scrollHeight;
        let test = document.getElementById("test")!!
        test.scrollTop = test.scrollHeight;
    }

    created() {
        let a = new PageLocation()
        let w = a.hostname
        let p = Number(a.port) + 1
        this.webSocketClient = new WebSocket(`ws://${w}:${p}/chat`);
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
                    let l = b.user_list
                    l.forEach((user: any) => {
                        this.givePropertiesToNewUser(user)
                    })
                    this.users = l

                    this.me = b.me
                } else if (type === "ALIVE_PING") {
                    console.log("receive alive ping")
                    this.webSocketClient?.send(JSON.stringify({
                        messageType: "ALIVE_ACK"
                    }));
                } else if (type === "MESSAGE") {
                    let b = JSON.parse(a.message)
                    console.log("receive message from others", b)
                    let f = b.fromId
                    let t = b.toId
                    let m = b.msg
                    let e = this.users.find((user) => {
                        return user.id === f
                    })
                    if (e != null) {
                        e.msgs.push({
                            peerId: f,
                            msg: m,
                            type: "receive"
                        })

                        let n = m + "\n"
                        if (e.output == null) {
                            e.output = n
                        } else {
                            e.output += n
                        }
                        this.$nextTick(() => {
                            this.scrollToBottom(f)
                        })
                    }
                    // this.webSocketClient?.send(JSON.stringify({
                    //     messageType: "ALIVE_ACK"
                    // }));
                } else if (type === "HELLO") {

                    let b = JSON.parse(a.message)
                    console.log("new user comes into room: ", b)
                    let newFri = b
                    let e = this.users.find((user) => {
                        return user.id === newFri.id
                    })
                    if (e == null) {
                        this.givePropertiesToNewUser(newFri)
                        this.users.push(newFri)
                    }
                } else if (type === "LEAVE") {
                    let b = JSON.parse(a.message)
                    console.log("user leaves room: ", b)
                    this.users = this.users.filter((user) => {
                        return user.id !== b.id
                    })
                }
            }

        };
    }

    givePropertiesToNewUser(newUser: any) {
        newUser.input = ""
        newUser.output = ""
        newUser.msgs = []
    }

}
</script>
<style lang="scss" scoped>

@import "~@/style/common-style.scss";
</style>
