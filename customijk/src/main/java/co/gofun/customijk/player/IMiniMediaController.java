/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.gofun.customijk.player;


import android.view.ViewGroup;

import co.gofun.customijk.view.MiniMediaController;
import co.gofun.customijk.view.MiniMediaTitleBar;

public interface IMiniMediaController {
    void hide();

    boolean isShowing();

    void setAnchorView(ViewGroup view);

    void setEnabled(boolean enabled);

    void setMediaPlayer(MiniMediaController.MediaPlayerControl player);

    void show(boolean reset, int timeout);

    void show(boolean reset);

    boolean isScreenLocked();

    MiniMediaTitleBar getMediaTitleBar();

    void resetPlayBtn(ViewGroup archor);

    void showPlayBtnView(final ViewGroup anchor);

    void notifyPlayStateChange(boolean isPlaying);



    //----------
    // Extends
    //----------
//    void showOnce(View view);
}
