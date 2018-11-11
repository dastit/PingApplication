package com.example.pingapplication;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {HostName.class}, version = 1)
public abstract class HostNameDatabase extends RoomDatabase {
    public abstract HostNameDao hostNameDao();
}
