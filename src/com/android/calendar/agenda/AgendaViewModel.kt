package com.android.calendar.agenda

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.CalendarContract
import androidx.lifecycle.*
import com.android.calendar.event.EventManager
import com.android.calendar.event.EventManager.OneEvent
import com.android.kr_common.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.Serializable

/**
 * 일정목록화면에서 리용하는 ViewModel
 * @see AgendaFragment
 */
class AgendaViewModel(application: Application, private val handle: SavedStateHandle) : AndroidViewModel(application) {
    companion object {
        const val CONSTANT_SAVE_KEY = "agenda_input_data_key"
    }

    //입력마당들을 포함한 model자료
    private var _inputData = MutableLiveData<AgendaInputModel>()
    //UI에 반영할 일정목록자료
    private val _eventList = MediatorLiveData<List<OneEvent>>()

    val eventList: LiveData<List<OneEvent>> get() = _eventList

    //`일정변화`가 일어났을때 일정을 다시 적재하기 위한 observer
    private var contentObserver: ContentObserver? = null

    init {
        _inputData = handle.getLiveData(CONSTANT_SAVE_KEY, null)

        _eventList.addSource(_inputData) {
            loadEvents()
        }
    }

    /**
     * CoroutineScope 을 통해 일정들을 불러들인다.
     */
    private fun loadEvents() {
        viewModelScope.launch {
            val eventList = queryEvents()
            _eventList.postValue(eventList)

            /**
             * 달력일정 db가 변하면 일정들을 다시 불러들인다.
             */
            if(contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(CalendarContract.Events.CONTENT_URI) {
                    loadEvents()
                }
            }
        }
    }

    /**
     * @return query를 통해 일정들을 불러들여서 돌려준다.
     */
    private suspend fun queryEvents() : List<OneEvent> {
        var events = mutableListOf<OneEvent>()

        _inputData = handle.getLiveData(CONSTANT_SAVE_KEY, null)
        val simpleModel = _inputData.value ?: return events

        //시작날자, 마감날자, 검색어를 얻는다.
        val startDay = Time.getJulianDay(simpleModel.startDate.year, simpleModel.startDate.monthOfYear, simpleModel.startDate.dayOfMonth)
        val endDay = Time.getJulianDay(simpleModel.endDate.year, simpleModel.endDate.monthOfYear, simpleModel.endDate.dayOfMonth)
        val query = simpleModel.query

        //일정들을 얻는다.
        withContext(Dispatchers.IO) {
             events = EventManager.getEventsInRange(getApplication<Application>().applicationContext, startDay, endDay, query)
        }

        //얻은 일정목록을 돌려준다.
        return events
    }

    /**
     * 시작날자 설정
     * @param date 시작날자
     */
    fun setStartDate(date: DateTime) {
        val simpleModel = _inputData.value

        if(simpleModel != null) {
            val originalStart = simpleModel.startDate
            if (originalStart.millis == date.millis)
                return
        }

        handle.set(CONSTANT_SAVE_KEY, AgendaInputModel(date, simpleModel!!.endDate, simpleModel.query))
    }

    /**
     * 마감날자 설정
     * @param date 마감날자
     */
    fun setEndDate(date: DateTime) {
        val simpleModel = _inputData.value

        if(simpleModel != null) {
            val originalEnd = simpleModel.endDate
            if (originalEnd.millis == date.millis)
                return
        }

        handle.set(CONSTANT_SAVE_KEY, AgendaInputModel(simpleModel!!.startDate, date, simpleModel.query))
    }

    /**
     * 시작날자, 마감날자 설정
     * @param startDate 시작날자
     * @param endDate 마감날자
     */
    fun setInputData(startDate: DateTime, endDate: DateTime, query: String) {
        val simpleModel = _inputData.value

        if(simpleModel != null) {
            val originalStart = simpleModel.startDate
            val originalEnd = simpleModel.endDate
            val originalQuery = simpleModel.query

            if (originalStart.millis == startDate.millis && originalEnd.millis == endDate.millis
                    && originalQuery == query)
                return
        }

        handle.set(CONSTANT_SAVE_KEY, AgendaInputModel(startDate, endDate, query))
    }

    /**
     * 검색어 설정
     * @param query 검색어
     */
    fun setQuery(query: String) {
        val simpleModel = _inputData.value
        val originalQuery = simpleModel!!.query

        if(query == originalQuery)
            return

        handle.set(CONSTANT_SAVE_KEY, AgendaInputModel(simpleModel.startDate, simpleModel.endDate, query))
    }

    /**
     * ViewModel이 파괴될때
     */
    override fun onCleared() {
        //등록하였던 ContentObserver를 없앤다.
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

    /**
     * 시작날자, 마감날자, 검색어를 하나로 묶어 관리하는 model클라스
     */
    class AgendaInputModel(val startDate: DateTime, val endDate: DateTime, val query: String) : Serializable
}

/**
 * [ContentObserver]를 등록해준다.
 * @return 등록된 [ContentObserver]를 돌려준다.
 */
private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}