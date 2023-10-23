package fit.wenchao.simple_chat.server;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger nextThreadNumber = new AtomicInteger(1);

    public CustomThreadFactory(String threadNamePrefix) {
        this.namePrefix = threadNamePrefix;
    }

    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + nextThreadNumber.getAndIncrement());
        return thread;
    }
}
