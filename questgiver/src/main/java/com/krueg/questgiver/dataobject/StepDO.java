package com.krueg.questgiver.dataobject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.Exceptions.RGTopLevelException;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.Singleton.DemSQL;
import com.krueg.questgiver.gamby.RGDate;
import com.krueg.questgiver.gamby.RGMutableDateTime;

import org.joda.time.MutableDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//all the extensions of this class share the same database table
//some fields are reused for different purposes
public class StepDO extends BaseDO {
    
    public MutableDateTime caltoday;
    public String title;
    private int type;
    Date lastIgnore;

    public static final int EVENT_COMPLETE = 1;
    public static final int EVENT_ALTER_RELEVANCE = 2;
    public static final int EVENT_ALTER_HOURCOST = 3;
    public static final int EVENT_ALTER_DEADLINE = 4;
    public static final int EVENT_REMOVE = 5;
    public static final int EVENT_WEEKLY_RESET = 6;
    public static final int EVENT_NEW = 7;
    public static final int EVENT_POSTPONE = 8;
    public static final int EVENT_AFTER_UPDATE = 9;
    public static final int EVENT_CHANGE_PARENT = 10;
    public static final int EVENT_ACTIVATE = 11;
    
    
    public interface OnEventListener {
        /**
         * Fired before something happens to a step. Except for
         * AFTER_UPDATE and NEW, which are fired after the changes are made.
         * @param step The dataz
         * @param what What happened (event id)
         * @return If the event is instance-wise (set by setLocalEv...), returning
         * TRUE will consume the event (delete it from the queue after the
         * execution). If it is class-wise (static), the return value is irrelevant.
         */
        public boolean onEvent(StepDO step,int what);
    }
    
    private static Map<String,OnEventListener> onEventListener =
            new HashMap<String,OnEventListener>();
    private Map<String,OnEventListener> localOnEventListener;

    private Object mTag;
    
    /**
     * Stores weekmask OR activation date
     */
    private long weekMask;
    
    boolean completion;
    Date created;
    
    public Float relevance;
    protected int parent;
    
    protected static final int UNDEFINED_PARENT = -1;
    protected static final int PARENT_SET_ME_ROOT = -2;
    
    //TODO someone said I also need to implement hashcode?
    public int compareTo(BaseDO another) {
        int out = 0;
        boolean isThisComplete = isComplete();
        boolean isOtherComplete = ((StepDO)another).isComplete();
        boolean isThisParent = this instanceof StepParentDO;
        boolean isOtherParent = another instanceof StepParentDO;
        
        if ( another.id == id ) out = 0;
        else if ( isThisComplete != isOtherComplete )
                out = (isThisComplete) ? -1 : 1;
        else if ( isThisParent != isOtherParent )
                out = (isThisParent) ? 1 : -1;
        else if ( isThisParent ) {
                Float a = (float)this.relevance;
                Float b = (float)((StepDO)another).relevance;
                out = a.compareTo(b);                  
        }
        else {
                Float a = this.getRelevanceTimesCompletion();
                Float b = ((StepDO)another).getRelevanceTimesCompletion();
                out = a.compareTo(b);
        }
       
        return out;
    }
    
    @SuppressWarnings("static-method")
    protected int getTypeId() {
        throw new Error("Trying to get type id from base step class. " +
            "This function should only be called from a subclass.");
    }
    
    /**
     * Returns one of the implementations based on cursor type
     */
    public static final StepDO newInstance(Cursor c) {
        if ( c.getPosition() == -1 ) c.moveToNext();    //throws cursorOutOfBounds if empty/ended
        int type = c.getInt(c.getColumnIndex("type"));
        try {
            switch (type) {
            case 0:
                return new StepSingleDO(c);
            case 1:
                return new StepWeekDO(c);
            case 2:
                return new StepSubsequentDO(c);
            case 3:
                return new StepParentDO(c);
            default:
                throw new Error("unexpected step type from cursor");    
            }
        } catch ( RGException e ) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns one of the implementations based on cursor type
     * This is the only way to construct a Step from an existing 
     * database record. For subclasses, only the void constructor is
     * public.
     */
    public static final StepDO newInstance(int pk) {
        String[] args = { Integer.toString(pk) };
        Cursor c = Singleton.db_r.query(DemSQL.TABLE_PERK_STEP, null, "_id = ?", args, null, null, null);
        if ( c.getCount() == 0 ) throw new PkNotFoundException();
        return newInstance(c);
    }
    
    public StepDO() {
        localOnEventListener = new HashMap<String,OnEventListener>();
        completion = false;
        created = new RGDate();
        relevance = 5F;
        setWeekMask(0);
        caltoday = new RGMutableDateTime();
        parent = UNDEFINED_PARENT;
        lastIgnore = new Date(0);
    }
    
    /**
     * @throws Exception if db type and object type differ
     */
    protected StepDO ( Cursor c ) throws RGException {
        super(c);
        if ( getTypeId() != type ) throw new RGException("wrong ctor usage");
    }
    
    /**
     * @throws Exception if db type and object type differ
     */
    protected StepDO ( int pk ) throws RGException {
        super(pk);
        if ( getTypeId() != type ) throw new RGException("wrong ctor usage");
    }
    
    public String getTable() {
        return DemSQL.TABLE_PERK_STEP;
    }
    
    //gamby-tm
    public Object getTag() {
        return mTag;
    }
    
    public Date getCreated() {
        return created;
    }
    
    @SuppressWarnings("static-method")
    float getCompletionScore() {
        throw new Error();
    }
    
    private float mLazyCompositeRelevance = -1;
    
    /**
     * Score used to weigh task priority
     * Takes into account the step relevance, the project relevance
     * and the time to the step deadline. Returns 0 for steps
     * not to be shown in act list.
     * @return relevance score
     */
    public float getRelevanceTimesCompletion() {
        return getCompletionScore() * getCompositeRelevance();
    }
    
    static int recursionCount = 0;
    
    /**
     * Step relevance, including parents
     * @return step relevance
     */
    public float getCompositeRelevance() {
        if ( mLazyCompositeRelevance >= 0 ) return mLazyCompositeRelevance;
        float out = relevance;
        
        //hack to avoid lockout in case of any error that causes infinite recursion
        if ( recursionCount > 7 ) {
            parent = id;
            update();
            Log.e("recursion", "Recursion Error on getCompositeRelevance(). Setting step to root");
        }
        try {
            recursionCount++;
            out *= getParent().getCompositeRelevance();
        } catch ( RGTopLevelException e ) {
            recursionCount = 0;
        }   //when we are at the top node, return just itself
        mLazyCompositeRelevance = out;
        return out;
    }
    
    /**
     * @throws RGTopLevelException 
     */
    public StepParentDO getParent() throws RGTopLevelException {
        if ( parent < 0 || parent == id ) throw new RGTopLevelException();
        StepDO ret;
        try { ret = StepDO.newInstance(parent); } catch ( PkNotFoundException e ) {
            delete();
            throw e;
        }
        return (StepParentDO) ret;
    }
    
    /**
     * @return True if the step shall be shown on act list
     * Default: removes complete steps
     */
    public boolean displayFilter() {
        if ( isComplete() || isIgnored() ) return false;
        return true;
    }
    
    @Override
    void initialize(Cursor c) {
        localOnEventListener = new HashMap<String,OnEventListener>();
        if ( c.getPosition() == -1 ) c.moveToNext();
        title = c.getString(c.getColumnIndex("name"));
        completion = c.getInt(c.getColumnIndex("complet")) != 0;
        type = c.getInt(c.getColumnIndex("type"));
        created = new Date(c.getLong(c.getColumnIndex("created")));
        relevance = c.getFloat(c.getColumnIndex("relevance"));
        setWeekMask(c.getLong(c.getColumnIndex("weekmask")));
        parent = c.getInt(c.getColumnIndex("parent"));
        caltoday = new RGMutableDateTime();
        lastIgnore = new RGDate(c.getLong(c.getColumnIndex("lastignore")));
    }
    
    public boolean isComplete() {
        return completion;
    }
    
    public boolean isRoot() {
        if ( id == parent ) return true;
        return false;
    }
    
    /**
     * The source contentvalues for the insert/update queries in save().
     * This was encapsulated so it could be overriden in subclasses.
     * (Because the same table holds the data for all the subclasses)
     */
    ContentValues preSaveHook() {
        type = getTypeId();
        if ( parent != PARENT_SET_ME_ROOT && 
                parent == UNDEFINED_PARENT )
            throw new RGRuntimeException("Parent not defined");
        
        ContentValues values = new ContentValues();
        values.put("name", title);
        values.put("complet", completion);
        values.put("type", type);
        values.put("relevance",relevance);
        values.put("weekmask",getWeekMask());
        values.put("parent", parent);
        values.put("created", created.getTime());
        values.put("lastignore", lastIgnore.getTime());
        return values;
    }
    
    void onSuccessfulUpdate() {
        sendEvents(this,EVENT_AFTER_UPDATE);
    }
    void onSuccessfulInsert() {
        if ( parent == PARENT_SET_ME_ROOT ) { 
            parent = id;
            ContentValues nv = new ContentValues();
            nv.put("parent", parent);
            try {
                Singleton.db_w.beginTransaction();
                String[] args = { Integer.toString(id) };
                Singleton.db_w.update(getTable(), nv, "_id = ?", args);
                Singleton.db_w.setTransactionSuccessful();
            } finally {
                Singleton.db_w.endTransaction();
            }
        }
        sendEvents(this, EVENT_NEW);
    }
    
    public void setTag(Object tag) {
        mTag = tag;
    }
    
    public Cursor queryType() {
        String[] selectionArgs = { Integer.toString(getTypeId()) };
        Cursor c = Singleton.db_r.query(getTable(), null, "type = ?",
                selectionArgs, null, null, null, null);
        return c;
    }
    
    public Cursor queryRoot() {
        Cursor c = Singleton.db_r.query(getTable(), null, "parent = _id",
                null, null, null, null, null);
        return c;
    }

    public void setComplete(Context context) {
        sendEvents(this, EVENT_COMPLETE);
        completion = true;
        update();
    }


    public void setRelevance(float rel) {
        if ( relevance < 0 ) return;
        relevance = rel;
        sendEvents(this, EVENT_ALTER_RELEVANCE);
        update();
    }

    public String toString() {
        String r = title;
        if ( completion ) r += " (complete)";
        r += "@"+ hashCode();
        return r;
    }
    
    @Override
    public void delete() {
        sendEvents(this, EVENT_REMOVE);
        super.delete();
    }

    public long getWeekMask() {
        return weekMask;
    }

    protected void setWeekMask(long weekMask) {
        this.weekMask = weekMask;
    }
    
    @Override
    public <T extends BaseDO> List<T> listMe(Cursor c) {
        List<T> list = new ArrayList<T>();
        while ( c.moveToNext() ) {
            @SuppressWarnings("unchecked")
            T obj = (T) newInstance(c);
            list.add(obj);
        }
        Collections.sort(list,Collections.reverseOrder());
        return list;
    }

    protected static void sendEvents(StepDO step,int event) {
        if (step.getId() == null) return;
        
        for ( Entry<String,OnEventListener> e : onEventListener.entrySet() ) {
            e.getValue().onEvent(step, event);
        }

        Iterator<Entry<String, OnEventListener>> it =
                step.localOnEventListener.entrySet().iterator();
        
        while ( it.hasNext() ) {
            Entry<String, OnEventListener> e = it.next();
            boolean consume = e.getValue().onEvent(step, event);
            if (consume) it.remove();
        }
        
    }

    public static void setOnEventListener(String key,
            OnEventListener onEventListener) {
        StepDO.onEventListener.put(key, onEventListener);
    }
    
    public OnEventListener getLocalOnEventListener(String tag) {
        return localOnEventListener.get(tag);
    }

    public void setLocalOnEventListener(String tag,
            OnEventListener list ) {
        localOnEventListener.put(tag,list);
    }

    public boolean isIgnored() {
        //fix to Joda "time gap exception"
        if ( lastIgnore == null || lastIgnore.getTime() == 0) return false;

        MutableDateTime calnow = new RGMutableDateTime();
        MutableDateTime calignore = new RGMutableDateTime();
        calignore.setTime(lastIgnore.getTime());
        calignore.setDate(lastIgnore.getTime());

        if ( calnow.getDayOfYear() != calignore.getDayOfYear() ) {
            return false;
        }
        return true;
    }

    public void setIgnored() {
        sendEvents(this, 8);
        lastIgnore = new RGDate();
        update();
    }
    
}
