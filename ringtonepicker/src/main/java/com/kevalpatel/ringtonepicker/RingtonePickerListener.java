/*
 * Copyright 2017 Keval Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance wit
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 *  the specific language governing permissions and limitations under the License.
 */

package com.kevalpatel.ringtonepicker;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Ringtone음이 선택되였다는것을 알려주는 Listener 이다.
 * @see RingtonePickerDialog
 * @see #OnRingtoneSelected
 */
public interface RingtonePickerListener extends Serializable {

    /**
     * Ringtone 음 선택대화창에서 Ringtone음을 선택하였을때 호출된다.
     * @param ringtoneName Ringtone 음의 제목
     * @param ringtoneUri  Ringtone 음의 {@link Uri}. 무음을 선택하였을때에 이것이 null 이 될수 있다.
     */
    void OnRingtoneSelected(@NonNull String ringtoneName, @Nullable Uri ringtoneUri);
}
