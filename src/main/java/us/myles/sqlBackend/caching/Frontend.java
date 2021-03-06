package us.myles.sqlBackend.caching;

import us.myles.sqlBackend.api.backend.RecordData;

public class Frontend {
    private RecordData internalData;
    private Long lastCheck;

    public Frontend(RecordData internalData) {
        this.internalData = internalData;
    }

    public Frontend(){
        this.internalData = new DummyRecordData();
    }

    public void init() {

    }

    public void load() {

    }

    // if this is false, record is deleted >.>
    public boolean ensureLatest(){
        if(internal().verify(this, true)){
            internal().pushUpdates();
            return true;
        }else{
            return false;
        }
    }

    protected RecordData internal() {
        return this.internalData;
    }

    public boolean verify() {
        return internal().verify(this, false);
    }

    public Long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public void inject(RecordData recordData) {
        this.internalData = recordData;
    }
}
