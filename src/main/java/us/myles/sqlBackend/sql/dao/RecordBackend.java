package us.myles.sqlBackend.sql.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import us.myles.sqlBackend.sql.MapData;

import java.util.List;

@RegisterMapper(value = MapDataMapper.class)
public interface RecordBackend extends GetHandle {
    @SqlUpdate("insert into <table> DEFAULT VALUES")
    @GetGeneratedKeys
    int createRecord(@Bind("table") String table);

    @SqlQuery("select * from <table> where id = :id")
    MapData findRecord(@Bind("table") String table, @Bind("id") int id);

    @SqlQuery("select * from <table> where <key> = :value LIMIT 1")
    MapData findRecord(@Bind("table") String table, @Bind("key") String key, @Bind("value") Object value);

    @SqlQuery("select * from <table> where <key> = :value")
    List<MapData> findRecords(@Bind("table") String table, @Bind("key") String key, @Bind("value") Object value);

    @SqlUpdate("update <table> set <key> = :value where id = :id")
    int updateRecord(@Bind("table") String table, @Bind("id") int id, @Bind("key") String key, @Bind("value") Object value);

    @SqlUpdate("delete from <table> where id = :id")
    void deleteRecord(@Bind("table") String table, @Bind("id") int id);

    @SqlQuery("select * from <table> where id = :id")
    boolean isRecord(@Bind("table") String table, @Bind("id") int id);

    @SqlQuery("select * from <table>")
    List<MapData> findAll(@Bind("table") String table);

    void close();
}
