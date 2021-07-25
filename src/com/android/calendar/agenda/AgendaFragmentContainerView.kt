package com.android.calendar.agenda

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.android.calendar.utils.Utils
import com.android.krcalendar.R

/**
 * 일정목록화면의 root view
 * @see AgendaFragment
 */
class AgendaFragmentContainerView : LinearLayout {

    private var mAnimationStart = true                      //Animation이 진행되는 동안 true로 설정된다.
    private var mListener: TransitionEndListener? = null    //Animation이 끝나는것을 감지하는 listener
    private var mToAgendaAnimation = false                  //일정목록화면이 보이는 animation인가, 사라지는 animation인가?
    private var mAnimationEndValue = 0f                     //Animation이 끝날때의 속성값

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)
    constructor(context: Context, attributes: AttributeSet?, defStyleAttr: Int) : super(context, attributes, defStyleAttr)

    /**
     * Animation 갱신함수
     * [R.animator.slide_from_right_a], [R.animator.slide_to_right_a]
     */
    fun setSlideX(value: Float) {
        //animation이 시작될때 mAnimationEndValue값을 계산한다.
        if(mAnimationStart) {
            mToAgendaAnimation = Utils.isToAgendaTransition()
            mAnimationEndValue = if(mToAgendaAnimation) 0f else 1f

            mAnimationStart = false
        }

        //view를 이동시킨다.
        val width = resources.displayMetrics.widthPixels
        translationX = width * value

        //value가 mAnimationEndValue과 같으면 animation이 끝났다고 listener에 알려준다.
        if(value == mAnimationEndValue) {
            if(mToAgendaAnimation) {
                mListener?.onTransitionEnd()
            }
            mAnimationStart = true
        }
    }

    fun setTransitionEndListener(listener:TransitionEndListener){
        mListener = listener
    }

    interface TransitionEndListener {
        fun onTransitionEnd();
    }
}