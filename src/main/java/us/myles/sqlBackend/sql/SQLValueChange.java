package us.myles.sqlBackend.sql;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLValueChange implements Runnable {
    private Long lastUpdated;
    private String table;
    private int record;
    private Map<String, Object> map = new HashMap<>();
    private SQLService service;

    public SQLValueChange(SQLService service, String table, int record, String key, Object value, Long lastUpdated) {
        this.table = table;
        this.record = record;
        map.put(key, value);
        this.service = service;
        this.lastUpdated = lastUpdated;
    }

    public String getTable() {
        return table;
    }

    public int getRecord() {
        return record;
    }

    public Map<String, Object> getMap(){
        return this.map;
    }
    @Override
    public void run() {
        // we're gonna manually change this
        map.put("lastUpdated", new Timestamp(lastUpdated));
        service.getTable(getTable()).bulkUpdate(getRecord(), map);
    }
}
