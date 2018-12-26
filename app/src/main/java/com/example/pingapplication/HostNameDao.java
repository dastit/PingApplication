package com.example.pingapplication;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface HostNameDao {
    @Query("SELECT * FROM HostName")
    List<HostName> getAll();

    @Query("SELECT * FROM HostName where name = :name")
    HostName get(String name);

    @Insert
    void insertAll(HostName... names);

    @Query ("DELETE from HostName where name= :name")
    void delete(String name);
}
