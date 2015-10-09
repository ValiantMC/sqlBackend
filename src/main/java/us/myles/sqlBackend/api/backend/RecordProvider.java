package us.myles.sqlBackend.api.backend;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;

import java.util.List;

public interface RecordProvider<T> {
    Optional<T> findRecord(int id);

    T createRecord();

    void deleteRecord(int id);

    void updateRecord(int id, String key, Object value);

    boolean isRecord(int id);

    List<T> findRecords(String key, Object value);

    Query createQuery(String initialQuery);

    Update createUpdate(String initialQuery);

    List<T> findRecords(Query query);

    Optional<T> findRecord(String key, Object value);

    List<T> findAll();

    void shutdown();

    void forceQueuedUpdates(T id);
}
