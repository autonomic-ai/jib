/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.jib.builder.steps;

import com.google.cloud.tools.jib.api.DescriptorDigest;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.blob.BlobDescriptor;
import com.google.cloud.tools.jib.builder.ProgressEventDispatcher;
import com.google.cloud.tools.jib.builder.TimerEventDispatcher;
import com.google.cloud.tools.jib.configuration.BuildConfiguration;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.hash.Digests;
import com.google.cloud.tools.jib.http.Authorization;
import com.google.cloud.tools.jib.image.Image;
import com.google.cloud.tools.jib.image.json.BuildableManifestTemplate;
import com.google.cloud.tools.jib.image.json.ImageToJsonTranslator;
import com.google.cloud.tools.jib.registry.RegistryClient;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;

/**
 * Pushes a manifest for a tag. Returns the manifest digest ("image digest") and the container
 * configuration digest ("image id") as {#link BuildResult}.
 */
class PushImageStep implements Callable<BuildResult> {

  private static final String DESCRIPTION = "Pushing manifest";

  static ImmutableList<PushImageStep> makeList(
      BuildConfiguration buildConfiguration,
      ProgressEventDispatcher.Factory progressEventDispatcherFactory,
      Authorization pushAuthorization,
      BlobDescriptor containerConfigurationDigestAndSize,
      Image builtImage)
      throws IOException {
    Set<String> tags = buildConfiguration.getAllTargetImageTags();

    try (TimerEventDispatcher ignored =
            new TimerEventDispatcher(
                buildConfiguration.getEventHandlers(), "Preparing manifest pushers");
        ProgressEventDispatcher progressEventDispatcher =
            progressEventDispatcherFactory.create("preparing manifest pushers", tags.size())) {

      // Gets the image manifest to push.
      BuildableManifestTemplate manifestTemplate =
          new ImageToJsonTranslator(builtImage)
              .getManifestTemplate(
                  buildConfiguration.getTargetFormat(), containerConfigurationDigestAndSize);

      DescriptorDigest manifestDigest = Digests.computeJsonDigest(manifestTemplate);

      return tags.stream()
          .map(
              tag ->
                  new PushImageStep(
                      buildConfiguration,
                      progressEventDispatcher.newChildProducer(),
                      pushAuthorization,
                      manifestTemplate,
                      tag,
                      manifestDigest,
                      containerConfigurationDigestAndSize.getDigest(),
                      new RetryConfig(5, 1, 60, 2.0)))
          .collect(ImmutableList.toImmutableList());
    }
  }

  private final BuildConfiguration buildConfiguration;
  private final ProgressEventDispatcher.Factory progressEventDispatcherFactory;

  private final BuildableManifestTemplate manifestTemplate;
  @Nullable private final Authorization pushAuthorization;
  private final String tag;
  private final DescriptorDigest imageDigest;
  private final DescriptorDigest imageId;
  private final RetryConfig retryConfig;

  PushImageStep(
      BuildConfiguration buildConfiguration,
      ProgressEventDispatcher.Factory progressEventDispatcherFactory,
      @Nullable Authorization pushAuthorization,
      BuildableManifestTemplate manifestTemplate,
      String tag,
      DescriptorDigest imageDigest,
      DescriptorDigest imageId,
      RetryConfig retryConfig) {
    this.buildConfiguration = buildConfiguration;
    this.progressEventDispatcherFactory = progressEventDispatcherFactory;
    this.pushAuthorization = pushAuthorization;
    this.manifestTemplate = manifestTemplate;
    this.tag = tag;
    this.imageDigest = imageDigest;
    this.imageId = imageId;
    this.retryConfig = retryConfig;
  }

  @Override
  public BuildResult call() throws IOException, RegistryException {
    EventHandlers eventHandlers = buildConfiguration.getEventHandlers();
    try (TimerEventDispatcher ignored = new TimerEventDispatcher(eventHandlers, DESCRIPTION);
        ProgressEventDispatcher ignored2 =
            progressEventDispatcherFactory.create("pushing manifest for " + tag, 1)) {
      eventHandlers.dispatch(LogEvent.info("Pushing manifest for " + tag + "..."));

      pushManifestWithRetries();

      return new BuildResult(imageDigest, imageId);
    }
  }

  private void pushManifestWithRetries() throws IOException, RegistryException {
    RegistryClient registryClient =
        buildConfiguration
            .newTargetImageRegistryClientFactory()
            .setAuthorization(pushAuthorization)
            .newRegistryClient();

    RetryPolicy retryPolicy =
        new RetryPolicy()
            .withMaxRetries(retryConfig.getMaxRetries())
            .withBackoff(
                retryConfig.getBackoffDelay(),
                retryConfig.getMaxDelay(),
                TimeUnit.SECONDS,
                retryConfig.getDelayFactor())
            .retryOn(RegistryException.class);

    try {
      Failsafe.with(retryPolicy)
          .onRetry(
              (result, failure, context) -> LogEvent.warn("Retrying manifest push: " + failure))
          .onFailure(
              (result, failure, context) -> LogEvent.error("Unable to push manifest: " + failure))
          .run(() -> registryClient.pushManifest(manifestTemplate, tag));
    } catch (FailsafeException e) {
      if (e.getCause() instanceof RegistryException) {
        throw (RegistryException) e.getCause();
      }
    }
  }
}
