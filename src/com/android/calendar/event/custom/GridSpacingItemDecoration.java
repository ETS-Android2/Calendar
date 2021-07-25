package com.android.calendar.event.custom;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 일정편집화면 - 일정형식선택에서 매 항목view들의 위치들을 설정해주기 위한 ItemDecoration클라스이다.
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;          //column개수
    private int spacing;            //여백
    private boolean includeEdge;    //테두리를 포함하겠는가?

    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // Adapter위치
        int column = position % spanCount; // column 위치

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)
        } else {
            outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
            outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
        }
    }
}