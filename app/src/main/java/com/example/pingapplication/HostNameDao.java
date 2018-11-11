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

    @Insert
    void insertAll(HostName... names);

    @Delete
    void delete(HostName name);
}
