/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorPrivacyManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;
import android.view.Surface;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SettingObserver;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.util.settings.SystemSettings;

import javax.inject.Inject;

/** Quick settings tile: Rotation Toggle **/
public class RotationToggleTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "rotation_toggle";

    private static final String EMPTY_SECONDARY_STRING = "";

    private final Icon mIcon = ResourceIcon.get(com.android.internal.R.drawable.ic_qs_auto_rotate);
    // private final RotationToggleController mController;
    // private final SensorPrivacyManager mPrivacyManager;
    // private final BatteryController mBatteryController;
    private final SettingObserver mSetting;

    private int mCurrentUser;

    @Inject
    public RotationToggleTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,            
            SystemSettings systemSettings
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mCurrentUser = host.getUserContext().getUserId();
        mSetting = new SettingObserver(
                systemSettings,
                mHandler,
                Settings.System.USER_ROTATION,
                mCurrentUser
        ) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                // mHandler is the background handler so calling this is OK
                handleRefreshState(null);
            }
        };        
    }

    @Override
    protected void handleInitialize() {
        
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS);
    }

    @Override
    protected void handleClick(@Nullable View view) {
        final boolean newState = !mState.value;
        
        mContext.sendBroadcastAsUser(new Intent("android.intent.action.pc.SCREEN_ORIENTATION_TOGGLE"), UserHandle.of(mCurrentUser));
        refreshState(newState);
    }

    @Override
    public CharSequence getTileLabel() {
        return getState().label;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        int userRotation = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.USER_ROTATION, Surface.ROTATION_270,
                    UserHandle.USER_CURRENT);

        state.label = mContext.getString(R.string.status_bar_settings_rotation_toggle);
        state.value = userRotation != Surface.ROTATION_0 && userRotation != Surface.ROTATION_180;
        if (!state.value) {
            state.icon = ResourceIcon.get(R.drawable.qs_auto_rotate_icon_on);            
        } else {            
            state.icon = ResourceIcon.get(R.drawable.qs_auto_rotate_icon_off);
            
        }
        state.stateDescription = state.secondaryLabel;

        state.expandedAccessibilityClassName = Switch.class.getName();
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mSetting.setListening(false);
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mCurrentUser = newUserId;
        mSetting.setUserId(newUserId);
        handleRefreshState(null);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_ROTATIONTOGGLE;
    }    
}
