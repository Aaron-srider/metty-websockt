export function getRef(vue: any, refname: string) {
    let rs = vue.$refs[refname]
    if(isArray(rs)) {
        if(rs.length >0) {
            return rs[0]
        } else {
            throw new Error(`ref ${refname} not found`)
        }
    } else {
        return rs
    }
}

export function getVueEl(vue: any, refname: string): HTMLElement {
    let rs = vue.$refs[refname]
    if(isArray(rs)) {
        if(rs.length >0) {
            return rs[0].$el as HTMLElement
        } else {
            throw new Error(`ref ${refname} not found`)
        }
    } else {
        return rs.$el as HTMLElement
    }
}

export function getRefAsHtml(vue: any, refname: string): HTMLElement {
    return getRef(vue, refname) as HTMLElement
}


function isArray(o: any) {
    return Object.prototype.toString.call(o) === '[object Array]';
}
