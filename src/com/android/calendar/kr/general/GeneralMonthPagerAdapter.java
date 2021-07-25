package com.android.calendar.kr.general;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 양식 2
 * 월별 날자현시 viewpager를 위한 adapter
 */
public class GeneralMonthPagerAdapter extends RecyclerView.Adapter<GeneralMonthPagerAdapter.ViewHolder> {
    private final Context mContext;
    CalendarController mController;
    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    ViewPager2 mViewPager;

    //총 페지개수
    private final int mMonthCount;

    public GeneralMonthPagerAdapter(Context context, CalendarViewDelegate delegate, ViewPager2 viewPager) {
        mContext = context;
        mController = CalendarController.getInstance(context);
        mDelegate = delegate;
        mViewPager = viewPager;
        mMainActivity = AllInOneActivity.getMainActivity(context);

        if(mController.getTime() >= 0) {
            DateTime dateTime = new DateTime(mController.getTime());
            if(mDelegate.mSelectedCalendar != null) {
                mDelegate.mSelectedCalendar.setYear(dateTime.getYear());
                mDelegate.mSelectedCalendar.setMonth(dateTime.getMonthOfYear());
                mDelegate.mSelectedCalendar.setDay(dateTime.getDayOfMonth());
            }
            else{
                Calendar calendar = new Calendar();
                calendar.setYear(dateTime.getYear());
                calendar.setMonth(dateTime.getMonthOfYear());
                calendar.setDay(dateTime.getDayOfMonth());
                mDelegate.mSelectedCalendar = calendar;
            }
        }

        //총 페지개수를 계산한다.
        mMonthCount = 12 * (mDelegate.getMaxYear() - mDelegate.getMinYear() + 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.general_month_view_container, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final int year = (i + mDelegate.getMinYearMonth() - 1) / 12 + mDelegate.getMinYear();
        final int month = (i + mDelegate.getMinYearMonth() - 1) % 12 + 1;

        GeneralMonthViewContainer view = (GeneralMonthViewContainer) viewHolder.itemView;
        view.setup(mDelegate, year, month, i);
        view.setTag(i);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        GeneralMonthViewContainer view = (GeneralMonthViewContainer) holder.itemView;

        int curPage = mViewPager.getCurrentItem();
        int page = (int) view.getTag();
        if(curPage != page) {
            view.getMonthView().removeTodayAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return mMonthCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
