package com.cxy.redisclient.domain;

import com.cxy.redisclient.integration.I18nFile;
import com.cxy.redisclient.presentation.RedisClient;

public enum NodeType {
    ROOT, SERVER, DATABASE, CONTAINER, STRING, HASH, LIST, SET, SORTEDSET;

    @Override
    public String toString() {
        if (this.name().equals(SERVER.name())) {
            return RedisClient.i18nFile.getText(I18nFile.SERVER);
        } else if (this.name().equals(DATABASE.name())) {
            return RedisClient.i18nFile.getText(I18nFile.DATABASE);
        } else if (this.name().equals(CONTAINER.name())) {
            return RedisClient.i18nFile.getText(I18nFile.CONTAINER);
        } else if (this.name().equals(STRING.name())) {
            return RedisClient.i18nFile.getText(I18nFile.STRING);
        } else if (this.name().equals(HASH.name())) {
            return RedisClient.i18nFile.getText(I18nFile.HASH);
        } else if (this.name().equals(LIST.name())) {
            return RedisClient.i18nFile.getText(I18nFile.LIST);
        } else if (this.name().equals(SET.name())) {
            return RedisClient.i18nFile.getText(I18nFile.SET);
        } else if (this.name().equals(SORTEDSET.name())) {
            return RedisClient.i18nFile.getText(I18nFile.ZSET);
        } else {
            return "";
        }
    }
}
