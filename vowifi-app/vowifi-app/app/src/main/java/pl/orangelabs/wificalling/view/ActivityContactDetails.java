/*
 * Copyright (C) 2017 Orange Polska SA
 *
 * This file is part of WiFi Calling.
 *
 * WiFi Calling is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  WiFi Calling is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty o
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package pl.orangelabs.wificalling.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.ContactDetailsCallLogAdapter;
import pl.orangelabs.wificalling.content.ContactDetailsPhoneNumberAdapter;
import pl.orangelabs.wificalling.content.items.ContactDetailsConnectionItem;
import pl.orangelabs.wificalling.content.items.PhoneNumberItem;
import pl.orangelabs.wificalling.ctrl.RecyclerSeparatorDecoration;
import pl.orangelabs.wificalling.model.ContactDetails;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.model.db.AsyncGetCallLogByPhoneNumbers;
import pl.orangelabs.wificalling.model.db.AsyncGetContactDetailsById;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.ContactThumbnailTask;
import pl.orangelabs.wificalling.util.TextScaleTransition;

/**
 * @author Cookie
 */
public class ActivityContactDetails extends ActivityBase
{
    public static final String CONTACT_DETAILS_PARAM_ID = "CONTACT_ID_PARAM";
    public static final String CONTACT_DETAILS_PARAM_THUMBNAIL_URI = "CONTACT_THUMBNAIL_URI_PARAM";
    public static final String CONTACT_DETAILS_KEY = "CONTACT_ID_KEY";
    public static final int editContactRequestCode = 5;
    private final float[]
        mCallLogBtnInteractionLocationOnScreen =
        new float[] {-1.0f,
            -1.0f};
    private long mContactId;
    private TextView mContactName;
    private TextView mCallLogSummary;
    private ImageView mContactPhoto;
    private RecyclerView mCallLogView;
    private String mContactThumbnailUri;
    private Menu mMenu;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Transition makeEnterTransition()
    {
        Transition fade = new Fade();
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(R.id.toolbar, true);
        return fade;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Transition makeSharedElementEnterTransition(final Context context)
    {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);

        Transition changeBounds = new ChangeBounds();
        changeBounds.addTarget(R.id.view_contact_details_name);
        changeBounds.addTarget(R.id.view_contact_details_photo);
        set.addTransition(changeBounds);

        Transition textSize = new TextScaleTransition();
        textSize.addTarget(R.id.view_contact_details_name);
        set.addTransition(textSize);

        return set;
    }

    @Override
    protected void overrideTransitionAnim()
    {
        // nothing
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        initToolbar(false);

        if (savedInstanceState != null)
        {
            mContactId = savedInstanceState.getLong(CONTACT_DETAILS_KEY);
        }

        Bundle bundle = getIntent().getExtras();
        mContactId = bundle.getLong(CONTACT_DETAILS_PARAM_ID, -1);
        mContactThumbnailUri = bundle.getString(CONTACT_DETAILS_PARAM_THUMBNAIL_URI, null);

        SetView();
        LoadDetails();


    }

    private void LoadDetails()
    {
        if (mContactId != -1)
        {
            AsyncHelper.execute(new AsyncGetContactDetailsById(ActivityContactDetails.this, new OnGetContactCallback()), mContactId);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void SetView()
    {
        mContactName = (TextView) findViewById(R.id.view_contact_details_name);
        mContactPhoto = (ImageView) findViewById(R.id.view_contact_details_photo);
        mCallLogSummary = (TextView) findViewById(R.id.view_contact_details_call_log);

        mCallLogView = (RecyclerView) findViewById(R.id.view_contact_details_call_log_list);

        if (mContactThumbnailUri != null)
        {
//            mContactPhoto.setImageURI(Uri.parse(mContactThumbnailUri));
            AsyncHelper.execute(new ContactThumbnailTask(mContactPhoto, Uri.parse(mContactThumbnailUri), this, R.drawable.ic_avatar_full));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().setEnterTransition(makeEnterTransition());
            getWindow().setSharedElementEnterTransition(makeSharedElementEnterTransition(this));
            setEnterSharedElementCallback(new EnterSharedElementCallback(this));
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState, final PersistableBundle outPersistentState)
    {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putLong(CONTACT_DETAILS_KEY, mContactId);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        if (requestCode == editContactRequestCode)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                LoadDetails();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed()
    {
        if (mCallLogView.getVisibility() == View.VISIBLE)
        {
            HideCallLog();
        }
        else
        {
            supportFinishAfterTransition();
            //     super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        final boolean returnObj = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_contact_details, menu);
        mMenu = menu;
        return returnObj;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_contact_details_edit:
                EditClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void ShowCallLogView()
    {
        Animator anim;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
            float cx = mCallLogBtnInteractionLocationOnScreen[0] > 0.0f ? mCallLogBtnInteractionLocationOnScreen[0] : mCallLogView.getWidth() / 2.0f;
            float cy = mCallLogBtnInteractionLocationOnScreen[1] > 0.0f ? mCallLogBtnInteractionLocationOnScreen[1] : mCallLogView.getHeight() / 2.0f;

            float finalRadius = (float) Math.hypot(cx, cy);
            anim = ViewAnimationUtils.createCircularReveal(mCallLogView, (int) cx, (int) cy, 0, finalRadius);
        }
        else
        {
            anim = CreateAlphaAnimator(mCallLogView, 0f, 1f);
        }

        mCallLogView.setVisibility(View.VISIBLE);
        anim.start();
        HideMenu();


    }

    private void HideCallLog()
    {
        Animator anim;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
            float cx = mCallLogBtnInteractionLocationOnScreen[0] > 0.0f ? mCallLogBtnInteractionLocationOnScreen[0] : mCallLogView.getWidth() / 2.0f;
            float cy = mCallLogBtnInteractionLocationOnScreen[1] > 0.0f ? mCallLogBtnInteractionLocationOnScreen[1] : mCallLogView.getHeight() / 2.0f;
            float initialRadius = (float) Math.hypot(cx, cy);

            anim = ViewAnimationUtils.createCircularReveal(mCallLogView, (int) cx, (int) cy, initialRadius, 0);
        }
        else
        {
            anim = CreateAlphaAnimator(mCallLogView, 1f, 0f);
        }

// make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                mCallLogView.setVisibility(View.GONE);
                ShowMenu();

            }
        });
        anim.start();
    }

    private void HideMenu()
    {
        MenuItem item = mMenu.findItem(R.id.menu_contact_details_edit);
        item.setVisible(false);
    }

    private void ShowMenu()
    {
        MenuItem item = mMenu.findItem(R.id.menu_contact_details_edit);
        item.setVisible(true);
    }

    @NonNull
    private ObjectAnimator CreateAlphaAnimator(final View obj, final float startAlpha, final float endAlpha)
    {
        final ObjectAnimator animSheetAlpha = ObjectAnimator.ofFloat(obj, "alpha", startAlpha, endAlpha);
        animSheetAlpha.setDuration(100L);
        return animSheetAlpha;
    }

    private void onNumberEntryPressed(final PhoneNumberItem phoneNumberItem)
    {
        SipServiceCommand.makeCall(ActivityContactDetails.this, phoneNumberItem.mPhoneNumber.GetNumberForShow());
    }

    public void EditClick()
    {
        Log.v(this, "EditClick");

        Uri mSelectedContactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, mContactId);
        //final Uri mSelectedContactUri = ContactsContract.Contacts.getLookupUri(mContactId, ContactsContract.Contacts.LOOKUP_KEY);
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(mSelectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        editIntent.putExtra("finishActivityOnSaveCompleted", true);
        startActivityForResult(editIntent, editContactRequestCode);
    }

    private static class EnterSharedElementCallback extends SharedElementCallback
    {
        private static final String TAG = "EnterSharedElementCallback";
        private final float mStartTextSize;
        private final float mEndTextSize;

        public EnterSharedElementCallback(Context context)
        {
            Resources res = context.getResources();
            mStartTextSize = res.getDimensionPixelSize(R.dimen.text_size_default);
            mEndTextSize = res.getDimensionPixelSize(R.dimen.text_size_header_big);
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots)
        {
            Log.i(TAG, "=== onSharedElementStart(List<String>, List<View>, List<View>)");
            Stream.of(sharedElements).select(TextView.class).forEach(tv ->
            {
                tv.setScaleX(mStartTextSize / mEndTextSize);
                tv.setScaleY(mStartTextSize / mEndTextSize);
                tv.setPivotX(0.0f);
                tv.setPivotY(0.0f);
            });
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots)
        {
            Log.i(TAG, "=== onSharedElementEnd(List<String>, List<View>, List<View>)");
            Stream.of(sharedElements).select(TextView.class).forEach(v ->
            {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            });
        }
    }

    private class OnGetContactCallback implements AsyncGetContactDetailsById.OnContactDetailResultCallback
    {
        @Override
        public void OnLoadCompleteListener(final ContactDetails contactDetails)
        {
            mContactName.setText(contactDetails.mDisplayName);
            if (contactDetails.mPhotoBitmap != null)
            {
//                mContactPhoto.setImageBitmap(contactDetails.mPhotoBitmap);
                RoundedBitmapDrawable
                    drawable =
                    RoundedBitmapDrawableFactory.create(mContactPhoto.getContext().getResources(), contactDetails.mPhotoBitmap);
                drawable.setCircular(true);
                mContactPhoto.setImageDrawable(drawable);
//                AsyncHelper.execute(new ContactThumbnailTask(mContactPhoto, Uri.parse(contactDetails.mPhotoBitmap), this));
            }
            AsyncHelper
                .execute(new AsyncGetCallLogByPhoneNumbers(ActivityContactDetails.this, new OnGetCallLogCallback()), contactDetails.mPhoneNumberList);

            final RecyclerView contactNumbersView = (RecyclerView) findViewById(R.id.view_contact_details_phone_number);
            contactNumbersView.setItemAnimator(new DefaultItemAnimator());
            contactNumbersView.setLayoutManager(new LinearLayoutManager(ActivityContactDetails.this, LinearLayoutManager.VERTICAL, false));
            contactNumbersView.addItemDecoration(new RecyclerSeparatorDecoration(ActivityContactDetails.this));

            final ContactDetailsPhoneNumberAdapter adapter = new ContactDetailsPhoneNumberAdapter(ActivityContactDetails.this::onNumberEntryPressed);
            adapter.mDataSet.addAll(Stream.of(contactDetails.mPhoneNumberList).map(PhoneNumberItem::new).collect(Collectors.toList()));
            contactNumbersView.setAdapter(adapter);
        }
    }

    private class OnGetCallLogCallback implements AsyncGetCallLogByPhoneNumbers.OnCallLogByContactResultCallback
    {
        @Override
        public void OnLoadCompleteListener(final List<VWCallLog> callLogList)
        {
            if (callLogList == null)
            {
                mCallLogSummary.setVisibility(View.INVISIBLE);
                return;
            }
            final String callListText = getResources().getQuantityString(R.plurals.call_log, callLogList.size(), callLogList.size());
            mCallLogSummary.setText(callListText);
            mCallLogSummary.setVisibility(View.VISIBLE);
            mCallLogSummary.setOnClickListener(new OnShowCallLogClickListener());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // only needed for ripple anim
            {
                mCallLogSummary.setOnTouchListener(new OnCallLogTouchedListener());
            }

            mCallLogView.setItemAnimator(new DefaultItemAnimator());
            mCallLogView.setLayoutManager(new LinearLayoutManager(ActivityContactDetails.this, LinearLayoutManager.VERTICAL, false));

            final ContactDetailsCallLogAdapter adapter = new ContactDetailsCallLogAdapter(getApplicationContext());
            adapter.mDataSet.addAll(Stream.of(callLogList).map(ContactDetailsConnectionItem::new).collect(Collectors.toList()));
            mCallLogView.setAdapter(adapter);
        }
    }

    private class OnShowCallLogClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(final View v)
        {
            ShowCallLogView();

        }
    }

    private class OnCallLogTouchedListener implements View.OnTouchListener
    {
        @Override
        public boolean onTouch(final View v, final MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                mCallLogBtnInteractionLocationOnScreen[0] = event.getRawX();
                mCallLogBtnInteractionLocationOnScreen[1] = event.getRawY();
            }
            return false;
        }
    }
}
