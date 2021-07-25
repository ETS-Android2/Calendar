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

import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;

/**
 * Created by Keval on 29-Mar-17.
 * This class plays the ringtone sample whenever user selects any ringtone from the list.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */
final class RingTonePlayer implements Closeable {

    @NonNull
    private final Context mContext;

    /**
     * Selected ringtone
     */
    Ringtone mSoundRingTone = null;

    /**
     * Public constructor.
     */
    RingTonePlayer(@NonNull final Context context) {
        mContext = context;
    }

    /**
     * Play the ringtone for the given uri.
     *
     * @param uri uri of the ringtone to play.
     */
    void playRingtone(@Nullable final Uri uri) throws
            IllegalArgumentException,
            SecurityException,
            IllegalStateException {
        if(mSoundRingTone != null)
            mSoundRingTone.stop();

        mSoundRingTone = RingtoneManager.getRingtone(mContext, uri);
        mSoundRingTone.play();
    }

    /**
     * Release the {@link MediaPlayer} instance. Remember to call this method in on destroy.
     */
    @Override
    public void close() {
        if(mSoundRingTone != null) {
            mSoundRingTone.stop();
            mSoundRingTone = null;
        }
    }
}
