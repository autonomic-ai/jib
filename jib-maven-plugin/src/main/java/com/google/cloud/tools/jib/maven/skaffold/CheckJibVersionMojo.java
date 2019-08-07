/*-
 * ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
 * The Apache License, Version 2.0
 * ——————————————————————————————————————————————————————————————————————————————
 * Copyright (C) 2019 Autonomic, LLC - All rights reserved
 * ——————————————————————————————————————————————————————————————————————————————
 * Proprietary and confidential.
 * 
 * NOTICE:  All information contained herein is, and remains the property of
 * Autonomic, LLC and its suppliers, if any.  The intellectual and technical
 * concepts contained herein are proprietary to Autonomic, LLC and its suppliers
 * and may be covered by U.S. and Foreign Patents, patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Autonomic, LLC.
 * 
 * Unauthorized copy of this file, via any medium is strictly prohibited.
 * ______________________________________________________________________________
 */
/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.jib.maven.skaffold;

import com.google.cloud.tools.jib.maven.MojoCommon;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This internal Skaffold-related goal checks that the Jib plugin version is within some specified
 * range. It is only required so that older versions of Jib (prior to the introduction of the {@code
 * jib.requiredVersion} property) will error in such a way that it indicates the jib version is out
 * of date. This goal can be removed once there are no users of Jib prior to 1.4.0.
 *
 * <p>
 * Expected use: {@code mvn jib:_skaffold-fail-if-jib-out-of-date -Djib.requiredVersion='[1.4,2)'
 * jib:build -Dimage=xxx}
 */
@Mojo(
        name = CheckJibVersionMojo.GOAL_NAME,
        requiresProject = false,
        requiresDependencyCollection = ResolutionScope.NONE,
        defaultPhase = LifecyclePhase.INITIALIZE)
public class CheckJibVersionMojo extends SkaffoldBindingMojo {

    @VisibleForTesting
    static final String GOAL_NAME = "_skaffold-fail-if-jib-out-of-date";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Strings.isNullOrEmpty(System.getProperty(MojoCommon.REQUIRED_VERSION_PROPERTY_NAME))) {
            throw new MojoExecutionException(
                    GOAL_NAME + " requires " + MojoCommon.REQUIRED_VERSION_PROPERTY_NAME
                            + " to be set");
        }
        checkJibVersion();
    }
}
