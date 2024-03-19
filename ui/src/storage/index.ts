export function get_all_users(): any[] {
    let users = localStorage.getItem("users")
    if (users === null) {
        return [] as any[];
    }
    return JSON.parse(users)
}

export function get_user_by_id(id: string) {
    let users = get_all_users()
    return users.find(user => user.id === id)
}

export function delete_user_by_id(id: string) {
    let users = get_all_users()
    let new_users = users.filter(user => user.id !== id)
    localStorage.setItem("users", JSON.stringify(new_users))
}

export function add_user(user: any) {
    let users = get_all_users()
    users.push(user)
    localStorage.setItem("users", JSON.stringify(users))
}

export function update_user(user: any) {
    let users = get_all_users()
    let new_users = users.map(u => {
        if (u.id === user.id) {
            return user
        }
        return u
    })
    localStorage.setItem("users", JSON.stringify(new_users))
}

export function get_me() {
    let local_id = localStorage.getItem("me")
    if (local_id == null) {
        return null
    }
    return JSON.parse(local_id)
}

export function set_me(me: any) {
    localStorage.setItem("me", JSON.stringify(me))
}

export function add_msg_to_user_by_id(id: string, msg: any) {
    let users = get_all_users()
    let new_users = users.map(u => {
        if (u.id === id) {
            if(u.msgs === undefined || u.msgs === null) {
                u.msgs = []
            }
            u.msgs.push(msg)
        }
        return u
    })
    localStorage.setItem("users", JSON.stringify(new_users))
}
