<template>
    <div class="">
        <div class="mgb20">
            <div>ME: {{ me.device }}-{{ me.tempName }}-{{ me.id }}</div>
        </div>
        <template v-for="user in users">
            <div class="border" style="margin-bottom: 20px">
                <div class="pd10">
                    PEER: {{ user.device }}-{{ user.tempName }}-{{ user.id }}-{{
                        user.online
                    }}
                    <div>
                        <el-button
                            type="danger"
                            size="mini"
                            @click="remove_user(user)"
                        >
                            <i class="el-icon-delete"></i>
                        </el-button>
                    </div>
                </div>
                <div
                    id="test"
                    style="
                        height: 120px;
                        overflow-y: scroll;
                        border: 1px solid black;
                    "
                >
                    <div v-for="msg in user.msgs">
                        <div
                            v-if="msg.type === 'send'"
                            style="background-color: #0aa858"
                        >
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
                    <el-button @click="onclick_send_msg(user.id)">
                        send
                    </el-button>
                </div>
            </div>
        </template>
    </div>
</template>

<script lang="ts">
import { notify } from '@/ts/Notification';
import { Component, Vue } from 'vue-property-decorator';
import platform from 'platform';
import { tempIdentificationGenerator } from '@/ts/TempName';
import { PageLocation } from '@/ts/dynamicLocation';
import * as storage from '@/storage';

@Component({})
export default class WebSocketView extends Vue {
    webSocketClient: WebSocket | null = null;
    users: any[] = [];
    me: any = {};

    onclick_send_msg(user_id: string) {
        let user = this.users.find((user) => user.id === user_id);
        if (user != null) {
            let user_input = user.input;
            this.webSocketClient?.send(
                JSON.stringify({
                    messageType: 'MESSAGE',
                    message: JSON.stringify({
                        fromId: this.me.id,
                        toId: user_id,
                        msg: user_input,
                    }),
                }),
            );
            let msg = {
                time: new Date().getTime(),
                peerId: user_id,
                msg: user_input,
                type: 'send',
            };
            user.msgs.push(msg);
            storage.add_msg_to_user_by_id(user_id, msg);
            this.$nextTick(() => {
                this.scrollToBottom(1);
            });
        }
    }

    scrollToBottom(userId: any) {
        // let o = getVueEl(this, `output-${userId}`)
        // let t = o.querySelector('textarea') as HTMLTextAreaElement
        // t.scrollTop = t.scrollHeight;
        let test = document.getElementById('test')!;
        test.scrollTop = test.scrollHeight;
    }

    remove_user(user: any) {
        if (user.online === false) {
            this.users = this.users.filter((u) => {
                return u.id !== user.id;
            });
            storage.delete_user_by_id(user.id);
        } else {
            notify(null, null, 'user is online, cannot remove', null, true);
        }
    }

    created() {
        let a = new PageLocation();
        let w = a.hostname;
        let p = Number(a.port) + 1;
        this.webSocketClient = new WebSocket(`ws://${w}:${p}/chat`);
        // this.webSocketClient = new WebSocket('ws://localhost:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost1:8080/chat');
        this.webSocketClient = new WebSocket('ws://172.27.128.180:8081/chat');
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

            let me = storage.get_me();

            if (me === null) {
                this.webSocketClient?.send(
                    JSON.stringify({
                        messageType: 'HELLO',
                        message: JSON.stringify({
                            ua: platform.ua,
                            osFamily: platform.os?.family,
                            osArch: platform.os?.architecture,
                            tempName: tempIdentificationGenerator.generate(),
                        }),
                    }),
                );
            } else {
                this.webSocketClient?.send(
                    JSON.stringify({
                        messageType: 'HELLO',
                        message: JSON.stringify({
                            ua: platform.ua,
                            osFamily: platform.os?.family,
                            osArch: platform.os?.architecture,
                            tempName: me.tempName,
                            id: me.id,
                        }),
                    }),
                );
            }
        };

        this.webSocketClient.onerror = (error) => {
            notify(null, 'Error', 'WebSocketClient error', null, true);
            console.log('WebSocketClient error: ', error);
        };

        this.webSocketClient.onclose = () => {
            notify(null, null, 'WebSocketClient closed', null, true);
        };

        this.webSocketClient.onmessage = (message) => {
            let isjson = false;
            let a;
            try {
                a = JSON.parse(message.data);
                isjson = true;
            } catch (e) {}

            if (isjson) {
                let type = a.messageType;
                if (type === 'HELLO_BACK') {
                    let b = JSON.parse(a.message);
                    console.log('come into room, roommates: ');

                    let users_from_storage = storage.get_all_users();
                    users_from_storage.forEach((it) => (it.online = false));
                    b.user_list.forEach((it: any) => (it.online = false));

                    // add new users to store if not exist
                    b.user_list.forEach((user: any) => {
                        let u = users_from_storage.find((u: any) => {
                            return u.id === user.id;
                        });
                        if (u == null) {
                            users_from_storage.push(user);
                            user.online = true;
                        } else {
                            u.online = true;
                        }
                    });

                    let me = storage.get_me();
                    let remote_me = b.me;
                    if (me === null) {
                        storage.set_me(remote_me);
                        this.me = remote_me;
                    } else {
                        this.me = {
                            ...remote_me,
                            id: me.id,
                            tempName: me.tempName,
                        };
                    }

                    // filter me
                    b.user_list = b.user_list.filter(
                        (it: any) => it.id !== this.me.id,
                    );

                    localStorage.setItem(
                        'users',
                        JSON.stringify(users_from_storage),
                    );

                    console.log('your identity: ', b.me);
                    let l = users_from_storage;
                    l.forEach((user: any) => {
                        this.givePropertiesToNewUser(user);
                    });
                    l = l.filter((it: any) => it.id !== me.id);
                    this.users = l;
                } else if (type === 'ALIVE_PING') {
                    console.log('receive alive ping');
                    this.webSocketClient?.send(
                        JSON.stringify({
                            messageType: 'ALIVE_ACK',
                        }),
                    );
                } else if (type === 'MESSAGE') {
                    let b = JSON.parse(a.message);
                    console.log('receive message from others', b);
                    let f = b.fromId;
                    let t = b.toId;
                    let m = b.msg;
                    let e = this.users.find((user) => {
                        return user.id === f;
                    });
                    if (e != null) {
                        e.msgs.push({
                            time: new Date().getTime(),
                            peerId: f,
                            msg: m,
                            type: 'receive',
                        });

                        storage.add_msg_to_user_by_id(f, {
                            time: new Date().getTime(),
                            peerId: f,
                            msg: m,
                            type: 'receive',
                        });

                        let n = m + '\n';
                        if (e.output == null) {
                            e.output = n;
                        } else {
                            e.output += n;
                        }
                        this.$nextTick(() => {
                            this.scrollToBottom(f);
                        });
                    }
                    // this.webSocketClient?.send(JSON.stringify({
                    //     messageType: "ALIVE_ACK"
                    // }));
                } else if (type === 'HELLO') {
                    let b = JSON.parse(a.message);
                    console.log('new user comes into room: ', b);
                    let newFri = b;
                    newFri.online = true;
                    let e = this.users.find((user) => {
                        return user.id === newFri.id;
                    });
                    if (e == null) {
                        this.givePropertiesToNewUser(newFri);
                        this.users.push(newFri);
                        storage.add_user(newFri);
                    } else {
                        e.online = true;
                    }
                } else if (type === 'LEAVE') {
                    let b = JSON.parse(a.message);
                    console.log('user leaves room: ', b);

                    this.users.forEach((user: any) => {
                        if (user.id === b.id) {
                            user.online = false;
                        }
                    });
                }
            }
        };
    }

    givePropertiesToNewUser(newUser: any) {
        newUser.input = '';
        newUser.output = '';
        newUser.msgs = [];
    }
}
</script>
<style lang="scss" scoped>
@import '~@/style/common-style.scss';
</style>
