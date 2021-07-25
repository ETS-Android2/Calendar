package com.android.calendar.kr.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.krcalendar.R;

import static com.android.calendar.kr.big.BigCalendarView.DAYS_PER_ROW;

/**
 * 양식3
 * 한개 월의 월날자표시부분에 해당한 ViewPagerAdapter
 * 페지개수 2(상하)
 */
public class OneMonthPagerAdapter extends RecyclerView.Adapter<OneMonthPagerAdapter.ViewHolder> {

    private final Context mContext;
    private final BigCalendarView.BigCalendarViewDelegate mDelegate;
    private final int mYear;
    private final int mMonth;
    private final int mViewWidth;
    private final int mViewHeight;

    public OneMonthPagerAdapter(Context context, BigCalendarView.BigCalendarViewDelegate delegate, int year, int month, int viewWidth, int viewHeight) {
        mContext = context;
        mDelegate = delegate;
        mYear = year;
        mMonth = month;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.one_month_page_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecyclerView recyclerView = (RecyclerView) holder.itemView;
        BigMonthMainViewAdapter adapter = new BigMonthMainViewAdapter(mContext, mDelegate, mYear, mMonth, mViewWidth, mViewHeight, position);

        //Adapter, LayoutManager 설정
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, DAYS_PER_ROW));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
