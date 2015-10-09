package us.myles.sqlBackend.example;

import us.myles.sqlBackend.api.backend.RecordData;
import us.myles.sqlBackend.caching.Frontend;

public class User extends Frontend {
    public User(RecordData internalData) {
        super(internalData);
    }

    public String getUsername() {
        return internal().getAsString("username");
    }

    public void setUsername(String username){
        internal().put("username", username);
    }

    public int getAge(){
        return internal().getAsInt("age");
    }

    public void setAge(int age){
        internal().put("age", age);
    }
}
