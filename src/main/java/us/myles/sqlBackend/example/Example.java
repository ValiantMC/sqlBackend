package us.myles.sqlBackend.example;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;
import us.myles.sqlBackend.api.backend.RecordProvider;
import us.myles.sqlBackend.api.backend.RecordService;
import us.myles.sqlBackend.caching.CacheService;
import us.myles.sqlBackend.sql.SQLService;

public class Example {
    public static void main(String[] args) {
        // Example of using the service for a users table
        RecordService rs = new SQLService("jdbc:sqlite:data.db");
        RecordProvider rp = rs.getTable("users");
        CacheService<User> cacheTest = new CacheService<>(User.class, rp);

        System.out.println("Age 19: " + cacheTest.findRecord("age", 18).get().getUsername());

        System.out.println("Testing custom queries");
        Query q = cacheTest.createQuery("SELECT * FROM <table> ORDER BY age LIMIT 2");
        for(User x:cacheTest.findRecords(q)){
            System.out.println("Found record: " + x.getUsername());
        }
        Update q2 = cacheTest.createUpdate("UPDATE <table> SET age = '10' WHERE age = '6999'");
        System.out.println(q2.execute());
        System.out.println("Adding a new user");
        User u = cacheTest.createRecord();
        u.setUsername("Jeff");
        //u.setAge(69);
        System.out.println("Done making user");
        // finally
        cacheTest.shutdown();
    }
}
