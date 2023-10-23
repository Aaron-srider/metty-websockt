import {Notification} from "element-ui";

export function notify(type: string | null, title: string | null, message: string, fontSize: number | null, fontBold: boolean | null) {
    Notification({
        // @ts-ignore
        type: `${type == null ? 'info' : type}`,
        title: `${title == null ? 'Notification' : title}`,
        dangerouslyUseHTMLString: true,
        message: `<span style="font-size: ${fontSize == null ? '16' : fontSize}px; font-weight: ${fontBold ? 'bold' : 'normal'}">${message}</span>`
    })
}
