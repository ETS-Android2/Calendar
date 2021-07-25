package com.android.calendar.event;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import java.util.Objects;

/**
 * 반복일정삭제대화창 (`이 일정만`, `현재 및 이후 일정`, `모든 일정`)
 */
public class DeletionDialog extends Dialog implements View.OnClickListener {
    //자식 view들
    Button mBtnOk, mBtnCancel;
    RadioButton mBtnDeleteThis, mBtnDeleteFollowing, mBtnDeleteAll;

    //선택된 삭제형식
    int mWhich = -1;

    //`현재 및 이후 일정`을 보여주겠는가?
    boolean mShowFollowing = true;

    //삭제단추동작을 감지하는 listener
    DeleteClickedListener mDeletedListener;

    public DeletionDialog(@NonNull Context context) {
        super(context);
    }

    public DeletionDialog(@NonNull Context context, int themeResId, boolean showFollowing) {
        super(context, themeResId);
        mShowFollowing = showFollowing;
    }

    protected DeletionDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.deletion_dialog);

        //자식 view들 얻기
        mBtnDeleteThis = findViewById(R.id.delete_this);
        mBtnDeleteAll = findViewById(R.id.delete_all);
        mBtnDeleteFollowing = findViewById(R.id.delete_following);
        if(!mShowFollowing)
            mBtnDeleteFollowing.setVisibility(View.GONE);

        mBtnOk = findViewById(R.id.button_ok);
        mBtnCancel = findViewById(R.id.button_cancel);
        mBtnOk.setEnabled(false);

        mBtnDeleteThis.setOnClickListener(this);
        mBtnDeleteFollowing.setOnClickListener(this);
        mBtnDeleteAll.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);

        Utils.addCommonTouchListener(mBtnOk);
        Utils.addCommonTouchListener(mBtnCancel);
        Utils.addCommonTouchListener(mBtnDeleteThis);
        Utils.addCommonTouchListener(mBtnDeleteFollowing);
        Utils.addCommonTouchListener(mBtnDeleteAll);
    }

    @Override
    public void onStart() {
        super.onStart();

        //대화창위치를 화면 하단 위치로 설정
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = getWindow();
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        Objects.requireNonNull(window).setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        //Radio Group에서 선택이 바뀌였을때
        if(v instanceof RadioButton) {
            if(mWhich == -1) {
                mBtnOk.setEnabled(true);
            }

            if(v == mBtnDeleteThis) {
                mWhich = 0;
            }
            else if(v == mBtnDeleteFollowing) {
                mWhich = 1;
            }
            else if(v == mBtnDeleteAll) {
                mWhich = 2;
            }
        }

        //삭제단추를 눌렀을때
        else if(v == mBtnOk) {
            dismiss();
            if(mDeletedListener != null)
                mDeletedListener.onClicked(mWhich);
        }

        //취소단추를 눌렀을때
        else if(v == mBtnCancel) {
            dismiss();
        }
    }

    public void setDeleteClickedListener(DeleteClickedListener listener) {
        mDeletedListener = listener;
    }

    public interface DeleteClickedListener {
        void onClicked(int which);
    }
}
