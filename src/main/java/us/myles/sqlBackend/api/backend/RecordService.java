package us.myles.sqlBackend.api.backend;

public interface RecordService {
    public RecordProvider getTable(String name);

    void forceQueuedUpdates(String table, Integer id);
}
