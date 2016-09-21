package com.cxy.redisclient.integration.zset;

import com.cxy.redisclient.domain.RedisVersion;
import com.cxy.redisclient.integration.JedisCommand;

public class RemoveMembers extends JedisCommand {

    private final int db;
    private final String key;
    private final String[] members;

    public RemoveMembers(int id, int db, String key, String[] members) {
        super(id);
        this.db = db;
        this.key = key;
        this.members = members;
    }

    @Override
    protected void command() {
        jedis.select(db);
        jedis.zrem(key, members);

    }

    @Override
    public RedisVersion getSupportVersion() {
        return RedisVersion.REDIS_1_2;
    }

}
