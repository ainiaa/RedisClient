package com.cxy.redisclient.integration.hash;

import com.cxy.redisclient.domain.RedisVersion;
import com.cxy.redisclient.integration.JedisCommand;

public class SetField extends JedisCommand {

    private final int db;
    private final String key;
    private final String field;
    private final String value;

    public SetField(int id, int db, String key, String field, String value) {
        super(id);
        this.db = db;
        this.key = key;
        this.field = field;
        this.value = value;
    }

    @Override
    protected void command() {
        jedis.select(db);
        jedis.hset(key, field, value);

    }

    @Override
    public RedisVersion getSupportVersion() {
        return RedisVersion.REDIS_2_0;
    }

}
