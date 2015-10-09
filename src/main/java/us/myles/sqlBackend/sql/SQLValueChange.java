package us.myles.sqlBackend.sql;

public class SQLValueChange implements Runnable {
    private Long lastUpdated;
    private String table;
    private int record;
    private String key;
    private Object value;
    private SQLService service;

    public SQLValueChange(SQLService service, String table, int record, String key, Object value, Long lastUpdated) {
        this.table = table;
        this.record = record;
        this.key = key;
        this.value = value;
        this.service = service;
        this.lastUpdated = lastUpdated;
    }

    public String getTable() {
        return table;
    }

    public int getRecord() {
        return record;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void run() {
        service.getTable(getTable()).updateRecord(getRecord(), getKey(), getValue());
        service.getTable(getTable()).updateRecord(getRecord(), "lastUpdated", lastUpdated);
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
