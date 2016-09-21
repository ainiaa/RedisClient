package com.cxy.redisclient.integration.zset;

import java.util.Map;

import com.cxy.redisclient.domain.RedisVersion;

public class AddZSet12 extends AddZSet {

    public AddZSet12(int id, int db, String key, Map<String, Double> values) {
        super(id, db, key, values);
    }

    @Override
    public RedisVersion getSupportVersion() {
        return RedisVersion.REDIS_1_2;
    }

    @Override
    protected void addZSet() {
        values.entrySet().stream().forEach((value) -> {
            jedis.zadd(key, value.getValue(), value.getKey());
        });
    }

}
