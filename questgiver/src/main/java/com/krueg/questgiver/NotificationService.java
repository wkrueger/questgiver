package com.krueg.questgiver;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.dataobject.NotificationDO;

import java.io.FileNotFoundException;
import java.util.List;

public class NotificationService extends IntentService {

	public NotificationService() {
		super("prgNotificationService");
	}

    public static final int STEP_NOTIFICATION_GROUP = 0;
    public static final int ABANDON_NOTIFICATION_GROUP = 1;

    private Bitmap mBigIconCache = null;

    private Bitmap getBigIcon() {

        if (mBigIconCache != null) return mBigIconCache;

        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        ViewGroup pingroup = (ViewGroup) li.inflate(R.layout.sub_pingroup, null);
        final ImageView mPhoto = (ImageView) pingroup.findViewById(R.id.progressAvatar);
        final ImageView mPin = (ImageView) pingroup.findViewById(R.id.progressPin);

        try {
            Bitmap bmp = Singleton.loadPic(this);
            bmp = Singleton.crop(bmp);
            mPhoto.setImageBitmap(bmp);
        } catch (FileNotFoundException e) {}

        int height = MeasureSpec.makeMeasureSpec(Singleton.dpToPx(42), MeasureSpec.EXACTLY);
        int width = MeasureSpec.makeMeasureSpec(Singleton.dpToPx(42), MeasureSpec.EXACTLY);
        pingroup.measure(width, height);
        int pinheight = MeasureSpec.makeMeasureSpec(Singleton.dpToPx(42), MeasureSpec.EXACTLY);
        mPin.measure(LayoutParams.WRAP_CONTENT,pinheight);
        int photoheight = MeasureSpec.makeMeasureSpec(Singleton.dpToPx(34), MeasureSpec.EXACTLY);
        mPhoto.measure(photoheight,photoheight);
        pingroup.layout(0, 0, pingroup.getMeasuredWidth(), pingroup.getMeasuredHeight());

        int border = Singleton.dpToPx(2);
        mPhoto.layout(border, border,
                Singleton.dpToPx(34)+border/2, Singleton.dpToPx(34)+border/2);

        Bitmap b = Bitmap.createBitmap(pingroup.getMeasuredWidth(),
                pingroup.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        pingroup.draw(c);

        //Bitmap largeIcon = BitmapFactory
        //		.decodeResource(getResources(), R.drawable.ic_notification);

        mBigIconCache = b;

        return b;
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		
		Singleton.init(getApplicationContext());

		NotificationDO dummy = new NotificationDO();
		List<NotificationDO> nList = dummy.listMe(NotificationDO.queryUnchecked());

        String cinfo = "" + nList.size();


		if (nList.size() > 0) {
			NotificationDO not = nList.get(0); 
			String mainText = not.title;
			String smallText = not.desc;
			
			//big notification: 4.1+ when at the top of the queue
			NotificationCompat.InboxStyle inboxStyle = 
					new NotificationCompat.InboxStyle()
				.setBigContentTitle(nList.size() + " " + getString(R.string.alerts))
				.setSummaryText(getString(R.string.discipline) + ": " +
						Math.round(Progress.getCurrentValue()));

			for ( int it = 0 ; it < 4 ; it++ ) {
				if ( it >= nList.size() ) break;
				NotificationDO tNot = nList.get(it);
				String shortType = getString(tNot.getShortTypeString());
				SpannableStringBuilder sb = new SpannableStringBuilder();
				sb.append("(").append(shortType).append(")");
				sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
						0, shortType.length()+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				sb.append(" ").append(tNot.title);
				inboxStyle.addLine(sb);
			}
			
			
			//action on notification click
			Intent resultIntent = new Intent(this, Notification.class);
			PendingIntent resultPendingIntent =
				PendingIntent.getActivity(this, 0, resultIntent, 
						PendingIntent.FLAG_CANCEL_CURRENT);
			
			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
				.setContentTitle(mainText)
				.setContentText(smallText)
				.setSmallIcon(R.drawable.ic_hide_complete_step_white)
				.setLargeIcon(getBigIcon())
				.setContentIntent(resultPendingIntent)
				.setContentInfo(cinfo)
				.setSound(RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				.setStyle(inboxStyle);
			
			NotificationManager nMgr = 
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nMgr.notify(STEP_NOTIFICATION_GROUP, mBuilder.build());
		
		}


        NotificationDO callerNotif = null;
        try {
            Uri uriData = intent.getData();
            String callerNotifStr = uriData.getQueryParameter("nid");
            callerNotif = new NotificationDO(Integer.parseInt(callerNotifStr));
        } catch (RGRuntimeException | NumberFormatException e) {
            //swallow
        }

        if (callerNotif == null) return;

        if (callerNotif.type == NotificationDO.TYPE_ABANDONED && Progress.getCurrentValue() < 10) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);

            mBuilder.setContentTitle(   getString(R.string.notif_abandon_title) )
                    .setContentText(    getString(R.string.notif_abandon_text) )
                    .setSmallIcon(R.drawable.ic_hide_complete_step_white)
                    .setLargeIcon(getBigIcon())
                    .setContentInfo(cinfo)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            NotificationManager nMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nMgr.notify(ABANDON_NOTIFICATION_GROUP,mBuilder.build());

        }

	}
	
}
