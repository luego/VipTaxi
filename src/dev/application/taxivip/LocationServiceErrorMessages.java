/*
 * Copyright (C) 2013 The Android Open Source Project
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

package dev.application.taxivip;

import android.content.Context;

/**
 * Map error codes to error messages.
 */
public class LocationServiceErrorMessages {

    // Don't allow instantiation
    private LocationServiceErrorMessages() {}

    public static String getErrorString(Context context, int errorCode) {

        // Get a handle to resources, to allow the method to retrieve messages.
       // Resources mResources = context.getResources();

        // Define a string to contain the error message
        String errorString = null;

        
        // Return the error message
        return errorString;
    }
}
