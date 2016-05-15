package com.krueg.questgiver.dataobject;

import android.content.ContentValues;
import android.database.Cursor;

import com.krueg.questgiver.Exceptions.EmptyCursorException;
import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Newbie data-object and database layer<br/><br/>
 * 
 * CONCEPT:<br/>
 * <b>Saving a new object:</b> Create it with the void contructor. Change public vars
 * and/or use setters, then call save(). Each setter will also call save (convention).<br/>
 * <b>Querying an existant object</b> Create it with the primary key or Cursor contructors.
 * Provided cursors must have all the table's columns and be from the proper table.
 * Only the current cursor position will be used. Use the Singleton.listMe statics
 * for getting lists.<br/>
 * <b>Editing</b> Call save in an object created with the pk or cursor constructor.<br/></br/>
 * 
 * EXTENDING:<br/>
 * Implement getTable(), save (cut-copy) and init(). In these methods, copy the data to/from
 * the DB to the class fields. Upon defining a constructor, probably call the super.
 */
public abstract class BaseDO implements Comparable<BaseDO> {

	Integer id;
	
	public Integer getId() {
		return id;
	}
	
	public BaseDO() {}
	
	public BaseDO (Cursor c) {
		if ( c.getCount() == 0 ) throw new EmptyCursorException();
		if ( c.getPosition() == -1 ) c.moveToNext();
		id = c.getInt(c.getColumnIndex("_id"));
		initialize(c);
	}
	
	public BaseDO (int pk)  {
		String[] args = { Integer.toString(pk) };
		Cursor c = Singleton.db_r.query(getTable(), null, "_id = ?", args, null, null, null);
		if ( c.getPosition() == -1 ) c.moveToNext();
		if ( c.getCount() == 0 ) throw new PkNotFoundException();
		id = c.getInt(c.getColumnIndex("_id"));
		initialize(c);
	}
	
	public abstract String getTable();
	
	abstract ContentValues preSaveHook();
	void onSuccessfulInsert() {}
	void onSuccessfulUpdate() {}
	
	public Integer create() {
		
		ContentValues values = preSaveHook();
		
		if ( id == null ){
			try {
				Singleton.db_w.beginTransaction();
				id = (int)Singleton.db_w.insertOrThrow(getTable(), null, values);
				Singleton.db_w.setTransactionSuccessful();
			}
			finally {
				Singleton.db_w.endTransaction();
			}
			
			//on new step successfully created 
			if ( id != -1 ) {
				onSuccessfulInsert();
			}
		} else throw new RGRuntimeException("Trying to insert into existing step");
		return id;
	}
	
	public void update() {
		
		ContentValues values = preSaveHook();
		
		if ( id != null ) {
			try {
				Singleton.db_w.beginTransaction();
				String[] args = { Integer.toString(id) };
				Singleton.db_w.update(getTable(), values , "_id = ?", args);
				Singleton.db_w.setTransactionSuccessful();
			}
			finally {
				Singleton.db_w.endTransaction();
			}
			
			//on step updated
			onSuccessfulUpdate();
			
		}
	}
	
	/**
	 * Given a queried line, fill up the object
	 * @param c Internal parameter
	 */
	abstract void initialize(Cursor c);
	
	public void delete() {
		if ( id == null) return;
		try {
			Singleton.db_w.beginTransaction();
			String[] args = { Integer.toString(id) };
			Singleton.db_w.delete(getTable(), "_id = ?", args);
			Singleton.db_w.setTransactionSuccessful();
		} finally {
			Singleton.db_w.endTransaction();
		}
	}
	
	@Override
	public int compareTo(BaseDO another) {
		if ( another.id == id ) return 0;
		Integer b = another.id;
		return b.compareTo(id);
	}
	
	public <T extends BaseDO> List<T> listMe() {
		Cursor c = Singleton.db_r.query(getTable(), null, null, null,
				null, null, null, null);
		return listMe(c);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BaseDO> List<T> listMe(Cursor c) {
		List<T> list = new ArrayList<T>();
		while ( c.moveToNext() ) {
			T obj;
			try {
				obj = (T) this.getClass().getConstructor(Cursor.class).newInstance(c);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			list.add(obj);
		}
		Collections.sort(list);
		return list;
	}
	
	@Override
	public boolean equals(Object o) {
		if ( this.getClass() != o.getClass() ) return false;
		return ((BaseDO)o).id.equals(id);
	}
	
	@SuppressWarnings("static-method")
	public String getSQLOrderClause() {
		return "_id ASC";
	}

}