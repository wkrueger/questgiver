package com.krueg.questgiver.dataobject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;

import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.NotificationService;
import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.Singleton.DemSQL;
import com.krueg.questgiver.gamby.RGDate;

public class NotificationDO extends BaseDO {
    
    //if TESTMODE is true, all new notifications
    //are scheduled to run in 10 seconds
    public static boolean TESTMODE = false;
    
    public String title;
    public String desc;
    private boolean checked;
    public Long schedule;
    public int stepId;
    public int type;
    
    public static final int TYPE_SINGLE60PCT = 0;
    public static final int TYPE_DAILYWEEK = 1;
    public static final int TYPE_SINGLEEXPIRED = 2;
    public static final int TYPE_ABANDONED = 3;
    
    public NotificationDO() {
        checked = false;
        stepId = -1;
        type = -1;
    }
    
    public NotificationDO(Cursor c) {
        super(c);
    }
    
    public NotificationDO(int pk) {
        super(pk);
    }
    
    @Override
    public String getTable() {
        return "notification";
    }

    ContentValues preSaveHook() {
        ContentValues values = new ContentValues();
        values.put("title",title);
        values.put("desc",desc);
        values.put("checked",checked);
        if ( schedule < new RGDate().getTime() ) schedule = new RGDate().getTime();
        values.put("schedule", schedule);
        values.put("step",stepId);
        values.put("type", type);
        return values;
    }
    
    @Override
    void initialize(Cursor c) {
        title = c.getString(c.getColumnIndex("title"));
        desc = c.getString(c.getColumnIndex("desc"));
        checked = c.getInt(c.getColumnIndex("checked")) != 0;
        schedule = c.getLong(c.getColumnIndex("schedule"));
        stepId = c.getInt(c.getColumnIndex("step"));
        type = c.getInt(c.getColumnIndex("type"));
    }
    
    @Override
    void onSuccessfulInsert() {
        scheduleAlarm();
    }
    
    @Override
    void onSuccessfulUpdate() {
        scheduleAlarm();
    }
    
    /**
     * Checked notifications will not be called again
     */
    public void setChecked(boolean val) {
        checked = val;
        update();
    }
    
    /**
     * Schedules a call to the service at the desired
     * time. The call is sent with an "id" unique to this
     * dataobject and may be further cancelled.
     * 
     * Upon running, the notification service builds a new
     * notification based on the current notification table
     */
    private void scheduleAlarm() {
        scheduleAlarm(schedule);
    }

    public void scheduleAlarm(long time) {
        Intent intent = new Intent(Singleton.context,NotificationService.class);
        
        Uri.Builder uBuilder = new Uri.Builder();
        uBuilder.scheme(Singleton.SCHEME);
        uBuilder.authority("prg.me");
        uBuilder.appendQueryParameter("nid",Integer.toString(getId()));
        intent.setData(uBuilder.build());

        // ^ even if this uri is invalid, as per the docs it makes the intent
        //"unique". When posting to the alarm, it will replace another schedule
        //with the same notification id. The same id can also be used to cancel
        //the intent
        
        PendingIntent alarmIntent = PendingIntent.getService(Singleton.context,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) Singleton.context
                .getSystemService(Context.ALARM_SERVICE);
        long timefromnow;
        if ( !TESTMODE )
            timefromnow = (long) (time - new RGDate().getTime());   //time from now to then
        else 
            timefromnow = 1000 * 3;
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timefromnow , alarmIntent);
    }
    
    public static Cursor queryMe(int step,int type) {
        String query = "SELECT * FROM " + DemSQL.TABLE_NOTIFICATION +
                " WHERE step = ? AND type = ?";
        return Singleton.db_r.rawQuery(query,
                new String[] {Integer.toString(step),Integer.toString(type)});
    }

    public static Cursor queryMeType(int type) {
        String query = "SELECT * FROM " + DemSQL.TABLE_NOTIFICATION +
                " WHERE type = ?";
        return Singleton.db_r.rawQuery(query,new String[]{Integer.toString(type)});
    }
    
    public static Cursor queryMe(int step) {
        String query = "SELECT * FROM " + DemSQL.TABLE_NOTIFICATION +
                " WHERE step = ?";
        Cursor c = Singleton.db_r.rawQuery(query,
                new String[] {Integer.toString(step)});
        if ( c.getCount() == 0 ) throw new PkNotFoundException();
        return c;
    }
    
    public static Cursor queryUnchecked() {
        long now = new RGDate().getTime();
        String query = "SELECT * FROM " + DemSQL.TABLE_NOTIFICATION +
                " WHERE checked = ? AND schedule < ?";
        Cursor c = Singleton.db_r.rawQuery(query, new String[]{"0",""+now});
        return c;
    }
    
    @Override
    public int compareTo(BaseDO another) {
        NotificationDO not = (NotificationDO) another;
        return -schedule.compareTo(not.schedule);
    }

    /**
     * Remove pending notifications on DO remove
     */
    @Override
    public void delete() {
        Intent intent = new Intent(Singleton.context,NotificationService.class);
        
        Uri.Builder uBuilder = new Uri.Builder();
        uBuilder.scheme(Singleton.SCHEME);
        uBuilder.authority("prg.me");
        uBuilder.appendQueryParameter("nid",Integer.toString(getId()));
        intent.setData(uBuilder.build());
        PendingIntent alarmIntent = PendingIntent.getService(Singleton.context,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) Singleton.context
                .getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(alarmIntent);
        
        super.delete();
    }

    public Integer getShortTypeString() {
        switch (type) {
            case 0:return R.string.notif_short_type_0;
            case 1:return R.string.notif_short_type_1;
            case 2:return R.string.notif_short_type_2;
            case 3:return R.string.notif_short_type_3;
        }
        
        return R.string.notif_short_type_0;
    }
    
}
