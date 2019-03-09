
package com.example.demo.service;

import com.example.demo.dto.MsgDto;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MsgServiceImpl implements MsgService {
    private static final int INITIAL_DELAY = 0;
    private static Logger logger = LoggerFactory.getLogger(MsgServiceImpl.class);

    @Value(value = "${node.queue.key}")
    private String nodeQueueKey;

    @Value(value = "${node.increment.key}")
    private String incrementKey;

    @Value(value = "${batch.period}")
    private Integer period;

    private RedisClient redisClient;
    private RedisAsyncCommands<String, String> asyncConnection;
    private RedisCommands<String, String> syncConnection;

    /**
     * Lazy change the status of the node in case of planned shutdown. Not implemented as it out of scope for the task
     */
    private AtomicBoolean nodeAlive = new AtomicBoolean(true);

    @Autowired
    public MsgServiceImpl(RedisClient redisClient) {
        this.redisClient = redisClient;

        this.asyncConnection = this.redisClient.connect().async();
        this.syncConnection = this.redisClient.connect().sync();
    }

    @Override
    public void addMsg(MsgDto msg) {
        Long key = toKeyRepresentation(msg.getTime());
        Long score = toMilliSecondRepresentation(msg.getTime());
        this.asyncConnection.zadd(key.toString(), score, msg.getMsg());
    }

    @Override
    public void pop() {
        Executors.newSingleThreadExecutor().execute(() -> {
            RedisCommands<String, String> sync = this.redisClient.connect().sync();

            while (this.nodeAlive.get()) {
                KeyValue<String, String> kv = sync.blpop(0, this.nodeQueueKey);
                String value = kv.getValue();
                logger.info(value);
            }
        });
    }

    @Override
    public void pull() {
        pullMissedMsgOnStart();
        pullMsgOnRegularWay();
    }

    private void pullMsgOnRegularWay() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            String key = toKeyRepresentation(LocalDateTime.now()).toString();
            pullBatchToQueue(key);
        }, INITIAL_DELAY, period, TimeUnit.MILLISECONDS);
    }

    private void pullMissedMsgOnStart() {
        String stringKey = this.syncConnection.get(this.incrementKey); // if key == null - means that it is new node.
        if (stringKey != null) {
            Long stoppedKey = Long.valueOf(stringKey);

            while (stoppedKey <= toKeyRepresentation(LocalDateTime.now())) {
                pullBatchToQueue(stoppedKey.toString());
                stoppedKey++;
            }
        }
    }

    private void pullBatchToQueue(String key) {
        List<String> messages = this.syncConnection.zrange(key, 0, -1);// full zSet
        if (this.nodeAlive.get() && !CollectionUtils.isEmpty(messages)) {
            this.syncConnection.multi();
            messages.forEach(line -> syncConnection.lpush(this.nodeQueueKey, line));
            this.syncConnection.set(this.incrementKey, key);
            this.syncConnection.exec();
        }
    }

    private Long toKeyRepresentation(LocalDateTime time) {
        long stamp = toMilliSecondRepresentation(time);
        return stamp / this.period;
    }

    private Long toMilliSecondRepresentation(LocalDateTime time) {
        return Timestamp.valueOf(time).getTime();
    }
}
