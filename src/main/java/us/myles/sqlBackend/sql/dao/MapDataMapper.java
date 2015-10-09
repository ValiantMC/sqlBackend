package us.myles.sqlBackend.sql.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import us.myles.sqlBackend.sql.MapData;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MapDataMapper implements ResultSetMapper<MapData> {
    public MapData map(int z, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        MapData mapData = new MapData();
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            if (resultSet.getObject(i) != null)
                mapData.directPut(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
        }
        return mapData;
    }
}
