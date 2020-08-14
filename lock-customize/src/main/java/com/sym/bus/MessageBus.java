package com.sym.bus;

import com.sym.enums.MessageTypeEnum;
import com.sym.holder.ThreadHolder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * 消息总线,
 *
 * @author shenyanming
 * Created on 2020/8/13 18:28
 */
public class MessageBus {

    private static BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private static Thread thread;

    private static volatile boolean isRunning = true;
    private static volatile boolean isStarted = false;

    private MessageBus() {

    }

    /**
     * 开启消息总线
     */
    public synchronized static void start() {
        if (isStarted) {
            return;
        }
        isStarted = true;
        thread = new Thread(new Worker(), "messageBus-work");
        thread.start();
    }

    /**
     * 关闭消息总线
     */
    public synchronized static void stop() {
        if (!isStarted) {
            return;
        }
        isRunning = false;
        thread.interrupt();
    }

    /**
     * 发布消息
     *
     * @param message 具体消息
     */
    public static void publish(Message message) {
        queue.offer(message);
    }

    /**
     * 消费消息, 会单独跑在一个线程里.这边逻辑很简单, 就是唤醒处于阻塞状态的线程
     */
    private static void consume(Message message) {
        if (message.getTypeEnum() != MessageTypeEnum.RELEASE_LOCK) {
            return;
        }
        String key = message.getDate().toString();
        List<Thread> threads = ThreadHolder.get(key);
        threads.forEach(LockSupport::unpark);
    }

    @Data
    public static class Message {
        private MessageTypeEnum typeEnum;
        private Object date;
    }

    /**
     * 实际工作线程
     */
    private static class Worker implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                // 获取队列中的消息
                try {
                    consume(queue.take());
                } catch (InterruptedException e) {
                    // 如果线程被中断, 判断当前消息总线是否被暂停了
                    if (!isRunning) {
                        break;
                    }
                    // 如果未停止, 中断状态会被清除, 继续运行
                }
            }
        }
    }
}
