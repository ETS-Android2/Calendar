package com.android.calendar.agenda;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;
import com.android.krcalendar.R;
import com.wang.avi.AVLoadingIndicatorView;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

/**
 * 일정목록화면
 */
public class AgendaFragment extends Fragment
        implements CalendarController.EventHandler, View.OnClickListener, AgendaFragmentContainerView.TransitionEndListener {
    //Activity가 재창조될때 상태들을 보관하기 위한 이름상수들
    static final String START_DATE_KEY = "start_date_key";
    static final String END_DATE_KEY = "end_date_key";
    static final String QUERY_KEY = "query_key";

    //기본화면 activity
    private AllInOneActivity mMainActivity;

    //Fragment의 root view
    AgendaFragmentContainerView mView;

    //자식 view들
    View mBackButton;
    EditText mSearchText;
    View mStartPrevBtn, mStartNextBtn, mEndPrevBtn, mEndNextBtn;
    TextView mStartDateView, mEndDateView;
    TextView mTodayButton;

    //시작날자, 마감날자, 검색어
    DateTime mStartDate, mEndDate;
    String mQuery = "";

    //onCreateView()에서 한번 true로 설정된다.
    //시작에 Query가 변하므로 하여 불필요하게 ViewModel에 같은 자료를 설정하는것을 피하기 위해 리용된다.
    boolean mManualQueryChange = false;

    //일정목록을 얻는 시간동안 loading view를 보여준다.
    AVLoadingIndicatorView mLoadingView;
    boolean mFirstLoad = true;

    //일정목록을 현시하는 recyclerview와 adapter, layoutmanager
    RecyclerView mRecyclerView;
    AgendaListViewAdapter mAdapter;
    LinearLayoutManager mLayoutManager;

    //일정목록을 얻기 위한 viewmodel
    AgendaViewModel mViewModel;

    public AgendaFragment(){
        mStartDate = DateTime.now();
        mEndDate = DateTime.now();
        mQuery = "";
    }

    /**
     * 시작날자, 마감날자를 받는다.
     * @param timeMillis 미리초시간, startTime|endTime이 null로 들어오면 이것이 시작날자/마감날자로 설정된다.
     * @param startTime 시작날자
     * @param endTime 마감날자
     */
    public AgendaFragment(long timeMillis, Time startTime, Time endTime, String query){
        DateTime dateTime = new DateTime(timeMillis);
        if(startTime == null){
            mStartDate = new DateTime(dateTime).withMillisOfDay(0);
        }
        else {
            mStartDate = new DateTime(startTime.toMillis(true)).withMillisOfDay(0);
        }

        if (endTime == null) {
            mEndDate = new DateTime(mStartDate).withMillisOfDay(0);
        }
        else{
            mEndDate = new DateTime(endTime.toMillis(true)).withMillisOfDay(0);
        }
        mQuery = query;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ViewModel창조
        mViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(mMainActivity.getApplication(), this)).get(AgendaViewModel.class);

        //Fragment가 재창조되였을때 보관된 값들을 얻는다.
        if(savedInstanceState != null) {
            mStartDate = (DateTime) savedInstanceState.getSerializable(START_DATE_KEY);
            mEndDate = (DateTime) savedInstanceState.getSerializable(END_DATE_KEY);
            mQuery = (String) savedInstanceState.getSerializable(QUERY_KEY);
        }

        //Viewmodel에 시작날자, 마감날자를 설정한다.
        mViewModel.setInputData(mStartDate, mEndDate, mQuery);
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(START_DATE_KEY, mStartDate);
        outState.putSerializable(END_DATE_KEY, mEndDate);
        outState.putSerializable(QUERY_KEY, mQuery);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainActivity.updateVisibility();

        mView = (AgendaFragmentContainerView) inflater.inflate(R.layout.agenda_fragment, container, false);
        initViews();

        mStartDateView.setText(getDateString(mStartDate));
        mEndDateView.setText(getDateString(mEndDate));

        mManualQueryChange = true;
        mSearchText.setText(mQuery);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mView.setTransitionEndListener(this);

        //Fragment의 transition animation이 진행되고있다면 loading view를 현시한다.
        boolean showLoading = Utils.isToAgendaTransition();
        if(showLoading) {
            mLoadingView.smoothToShow();
            mRecyclerView.setVisibility(View.INVISIBLE);
        }

        //그렇지 않다면 view갱신을 한다.
        else {
            onTransitionEnd();
        }
    }

    private void initViews(){
        //자식 view들을 얻는다.
        mBackButton = findViewById(R.id.back_button);
        mSearchText = findViewById(R.id.search_text);
        mStartDateView = findViewById(R.id.start_date_view);
        mEndDateView = findViewById(R.id.end_date_view);
        mStartPrevBtn = findViewById(R.id.start_date_prev);
        mStartNextBtn = findViewById(R.id.start_date_next);
        mEndPrevBtn = findViewById(R.id.end_date_prev);
        mEndNextBtn = findViewById(R.id.end_date_next);
        mTodayButton = findViewById(R.id.today_button);
        mLoadingView = findViewById(R.id.loading_message);
        mRecyclerView = findViewById(R.id.agenda_list_view);

        //단추눌림, text변화사건동작들을 추가한다.
        mStartPrevBtn.setOnClickListener(this);
        mStartNextBtn.setOnClickListener(this);
        mEndPrevBtn.setOnClickListener(this);
        mEndNextBtn.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mTodayButton.setOnClickListener(this);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(mManualQueryChange) {
                    mManualQueryChange = false;
                    return;
                }

                String query = s.toString();
                mQuery = query;
                mViewModel.setQuery(query);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        Utils.addCommonTouchListener(mBackButton);
        Utils.addCommonTouchListener(mTodayButton);

        View searchView = findViewById(R.id.assist_search_box);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //검색어입력칸의 parent view에 click했을때 입력칸에 초점이 가도록 한다.
                mSearchText.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(mSearchText, 0);
            }
        });

        mStartDateView.setText(getDateString(mStartDate));
        mEndDateView.setText(getDateString(mEndDate));

        mLayoutManager = new LinearLayoutManager(mMainActivity, RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new AgendaListViewAdapter(getContext());
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        mMainActivity = AllInOneActivity.getMainActivity(context);
        super.onAttach(context);
    }

    /**
     * @param dateTime: 날자
     * @return 날자문자렬을 돌려준다(2021.1.1)
     */
    static String getDateString(DateTime dateTime){
        @SuppressLint("DefaultLocale") String dateString = String.format("%1$d.%2$02d.%3$02d", dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
        return dateString;
    }

    /**
     * 시작날자-1년전으로
     */
    private void onStartDatePrev() {
        mStartDate = mStartDate.minusYears(1);
        mStartDateView.setText(getDateString(mStartDate));
        mViewModel.setStartDate(mStartDate);
    }

    /**
     * 시작날자-1년후로
     */
    private void onStartDateNext() {
        DateTime dateTime = mStartDate.plusYears(1);
        if(dateTime.isAfter(mEndDate)){
            return;
        }

        mStartDate = dateTime;
        mStartDateView.setText(getDateString(mStartDate));
        mViewModel.setStartDate(mStartDate);
    }

    /**
     * 마감날자-1년전으로
     */
    private void onEndDatePrev() {
        DateTime dateTime = mEndDate.minusYears(1);
        if(dateTime.isBefore(mStartDate)){
            return;
        }

        mEndDate = dateTime;
        mEndDateView.setText(getDateString(mEndDate));
        mViewModel.setEndDate(mEndDate);
    }

    /**
     * 마감날자-1년후로
     */
    private void onEndDateNext() {
        mEndDate = mEndDate.plusYears(1);
        mEndDateView.setText(getDateString(mEndDate));
        mViewModel.setEndDate(mEndDate);
    }

    /**
     * 오늘단추를 눌렀을때
     */
    private void onToday() {
        //Recyclerview를 scroll 하여 오늘의 일정항목까지 이동한다.
        DateTime dateTime = new DateTime();
        DateTime todayDate = new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 0, 0);

        if(mAdapter.getItemCount() == 0)
            return;

        int todayPos = mAdapter.getDayPosition(todayDate);
        mRecyclerView.scrollToPosition(todayPos);
    }

    /**
     * 입력날자의 일정항목까지 이동한다.
     * @param dateTime: 날자
     */
    public void onSelectDate(DateTime dateTime){
        int pos = mAdapter.getDayPosition(dateTime);
        mRecyclerView.scrollToPosition(pos);
    }

    /* 시작날자, 마감날자시간의 미리초수*/
    public long getStartMillis (){
        return mStartDate.getMillis();
    }

    public long getEndMillis (){
        return mEndDate.getMillis();
    }

    @Override
    public long getSupportedEventTypes() {
        return 0;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public void minuteChanged() {
    }

    public String getQuery() {
        return mQuery;
    }

    public <T extends View> T findViewById(int id){
        if(mView != null)
            return mView.findViewById(id);
        return null;
    }

    @Override
    public void onClick(View v) {
        if(v == mBackButton) {
            //건반을 숨기고 종료한다.
            View view = mMainActivity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            mMainActivity.onBackPressed();
        }

        /* 단추 click동작들 */
        else if(v == mStartPrevBtn) {
            onStartDatePrev();
        }

        else if(v == mStartNextBtn) {
            onStartDateNext();
        }

        else if(v == mEndPrevBtn) {
            onEndDatePrev();
        }

        else if(v == mEndNextBtn) {
            onEndDateNext();
        }

        else if(v == mTodayButton){
            onToday();
        }
    }

    @Override
    public void onTransitionEnd() {
        mViewModel.getEventList().observe(getViewLifecycleOwner(), eventList -> {
            mAdapter.setQuery(mQuery);

            //loading view를 숨긴다.(처음 한번만 호출된다)
            if(mFirstLoad) {
                mLoadingView.smoothToHide();
                mRecyclerView.setVisibility(View.VISIBLE);
                mFirstLoad = false;
            }

            //Recyclerview를 갱신한다.
            mAdapter.redrawChildren();
            mAdapter.submitList(eventList);
        });
    }
}
