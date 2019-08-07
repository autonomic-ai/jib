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
 * Copyright 2018 Google LLC.
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

package com.google.cloud.tools.jib.maven;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.plugins.common.HelpfulSuggestions;
import javax.annotation.Nullable;

/** Builder for Maven-specific {@link HelpfulSuggestions}. */
class MavenHelpfulSuggestionsBuilder {

    private final String messagePrefix;
    private final JibPluginConfiguration jibPluginConfiguration;

    @Nullable
    private ImageReference baseImageReference;
    @Nullable
    private ImageReference targetImageReference;
    private boolean baseImageHasConfiguredCredentials;
    private boolean targetImageHasConfiguredCredentials;

    MavenHelpfulSuggestionsBuilder(
            String messagePrefix, JibPluginConfiguration jibPluginConfiguration) {
        this.messagePrefix = messagePrefix;
        this.jibPluginConfiguration = jibPluginConfiguration;
    }

    MavenHelpfulSuggestionsBuilder setBaseImageReference(ImageReference baseImageReference) {
        this.baseImageReference = baseImageReference;
        return this;
    }

    MavenHelpfulSuggestionsBuilder setBaseImageHasConfiguredCredentials(
            boolean areKnownCredentialsDefined) {
        baseImageHasConfiguredCredentials = areKnownCredentialsDefined;
        return this;
    }

    MavenHelpfulSuggestionsBuilder setTargetImageReference(ImageReference targetImageReference) {
        this.targetImageReference = targetImageReference;
        return this;
    }

    MavenHelpfulSuggestionsBuilder setTargetImageHasConfiguredCredentials(
            boolean areKnownCredentialsDefined) {
        targetImageHasConfiguredCredentials = areKnownCredentialsDefined;
        return this;
    }

    /**
     * Builds the {@link HelpfulSuggestions}.
     *
     * @return the {@link HelpfulSuggestions}
     */
    HelpfulSuggestions build() {
        boolean isCredHelperDefinedForBaseImage =
                jibPluginConfiguration.getTargetImageCredentialHelperName() != null;
        boolean isCredHelperDefinedForTargetImage =
                jibPluginConfiguration.getTargetImageCredentialHelperName() != null;
        return new HelpfulSuggestions(
                messagePrefix,
                "mvn clean",
                baseImageReference,
                !isCredHelperDefinedForBaseImage && !baseImageHasConfiguredCredentials,
                targetImageReference,
                !isCredHelperDefinedForTargetImage && !targetImageHasConfiguredCredentials,
                "<to><image>",
                "-Dimage",
                "pom.xml");
    }
}
