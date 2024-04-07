<template>
    <div class="">
        <div class="mgb20">
            <div>ME: {{ me.device }}-{{ me.tempName }}-{{ me.uid }}</div>
        </div>
        <template v-for="user in users">
            <div class="border" style="margin-bottom: 20px">
                <div class="pd10">
                    PEER: {{ user.device }}-{{ user.tempName }}-{{
                        user.uid
                    }}-{{ user.online }}
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
                    <el-button @click="onclick_send_msg(user.uid)">
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
import cache from '@/ts/cache';

export class MsgModel {
    local_uid = '';
    peer_uid = '';
    time = 0;
    msg = '';
    // send or receive
    type = '';
}

const { v4: uuidv4 } = require('uuid');

@Component({})
export default class WebSocketView extends Vue {
    webSocketClient: WebSocket | null = null;
    users: any[] = [];
    me: any = {};

    onclick_send_msg(user_id: string) {
        let user = this.users.find((user) => user.uid === user_id);
        if (user != null) {
            let user_input = user.input;
            this.webSocketClient?.send(
                JSON.stringify({
                    messageType: 'MESSAGE',
                    message: JSON.stringify({
                        fromUid: this.me.uid,
                        toUid: user_id,
                        msg: user_input,
                    }),
                }),
            );
            let msg = {
                local_uid: this.me.uid,
                time: new Date().getTime(),
                peer_uid: user_id,
                msg: user_input,
                type: 'send',
            };
            user.msgs.push(msg);
            cache.setItem('message', {
                local_uid: this.me.uid,
                time: new Date().getTime(),
                peer_uid: user_id,
                msg: user_input,
                type: 'send',
            });
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
                return u.uid !== user.uid;
            });

            storage.delete_user_by_id(user.uid);
        } else {
            notify(null, null, 'user is online, cannot remove', null, true);
        }
    }

    created() {
        let a = new PageLocation();
        let w = a.hostname;
        let p = Number(a.port) + 1;
        // this.webSocketClient = new WebSocket(`ws://${w}:${p}/chat`);
        // this.webSocketClient = new WebSocket('ws://localhost1:8080/chat');
        // this.webSocketClient = new WebSocket('ws://localhost:8080/chat');
        this.webSocketClient = new WebSocket('ws://172.27.128.180:8081/chat');
        // this.webSocketClient = new WebSocket('ws://192.168.31.36:8081/chat');
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

            storage.get_me_from_db().then((resp: any) => {
                let me = resp;
                if (me === null) {
                    this.me = null;
                    let uid = uuidv4();
                    let new_me = {
                        uid: uid,
                        tempName: tempIdentificationGenerator.generate(),
                        device: platform.os?.family,
                    };
                    storage.set_me_to_db(new_me);
                    storage.get_me_from_db().then((resp: any) => {
                        let id = resp.uid;
                        this.me = {
                            ...new_me,
                            id: id,
                        };

                        this.webSocketClient?.send(
                            JSON.stringify({
                                messageType: 'HELLO',
                                message: JSON.stringify({
                                    ua: platform.ua,
                                    osFamily: platform.os?.family,
                                    osArch: platform.os?.architecture,
                                    tempName: this.me.tempName,
                                    uid: this.me.uid,
                                }),
                            }),
                        );
                    });
                } else {
                    this.me = me;
                    this.webSocketClient?.send(
                        JSON.stringify({
                            messageType: 'HELLO',
                            message: JSON.stringify({
                                ua: platform.ua,
                                osFamily: platform.os?.family,
                                osArch: platform.os?.architecture,
                                tempName: me.tempName,
                                uid: me.uid,
                            }),
                        }),
                    );
                }
            });
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
                    let response_entity = JSON.parse(a.message);
                    let room_users_from_server = response_entity.user_list;
                    let online_users = [] as any;

                    let online_users_sort_over = () => {
                        online_users.forEach((user) => {
                            this.givePropertiesToNewUser(user);
                        });

                        online_users = online_users.filter((user) => {
                            return user.uid !== this.me.uid;
                        });

                        online_users.forEach((user) => {
                            cache
                                .filterRecords('message', (cursor) => {
                                    return cursor.value.peer_uid === user.uid;
                                })
                                .then((resp) => {
                                    resp.forEach((record) => {
                                        user.msgs.push(record);
                                    });
                                });
                        });
                    };

                    let futures = room_users_from_server.map((user: any) => {
                        // have recorded?
                        return cache
                            .filterRecords('user', (cursor) => {
                                return cursor.value.uid === user.uid;
                            })
                            .then((resp) => {
                                if (resp.length !== 0) {
                                    // if so, fetch out and set online
                                    online_users.push({
                                        ...resp[0],
                                        online: true,
                                    });
                                    return new Promise<any>((resolve) => {
                                        resolve({});
                                    });
                                } else {
                                    // else, record it, fetch out, and set online
                                    cache.setItem('user', user);
                                    return cache
                                        .filterRecords('user', (cursor) => {
                                            return (
                                                cursor.value.uid === user.uid
                                            );
                                        })
                                        .then((resp) => {
                                            online_users.push({
                                                ...resp[0],
                                                online: true,
                                            });
                                            return new Promise<any>(
                                                (resolve) => {
                                                    resolve({});
                                                },
                                            );
                                        });
                                }
                            });
                    });

                    Promise.all(futures).then(() => {
                        online_users_sort_over();
                        this.users = online_users;
                    });
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
                    let f = b.fromUid;
                    let m = b.msg;
                    let e = this.users.find((user) => {
                        return user.uid === f;
                    });
                    if (e != null) {
                        e.msgs.push({
                            time: new Date().getTime(),
                            peer_uid: f,
                            msg: m,
                            type: 'receive',
                        });

                        cache.setItem('message', {
                            local_uid: this.me.uid,
                            time: new Date().getTime(),
                            peer_uid: f,
                            msg: m,
                            type: 'receive',
                        });

                        // storage.add_msg_to_user_by_id(f, {
                        //     time: new Date().getTime(),
                        //     peer_uid: f,
                        //     msg: m,
                        //     type: 'receive',
                        // });

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
                        return user.uid === newFri.uid;
                    });
                    if (e == null) {
                        this.givePropertiesToNewUser(newFri);
                        this.users.push(newFri);
                        storage.add_user_to_db(newFri);
                    } else {
                        e.online = true;
                    }
                } else if (type === 'LEAVE') {
                    let b = JSON.parse(a.message);
                    console.log('user leaves room: ', b);

                    this.users.forEach((user: any) => {
                        if (user.uid === b.uid) {
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
