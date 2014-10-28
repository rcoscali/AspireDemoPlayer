/*
 * Copyright (C) 2014 NagraVision
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
package com.nagravision.aspiredemoplayer.drm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.nagravision.aspiredemoplayer.Content;
import com.nagravision.aspiredemoplayer.MainActivity;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.drm.DrmRights;
import android.util.Log;

public final class DrmAgent
{
    private final String        TAG                    = "DrmAgent";
    private DrmManagerClient    mDrmMgrClt             = null;
    private String[]            mDrmEngines            = null;
    private static final String PLUG_IN_NAME           = "NagraVision DRM plug-in";
    private boolean             mIsNagraPlugInAvalable = false;
    private static DrmAgent     gInstance              = null;
    private static String       FILE_BASE_PATH         = null;
    private String              mContentRightsFile     = "";

    private String              mUserName              = "";
    private String              mPassword              = "";
    private String              mAndroidId             = "";
    private Map<String, String> mKeysMap               = new HashMap<String, String>();

    /**
     * @brief
     *        Get the content base path
     * 
     * @return FILE_BASE_PATH
     */
    public static String gGetBasePath()
    {
        return FILE_BASE_PATH;
    }

    /**
     * @brief
     *        Set the content base path
     * 
     * @param xBasePath
     */
    public static void gSetBasePath(String xBasePath)
    {
        FILE_BASE_PATH = xBasePath;
    }

    /**
     * @brief
     *        Static method to create the drm agent
     * @param xContext
     *            The application context
     * @return The DrmAgent object
     */
    public static void gCreateInstance(Context xContext)
    {
        if (gInstance == null)
        {
            gInstance = new DrmAgent(xContext);
        }
    }

    /**
     * @brief
     *        Static method to get the drm agent
     * @return The DrmAgent object
     */
    public static DrmAgent getInstance()
    {
        return gInstance;
    }

    /**
     * @brief
     *        Constructor
     * 
     * @param xContext
     *            Application context
     * @throws NoSuchMethodException
     */
    private DrmAgent(Context xContext)
    {

        this.mKeysMap.put("121a0fca0f1b475b8910297fa8e0a07e",
            "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf");

        this.mKeysMap.put("809a82baec0e4254bf43573eed9eac02.txt",
            "7f99069578ab4dae8d6ce24cc3210232");

        if (FILE_BASE_PATH == null)
            gSetBasePath(xContext.getExternalFilesDir(null).getAbsolutePath());

        mContentRightsFile = gGetBasePath() + "/contentrights.rights";

        mDrmMgrClt = new DrmManagerClient(xContext);
        mDrmMgrClt.setOnErrorListener(new DrmManagerClient.OnErrorListener()
            {

                @Override
                public void onError(DrmManagerClient client, DrmErrorEvent event)
                {
                    String logstr = "DRM Error: " + event.getMessage();
                    Log.v(TAG, logstr);
                }
            });

        mDrmMgrClt.setOnEventListener(new DrmManagerClient.OnEventListener()
            {
                @Override
                public void onEvent(DrmManagerClient client, DrmEvent event)
                {
                    String logstr = "DRM Event: " + event.getMessage();
                    Log.v(TAG, logstr);
                }
            });

        mDrmMgrClt.setOnInfoListener(new DrmManagerClient.OnInfoListener()
            {
                @Override
                public void onInfo(DrmManagerClient client, DrmInfoEvent event)
                {
                    Log.v(TAG, "DRM Info: " + event.getMessage());
                }
            });

        mDrmEngines = mDrmMgrClt.getAvailableDrmEngines();
        checkPlugInIsAvailable(DrmAgent.PLUG_IN_NAME);
    }

    /**
     * @brief
     *        Register the device
     * @return true if the device was registered false if not
     */
    public boolean registerDevice()
    {
        boolean retVal = false;
        for (;;)
        {
            DrmInfoRequest drmInfoRequest = new DrmInfoRequest(
                DrmInfoRequest.TYPE_REGISTRATION_INFO, "video/mp4");

            DrmInfo drmInfo = mDrmMgrClt.acquireDrmInfo(drmInfoRequest);
            if (null == drmInfo)
            {
                Log.v(TAG, "registerDevice: null DrmInfo returned");
                break;
            }

            String registered = (String) drmInfo.get("PERSO");
            if (registered != null && registered.equals("YES"))
            {
                retVal = true;
                Log.v(TAG, "registerDevice: Device already registered");
                break;
            }

            drmInfo.put("PERSO", generateUniqueId());
            if (DrmManagerClient.ERROR_NONE != mDrmMgrClt
                .processDrmInfo(drmInfo))
            {
                Log.v(TAG, "checkDrmInfoRights: processDrmInfo failed");
                break;
            }

            Log.v(TAG, "registerDevice: Device registered");

            retVal = true;
            break;
        }

        return retVal;
    }

    /**
     * @brief
     *        Acquire the rights of the content
     * @param xVideoLocation
     * 
     * @param xContentRightsFile
     * 
     * @param xMimeType
     * 
     * @return true
     *         playable rights else false
     */
    public boolean acquireDrmInfoRights(String xContentId, String xMimeType)
    {

        boolean retVal = false;

        try
        {
            for (;;)
            {
                DrmInfoRequest drmInfoRequest = new DrmInfoRequest(
                    DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO, xMimeType);

                DrmInfo drmInfo = mDrmMgrClt.acquireDrmInfo(drmInfoRequest);
                if (null == drmInfo)
                {
                    Log.v(TAG, "checkDrmInfoRights: null DrmInfo returned");
                    break;
                }

                if (DrmManagerClient.ERROR_NONE != mDrmMgrClt
                    .processDrmInfo(drmInfo))
                {
                    Log.v(TAG, "checkDrmInfoRights: processDrmInfo failed");
                    break;
                }

                /*
                 * ContentValues values =
                 * mDrmMgrClt.getConstraints(xVideoLocation,
                 * DrmStore.Action.PLAY);
                 * if(null == values){
                 * Log.v(TAG, "checkDrmInfoRights: null ContentValues returned"
                 * );
                 * break;
                 * }
                 */

                DrmRights contentRights = new DrmRights(mContentRightsFile,
                    xMimeType);

                retVal = (0 == mDrmMgrClt.saveRights(contentRights,
                    mContentRightsFile, xContentId));
                retVal = true;
                break;
            }
        }
        catch (IOException err)
        {
            Log.v(TAG, "checkDrmInfoRights: " + err.getMessage());
        }

        return retVal;
    }

    public int checkDrmInfoRights(String xContentId)
    {
        return mDrmMgrClt.checkRightsStatus(xContentId);
    }

    /**
     * @brief
     *        Get the list of available drm plugins
     * 
     * @return An Array of strings of the
     *         drm plugin names is available else an empty array
     * 
     */
    public String[] getDrmEngines()
    {
        return mDrmEngines;
    }

    /**
     * @brief
     *        check if the nagra plugin is available
     * @return true if the nagra plug in is available else false
     */
    public boolean isNagraPlugInAvalable()
    {
        return mIsNagraPlugInAvalable;
    }

    /**
     * @brief
     *        check the available plugin list for a plugin
     * @param xPlugInName
     *            The plugin name to search for
     * @return true if the name is in the list else false
     */
    private void checkPlugInIsAvailable(String xPlugInName)
    {
        mIsNagraPlugInAvalable = false;
        for (int i = 0; i < mDrmEngines.length; i++)
        {
            Log.v(TAG, "Available DRM PLUGINS " + mDrmEngines[i]);
            if (mDrmEngines[i].equals(xPlugInName))
            {
                mIsNagraPlugInAvalable = true;
            }
        }
    }

    /**
     * @brief
     *        Get the user name
     * 
     * @return The user name
     */
    public String getUserName()
    {
        return mUserName;
    }

    /**
     * @brief
     *        set the user name
     * 
     * @param xUserName
     */
    public void setUserName(String xUserName)
    {
        this.mUserName = xUserName;
    }

    /**
     * @brief
     *        Get the password
     * 
     * @return The password
     */
    public String getPassword()
    {
        return mPassword;
    }

    /**
     * @brief
     *        Set the password
     * 
     * @param xPassword
     */
    public void setPassword(String xPassword)
    {
        this.mPassword = xPassword;
    }

    /**
     * @brief
     *        Check if the log in credentials are valid
     * 
     * @return true
     *         They are valid else false
     */
    public boolean loginSuccedded()
    {
        boolean retVal = false;
        for (;;)
        {
            if (mUserName.equals(""))
            {
                Log.v(TAG, "Missing user name");
                break;
            }

            if (mPassword.equals(""))
            {
                Log.v(TAG, "Missing password");
                break;
            }

            retVal = true;
            break;
        }
        return retVal;
    }

    public void setAndroidId(String xId)
    {
        this.mAndroidId = xId;
    }

    private String generateUniqueId()
    {
        return this.mUserName + ":" + this.mPassword + ":" + this.mAndroidId;
    }

    public void setDatabasePath(String xPath)
    {
        gSetBasePath(xPath);
    }

    public boolean isRegistered()
    {
        boolean retVal = false;
        for (;;)
        {
            DrmInfoRequest drmInfoRequest = new DrmInfoRequest(
                DrmInfoRequest.TYPE_REGISTRATION_INFO, "video/mp4");

            DrmInfo drmInfo = mDrmMgrClt.acquireDrmInfo(drmInfoRequest);
            if (null == drmInfo)
            {
                Log.v(TAG, "isRegistered: null DrmInfo returned");
                break;
            }

            String registered = (String) drmInfo.get("PERSO");
            if (registered == null || !registered.equals("yes"))
            {
                Log.v(TAG, "isRegistered: Device is not registered");
                break;
            }

            extractLoginCredentials((String) drmInfo.get("UNIQUE_ID"));

            Log.v(TAG, "isRegistered: Device is already registered");
            retVal = true;
            break;
        }
        return retVal;
    }

    private void extractLoginCredentials(String xUniqueId)
    {
        String[] tokens = xUniqueId.split(":");
        this.mUserName = tokens[0];
        this.mPassword = tokens[1];
    }

    public byte[] provideKey(String xContentId)
    {
        String key = this.mKeysMap.get(xContentId.replace("\n", ""));
        return hexStringToByteArray(key);
    }

    public byte[] provideIV()
    {
        return hexStringToByteArray("0123456789abcdef0000000000000000");
    }

    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                .digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
