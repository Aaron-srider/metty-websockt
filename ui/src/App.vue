<template>
    <div id="app">
        <router-view />
    </div>
</template>

<style lang="scss">
#app {
    font-family: Avenir, Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    color: #2c3e50;
}

nav {
    padding: 30px;

    a {
        font-weight: bold;
        color: #2c3e50;

        &.router-link-exact-active {
            color: #42b983;
        }
    }
}
</style>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import cache from '@/ts/cache';

@Component({})
export default class App extends Vue {
    created() {
        //  //  id, time, peer_id, msg, type(receive, send)
        // cache
        //     .setItem({
        //         time: new Date().getTime(),
        //         peer_id: 'jkafjklasf',
        //         msg: 'jlsdjfasjdlkfasdf',
        //         type: 'send',
        //     })
        //     .then((resp) => {
        //         console.log('insert success');
        //     })
        //     .catch((err) => {
        //         console.log(err);
        //     });

        cache.getAllKeys('message').then((resp) => {
            console.log(resp);
        });

        cache
            .filterRecords('message', (cursor) => {
                return true;
            })
            .then((resp) => {
                console.log(resp);
            });

        cache
            .getItem('message', 2)
            .then((resp) => {
                console.log(resp);
            })
            .catch((err) => {
                console.log(err);
            });

        cache.setItem('message', {
            time: new Date().getTime(),
            peer_id: 'jkafjklasf',
            msg: 'jlsdjfasjdlkfasdf',
            type: 'send',
            id: 2,
        });

        cache
            .getItem('message', 2)
            .then((resp) => {
                console.log(resp);
            })
            .catch((err) => {
                console.log(err);
            });

        cache.getAllKeys('message').then((resp) => {
            console.log(resp);
        });
    }
}
</script>
