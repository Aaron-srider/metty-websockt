import cache from '@/ts/cache';

// export function get_all_users(): any[] {
//     let users = localStorage.getItem('users');
//     if (users === null) {
//         return [] as any[];
//     }
//     return JSON.parse(users);
// }

export function get_all_users_from_db(): Promise<any[]> {
    return new Promise((resolve, reject) => {
        cache
            .getAllKeys('user')
            .then((keys) => {
                return keys;
            })
            .then((keys) => {
                return cache.filterRecords('user', (cursor) => {
                    return keys.includes(cursor.key);
                });
            })
            .then((resp) => {
                resolve(resp);
            })
            .catch((err) => reject(err));
    });
}

export function delete_user_by_id_from_db(id: any) {
    cache.removeItem('user', id);
}

// export function delete_user_by_id(id: string) {
//     let users = get_all_users();
//     let new_users = users.filter((user) => user.id !== id);
//     localStorage.setItem('users', JSON.stringify(new_users));
// }

export function add_user_to_db(user: any) {
    cache.setItem('user', {
        ...user,
    });
}

/**
 * get me from config table, if me is not exist, null, otherwise returns me object described below:
 *
 * {
 *     id: string,
 *     tempName: string,
 *     device: string,
 * }
 */
export function get_me_from_db() {
    return new Promise((resolve, reject) => {
        cache
            .filterRecords('config', (cursor) => {
                return cursor.value.key === 'me';
            })
            .then((resp) => {
                if (resp.length === 0) {
                    resolve(null);
                } else {
                    resolve(resp[0].value);
                }
            })
            .catch((err) => {
                reject(err);
            });
    });
}

export function set_me_to_db(me: any) {
    cache
        .setItem('config', {
            key: 'me',
            value: me,
        })
        .then(() => {
            console.log('me set');
        })
        .catch((err) => {
            console.log(err);
        });
}
