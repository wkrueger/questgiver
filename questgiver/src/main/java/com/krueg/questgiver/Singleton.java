package com.krueg.questgiver;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.krueg.questgiver.Exceptions.RGTopLevelException;
import com.krueg.questgiver.StepSelectDF.StepSelectIF;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepParentDO;
import com.krueg.questgiver.views.StepView;
import com.krueg.questgiver.views.TreeView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Superglobal HORSEpower, as per the XGH methodology
 * <pre>
 * 
 *   |*-*-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-*|
 *   |                                       java.lang.NullPointerException     |
 *   |       ._._._._. ._._._._._._  ._._._._.                                  |
 *   |       | ;.... | |  ......   | |  .....>                                  |
 *   |       | |...| | |  |    |   | |  |.....                                 |
 *   |       | ------` |  |,;,;|   | |   ,.... |                                |
 *   |       | |       |...........| |----------\    Adelante!                  |
 *   |                                                                          |
 *   |                       Siga por sua conta e risco.     Pog <3             |
 *   |*-*-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-**-*|
 * </pre>
 */
public class Singleton {	

	public static final String SCHEME = "prgdata";
	
	public static final String SELFIE_FILE = "selfie";
	
	//public static boolean trigger_holdToEditTip = true;
	
	public static boolean opt_hideComplete = false;
	public static boolean opt_notification60 = true;
	public static boolean opt_notificationWeekly = true;
    public static boolean opt_notificationOnExpire = true;
	public static float opt_threshold = 0;
	public static int opt_dailyNotHour = 8;
	public static int opt_dailyNotMinute = 0;
	
	public static Context context;		//i have a strong feeling I'm commiting a terrible sin here
	
	public static class DemSQL extends SQLiteOpenHelper {

		public static final int DB_VERSION = 27;
		public static final String DB_NAME = "prg";

		public static final String TABLE_PERK_STEP = "perk_step";
		public static final String TABLE_ACT = "actlog";
		public static final String TABLE_NOTIFICATION = "notification";
		
		//hack-rule-of-thumb: any query outside the dataobject should have fields referenced
		//public static final String FIELD_STEP_REWARD__STEP = "step";
		//public static final String FIELD_NOTIFICATION__STEP = "step";
		//public static final String FIELD_NOTIFICATION__TYPE = "type";
		//public static final String FIELD_STEP_REWARD__RELEVANCE = "relevance";
		
		String[] INIT_QUERY = 
		{ 
				"CREATE TABLE perk_step ( _id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL DEFAULT 0, name TEXT CHECK (length(name) > 0), complet INTEGER NOT NULL, repeats INTEGER NOT NULL DEFAULT 0, totalrepeats INTEGER NOT NULL DEFAULT 0, lastrepeat INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL DEFAULT 0, deadline INTEGER NOT NULL DEFAULT 0, relevance REAL NOT NULL DEFAULT 1, lastignore INTEGER NOT NULL DEFAULT 0, weekmask INTEGER NOT NULL DEFAULT 0, parent INTEGER NOT NULL DEFAULT -1, FOREIGN KEY (parent) REFERENCES perk_step(_id) );",
				"CREATE TABLE actlog ( _id INTEGER PRIMARY KEY AUTOINCREMENT, creation INTEGER NOT NULL, descr TEXT NOT NULL );",
				"CREATE TABLE notification(_id INTEGER PRIMARY KEY AUTOINCREMENT, step INTEGER NOT NULL UNIQUE, title TEXT CHECK(length(title)>0), desc TEXT NOT NULL DEFAULT '', schedule INTEGER NOT NULL DEFAULT 0, checked INTEGER NOT NULL, type INTEGER NOT NULL, FOREIGN KEY (step) REFERENCES perk_step(_id) ON DELETE CASCADE, UNIQUE (step,type))"
		};
		
		public DemSQL(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			for ( String s : INIT_QUERY ) {
				db.execSQL(s);
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if ( oldVersion < 25 ) {
				String[] str = {
						//drop column notification_id
						"CREATE TABLE Tnotification(_id INTEGER PRIMARY KEY AUTOINCREMENT, step INTEGER NOT NULL UNIQUE, title TEXT CHECK(length(title)>4), desc TEXT NOT NULL DEFAULT '', schedule INTEGER NOT NULL DEFAULT 0, checked INTEGER NOT NULL, type INTEGER NOT NULL, FOREIGN KEY (step) REFERENCES perk_step(_id) ON DELETE CASCADE, UNIQUE (step,type))",
						"INSERT INTO Tnotification(_id,step,title,desc,schedule,checked,type) SELECT _id,step,title,desc,schedule,checked,type FROM notification",
						"DROP TABLE notification",
						"ALTER TABLE Tnotification RENAME TO notification"
				};
				db.beginTransaction();
				try {
					for ( String s : str) db.execSQL(s);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			else if ( oldVersion < 26 ) {
				String[] str = {
						"CREATE TABLE Tperk_step(_id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL DEFAULT 0, name TEXT CHECK (length(name) > 4), complet INTEGER NOT NULL, repeats INTEGER NOT NULL DEFAULT 0, totalrepeats INTEGER NOT NULL DEFAULT 0, lastrepeat INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL DEFAULT 0, deadline INTEGER NOT NULL DEFAULT 0, relevance REAL NOT NULL DEFAULT 1, lastignore INTEGER NOT NULL DEFAULT 0, weekmask INTEGER NOT NULL DEFAULT 0, parent INTEGER NOT NULL DEFAULT -1)",
						"INSERT INTO Tperk_step(_id, type , name , complet , repeats , totalrepeats , lastrepeat , created , deadline , relevance , lastignore , weekmask , parent) SELECT _id, type , name , complet , repeats , totalrepeats , lastrepeat , created , deadline , relevance , lastignore , weekmask , parent FROM perk_step",
						"DROP TABLE perk_step",
						"ALTER TABLE Tperk_step RENAME TO perk_step"
				};
				db.beginTransaction();
				try {
					for ( String s : str) db.execSQL(s);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}

            //change text size restriction from 5 to 1
			} else if (oldVersion < 27 ) {
                String[] str = {
                        "CREATE TABLE Tperk_step ( _id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL DEFAULT 0, name TEXT CHECK (length(name) > 0), complet INTEGER NOT NULL, repeats INTEGER NOT NULL DEFAULT 0, totalrepeats INTEGER NOT NULL DEFAULT 0, lastrepeat INTEGER NOT NULL DEFAULT 0, created INTEGER NOT NULL DEFAULT 0, deadline INTEGER NOT NULL DEFAULT 0, relevance REAL NOT NULL DEFAULT 1, lastignore INTEGER NOT NULL DEFAULT 0, weekmask INTEGER NOT NULL DEFAULT 0, parent INTEGER NOT NULL DEFAULT -1 );",
                        "INSERT INTO Tperk_step ( _id , type , name , complet , repeats , totalrepeats , lastrepeat , created , deadline , relevance , lastignore , weekmask , parent ) SELECT _id , type , name , complet , repeats , totalrepeats , lastrepeat , created , deadline , relevance , lastignore , weekmask , parent FROM perk_step;",
                        "DROP TABLE perk_step;",
                        "ALTER TABLE Tperk_step RENAME TO perk_step;",
                        "CREATE TABLE Tnotification (_id INTEGER PRIMARY KEY AUTOINCREMENT, step INTEGER NOT NULL UNIQUE, title TEXT CHECK(length(title)>0), desc TEXT NOT NULL DEFAULT '', schedule INTEGER NOT NULL DEFAULT 0, checked INTEGER NOT NULL, type INTEGER NOT NULL, FOREIGN KEY (step) REFERENCES perk_step(_id) ON DELETE CASCADE, UNIQUE (step,type));",
                        "INSERT INTO Tnotification ( _id , step , title , desc , schedule , checked , type ) SELECT _id , step , title , desc , schedule , checked , type FROM notification;",
                        "DROP TABLE notification;",
                        "ALTER TABLE Tnotification RENAME TO notification;"
                };
                db.beginTransaction();
                try {
                    for ( String s : str ) db.execSQL(s);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
		}
	}
	
	public static DemSQL dem;
	public static SQLiteDatabase db_r,db_w;
	
	public static void init(Context cont) {
		if ( context == null ) context = cont;
		if ( dem == null ) dem = new DemSQL(context);
		if ( db_w == null ) db_w = dem.getWritableDatabase();
		if ( db_r == null ) db_r = dem.getReadableDatabase();
	}
	
	/**
	 * Testing
	 */
	public static void killThemAll() {
		String kills[] = { 
				"DELETE FROM actlog",
				"DELETE FROM notification",
				"DELETE FROM perk_step"
		};
		
		db_w.beginTransaction();
		try {
			for ( String s : kills) db_w.execSQL(s);
			db_w.setTransactionSuccessful();
		} finally {
			db_w.endTransaction();
		}
	}
	
	/**
	 * If a string is empty, just return zero instead of throwing
	 * an error in parseint.
	 * 
	 * Use when parsing an inputText to number.
	 */
	public static int toInt(String str) {
		int r = 0;
		if ( str.length() > 0 ) r = Integer.parseInt(str);
		return r;
	}
	
	/**
	 * If a string is empty, just return zero instead of throwing
	 * an error in parsefloat.
	 * 
	 * Use when parsing an inputText to number.
	 */
	public static float toFloat(String str) {
		float r = 0;
		if ( str.length() > 0 ) r = Float.parseFloat(str);
		return r;
	}
	
	public static int dpToPx(float dp) {
		Resources r = context.getResources();
		return (int) TypedValue.applyDimension(
		        TypedValue.COMPLEX_UNIT_DIP,
		        dp, 
		        r.getDisplayMetrics()
		);
	}
	
	public static float pxToDp ( int px ) {
		Resources r = context.getResources();
		float ratio = TypedValue.applyDimension(
		        TypedValue.COMPLEX_UNIT_DIP,
		        1, 
		        r.getDisplayMetrics()
		);
		return (float)px/ratio;
	}
	
	public static String makeFragmentName(int viewId, int index)
	{
	     return "android:switcher:" + viewId + ":" + index;
	}
	
	/**
	 * For use in objects "carrying" data with getTag() and setTag()
	 * Tries to retrieve the specified tag object, if absent
	 * creates a new empty one and stores it.
	 * 
	 * @param v the object carrying the tag
	 * @param theClass the class that encapsulates the tag data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T,V> T getTag(V v,Class<T> theClass) {
		T tag;
		try {
			Method getTagMethod = v.getClass().getMethod("getTag", (Class<?>[])null);
			Method setTagMethod = v.getClass().getMethod("setTag", Object.class);
			Object res = getTagMethod.invoke(v, (Object[])null); 
		
			if ( res == null ) {
				tag = (T) theClass.newInstance();
				setTagMethod.invoke(v, tag);
			} else {
				tag = (T) res;
			}
		} catch ( InstantiationException | IllegalAccessException | NoSuchMethodException
				| IllegalArgumentException | InvocationTargetException e ) {
			throw new RuntimeException(e);
		}
		return tag;
	
	}
	
	public interface ViewTreeExecIF {
		public Object exec(View v);
	}
	
	public static Object viewTreeExec(ViewGroup root,ViewTreeExecIF exec) {
		Object out = null;
		for ( int it = 0 ; it < root.getChildCount() ;  it++ ) {
			View child = root.getChildAt(it);
			if ( child instanceof LinearLayout) out = viewTreeExec((ViewGroup) child,exec);
			else out = exec.exec(child);
			if ( out != null ) return out;
		}
		return out;
	}
	
	//StepDO -> StepView "adapter" for TreeView
	public static class CommonTreeIF extends TreeView.TreeViewIF {

        protected boolean mShowProject;

        public CommonTreeIF() {
            mShowProject = false;
        }

        public void setShowProject(boolean set) {
           mShowProject = set;
        }

		public List<View> getChildren(View v) {
			StepView view = (StepView) v;
			if ( !(view.getStep() instanceof StepParentDO) )
				return new ArrayList<View>();
			StepParentDO step = (StepParentDO) view.getStep();
			List<StepDO> children = step.listMe(step.queryChildren());
			List<View> stepList = new ArrayList<View>(children.size());
			for ( StepDO child : children ) {
				if ( Singleton.opt_hideComplete == true && child.isComplete())
					continue;
				View sv = createView(child);
				stepList.add(sv);
			}
			return stepList;
		}

		@Override
		public Object getObject(View v) {
            return ((StepView) v).getStep();
		}

		@Override
		public void refreshView(final View v) {
            Handler ui = new Handler(Looper.getMainLooper());
            ui.post( new Runnable() {
                @Override
                public void run() {
                    StepView sv = (StepView) v;
                    sv.setStep(StepDO.newInstance(sv.getStep().getId()));
                }
            });
		}

		@Override
		public Object getObjectParent(Object o) {
			StepDO out;
			try {
				out = ((StepDO) o).getParent();
			} catch (RGTopLevelException e) {
				return Integer.valueOf(-1);
			}
			return out;
		}

		@Override
		public int compareObjects(Object o1, Object o2) {
			try {
			StepDO s1 = (StepDO) o1;
			StepDO s2 = (StepDO) o2;
			return s1.compareTo(s2); }
			catch ( ClassCastException e ) { return -1; }
		}

		@Override
		public View createView(Object o) {
			StepView sv = new StepView(context);
			sv.setStep((StepDO) o);
			sv.setShowProject(mShowProject);
			return sv;
		}
	}
	
	static class CommonAction extends TreeView.ActionListeners {
		Fragment mFrag;
		StepSelectIF mStepIF;
		
		CommonAction(TreeView t,Fragment f) {
			t.super();
			mFrag = f;
		}
		
		CommonAction(TreeView t,Fragment f, StepSelectIF ifs) {
			this(t,f);
			mStepIF = ifs;
		}
		
		public boolean longClick(View v) {
			StepView sv = (StepView) v;
			StepSelectDF sdf = StepSelectDF.newInstance(sv.getStep(), mStepIF);
			sdf.show(mFrag.getFragmentManager(), "stepselect");
			return true;
		}
	}
	
	public static Bitmap crop(Bitmap uncropped) {
		int width = uncropped.getWidth();
		int height = uncropped.getHeight();
		Bitmap croppedImage = Bitmap.createBitmap(width, height,
			Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(croppedImage);
		Rect dstRect = new Rect(0, 0, width, height);
		canvas.drawBitmap(uncropped, null, dstRect, null);
		uncropped.recycle();
		Path p = new Path();
		p.addCircle(width / 2F, height / 2F, width / 2F,
			Path.Direction.CW);
		canvas.clipPath(p, Region.Op.DIFFERENCE);
		canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
		return croppedImage;
	}
	
	public static Bitmap loadPic(Context context) throws FileNotFoundException {
		FileInputStream fis = null;
		fis = context.openFileInput(Singleton.SELFIE_FILE);
		if ( fis != null ) {
			Bitmap out =  BitmapFactory.decodeStream(fis);
			if ( out == null ) throw new FileNotFoundException();
			return out;
		} else throw new FileNotFoundException();
	}
	
}