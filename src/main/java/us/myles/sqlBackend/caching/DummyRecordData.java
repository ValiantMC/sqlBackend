package us.myles.sqlBackend.caching;

public class DummyRecordData extends us.myles.sqlBackend.api.backend.RecordData {
    @Override
    public void handlePut(String key, Object value) {

    }

    @Override
    public void pushUpdates() {

    }

    @Override
    public boolean verify(Frontend frontend, boolean force) {
        return true;
    }
}
