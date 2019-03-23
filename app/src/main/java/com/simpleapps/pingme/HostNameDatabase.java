package com.simpleapps.pingme;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {HostName.class}, version = 1)
public abstract class HostNameDatabase extends RoomDatabase {
    private static HostNameDatabase INSTANCE;

    public abstract HostNameDao hostNameDao();

    public static HostNameDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = buildDatabase(context);
        }
        return INSTANCE;
    }

    public static HostNameDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(context,
                                    HostNameDatabase.class,
                                    "Host_name_database")
                   .allowMainThreadQueries().build();
    }
}
