package com.krueg.questgiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.views.TreeView;
import com.krueg.questgiver.views.TreeView_;

import org.androidannotations.annotations.EFragment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

@EFragment
public class Acts extends Fragment {
    
    public ViewGroup rootView;
    private TreeView mTreeList;
    
    private ImageView mPhoto;
    private ImageView mPin;
    private FrameLayout mPinGroup;
    private ImageView mProgBar;
    
    public static final int REQUEST_GET_IMAGE = 62;
    
    public static Acts newInstance() {
        Acts fragment = new Acts();
        return fragment;
    }

    public Acts() {
        // Required empty public constructor
    }

    public void setPinPosition(int pct) {
        RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) mPinGroup.getLayoutParams();
        int effectivew = mProgBar.getWidth() - mPinGroup.getWidth();
        params.leftMargin = (int) (effectivew * pct / 100F);
        mPinGroup.requestLayout();
    }
    

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StepDO.setOnEventListener("actRefresher", new StepDO.OnEventListener() {
            public boolean onEvent(final StepDO step, final int what) {
                getActivity().runOnUiThread( new Runnable() {
                    public void run() {
                        if ( step.getCompositeRelevance() > Progress.maxrel )
                            Progress.maxrel = step.getCompositeRelevance();
                        switch ( what ) {
                        
                            //events that request a UI remove
                            case StepDO.EVENT_COMPLETE:
                            case StepDO.EVENT_POSTPONE:
                            case StepDO.EVENT_REMOVE:
                                mTreeList.removeItem(step);
                                break;
                                
                            //events that require a reorder/redraw
                            case StepDO.EVENT_ALTER_RELEVANCE:
                            case StepDO.EVENT_ALTER_DEADLINE:
                            case StepDO.EVENT_WEEKLY_RESET:
                            case StepDO.EVENT_ACTIVATE:
                                //the events above are sent before the data is updated
                                //schedule one for after updating
                                step.setLocalOnEventListener( "actRefresher",
                                        new StepDO.OnEventListener() {
                                    
                                    @Override
                                    public boolean onEvent(StepDO step, int what) {
                                        if ( what != StepDO.EVENT_AFTER_UPDATE )
                                            return false;
                                        mTreeList.updateItem(step);
                                        return true;
                                    }
                                });
                                break;
                                
                            //events that require inserting a view
                            case StepDO.EVENT_NEW:
                                if ( !step.displayFilter() ) return;
                                if ( step.getRelevanceTimesCompletion()
                                        < Singleton.opt_threshold ) return;
                                mTreeList.insertItem(step);
                                break;
                        }
                    }
                });
                return false;
            }
        });
        
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode != Activity.RESULT_OK ) return;
        switch(requestCode) {
            case REQUEST_GET_IMAGE:
                int avatarSize = mPin.getWidth() - 4;
                final BitmapWorkerTask task = new BitmapWorkerTask(avatarSize,avatarSize);
                task.execute(data.getData());
                new Thread(new Runnable() { public void run() {
                    Bitmap out;
                    try {
                        out = task.get();
                        out = Singleton.crop(out);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return;
                        //if it sucks, do nothing
                    }
                    final Bitmap out2 = out;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPhoto.setImageBitmap(out2);
                        }
                    });
                }}).start();
                break;
        }
    }
    
    //also copied from the docs
    class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
        private Uri mUri;
        private int mWidth, mHeight;

        public BitmapWorkerTask(int width,int height) {
            mWidth = width;
            mHeight = height;
        }
        
        public int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {

            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;
        
            if (height > reqHeight || width > reqWidth) {
        
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }
        
            return inSampleSize;
        }
        
        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Uri... params) {
            mUri = params[0];

            //check size and downlample ratio
            InputStream stream = null;
            Bitmap newBmp = null;
            try {
                stream = getActivity().getContentResolver().openInputStream(mUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(stream,null,options);
                options.inSampleSize = calculateInSampleSize(options, mHeight, mWidth);
                options.inJustDecodeBounds = false;

                //need to reopen the stream
                stream.close();
                stream = getActivity().getContentResolver().openInputStream(mUri);
                
                Bitmap oBmp = BitmapFactory.decodeStream(stream,null,options);
                int smallerSize = Math.min(options.outWidth, options.outHeight);
                newBmp = Bitmap.createBitmap(oBmp, (options.outWidth-smallerSize)/2,
                        (options.outHeight-smallerSize)/2,
                        smallerSize, smallerSize);
                stream.close();
                
                //save the scaled down version
                FileOutputStream fos = null;
                fos = getActivity().openFileOutput(Singleton.SELFIE_FILE, Context.MODE_PRIVATE);

                if ( newBmp.compress(CompressFormat.JPEG, 70, fos) == false ) {
                    Toast.makeText(getActivity(), R.string.err_file_not_found, Toast.LENGTH_SHORT).show();
                    return null;
                }
                fos.close();
                
            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), R.string.err_file_not_found, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                Toast.makeText(getActivity(), R.string.err_file_not_found, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            }
            return newBmp;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_frprofile, container, false);
        ViewGroup activityLW = (ViewGroup) rootView.findViewById(R.id.activityLW);
        
        //PIN-PROGRESS BAR STUFF
        
        mProgBar = (ImageView) rootView.findViewById(R.id.progBar);
        mPinGroup = (FrameLayout) rootView.findViewById(R.id.pinGroup);
        mPin = (ImageView) rootView.findViewById(R.id.progressPin); 
        mPhoto = (ImageView) rootView.findViewById(R.id.progressAvatar);
        
        //crop and draw your picture inside the pin
        //getWidth is still zero here. Post to after layout pass
        mPin.post(new Runnable() { public void run() {
            int border = Singleton.dpToPx(4);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(mPin.getWidth(),
                            mPin.getWidth()-border);
            params.setMargins(0, border/2, 0, 0);
            mPhoto.setLayoutParams(params);
            mPhoto.requestLayout();
            Bitmap bmp;
            try {
                bmp = Singleton.loadPic(getActivity());
                bmp = Singleton.crop(bmp);
                mPhoto.setImageBitmap(bmp);
            } catch (FileNotFoundException | NullPointerException e) {}
        }});
        
        //click on pin to choose picture
        //response checked on "onActivityResult"
        mPin.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                if ( intent.resolveActivity(getActivity().getPackageManager()) != null )
                    startActivityForResult(intent,REQUEST_GET_IMAGE);

            }
        });
        
        //set pin position after layout pass
        mProgBar.post(new Runnable() { public void run() {
            setPinPosition((int)Progress.getCurrentValue());
        }});
        
        //set pin position on any underlying value change
        Progress.listener = new Progress.ProgressListener() {
            public void onUpdate(final float val) {
                getActivity().runOnUiThread( new Runnable() { public void run() {
                    setPinPosition((int) val);
                }});
            }
        };
        
        
        //STEP LIST STUFF

        Singleton.CommonTreeIF treeAdapter = new Singleton.CommonTreeIF();
        treeAdapter.setShowProject(true);
        mTreeList = new TreeView_(getActivity(), treeAdapter).
                setIdentMargin(0).setFlatInsert(true);
        mTreeList.setActionListeners
            (new Singleton.CommonAction(mTreeList, Acts.this));
        
        pop();

        activityLW.addView(mTreeList);
        
        return rootView;
    }
    
    public void pop(){
        
        //this was some ugly hack I made sometime to clean
        //broken databases
        List<StepDO> sList = null;
        while ( sList == null ) {
            try { sList = new StepDO().listMe(); }
            catch (PkNotFoundException e){ 
                sList = null;
            }
        }
        
        mTreeList.removeAllViews();
        
        for ( final StepDO step : sList ) {
            if ( !step.displayFilter() ) continue;
            if ( step.getRelevanceTimesCompletion() < Singleton.opt_threshold )
                continue;
            mTreeList.pushItem(step);
            if ( step.getCompositeRelevance() > Progress.maxrel )
                    Progress.maxrel = step.getCompositeRelevance();
        }       
        
    }
    
}
