package com.krueg.questgiver.dataobject;

import android.content.ContentValues;
import android.database.Cursor;

import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.Singleton.DemSQL;
import com.krueg.questgiver.gamby.RGDate;

import java.util.Date;

public class ActivityDO extends BaseDO {
    
    public String text;
    private Date date;

    public ActivityDO() {
        date = new RGDate();
    }
    
    public ActivityDO(Cursor c) {
        super(c);
    }
    
    public ActivityDO (int pk) {
        super(pk);
    }
    
    @Override
    public String getSQLOrderClause() {
        return "creation DESC";
    }
    
    protected void initialize(Cursor c) {
        if ( c.getPosition() == -1 ) c.moveToNext();
        text = c.getString(c.getColumnIndex("descr"));
        date = new Date(c.getLong(c.getColumnIndex("creation")));
    }
    
    public String getTable() {
        return DemSQL.TABLE_ACT;
    }
    
    public Date getDate() {
        return date;
    }

    public static void logAct(String text) {
        ActivityDO act = new ActivityDO();
        act.text = text;
        act.create();
    }
    
    @Override
    ContentValues preSaveHook() {
        ContentValues values = new ContentValues();
        values.put("creation", date.getTime());
        values.put("descr", text);
        return values;
    }
    
    @Override
    public Integer create() {

        ContentValues values = preSaveHook();

        try {
            Singleton.db_w.beginTransaction();
            id = (int)Singleton.db_w.insertOrThrow(getTable(), null, values);
            String[] args = { Long.toString(id - 40) };
            Singleton.db_w.delete(getTable(), "_id < ?", args);
            Singleton.db_w.setTransactionSuccessful();
        } finally {
            Singleton.db_w.endTransaction();
        }
        return id;
    }
}
