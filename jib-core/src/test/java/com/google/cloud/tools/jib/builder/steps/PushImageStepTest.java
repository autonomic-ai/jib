package com.google.cloud.tools.jib.builder.steps;

import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.builder.ProgressEventDispatcher;
import com.google.cloud.tools.jib.configuration.BuildConfiguration;
import com.google.cloud.tools.jib.image.json.BuildableManifestTemplate;
import com.google.cloud.tools.jib.registry.RegistryClient;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests for {@link PushBlobStep}. */
@RunWith(MockitoJUnitRunner.class)
public class PushImageStepTest {

  @Mock private RegistryClient registryClient;

  @Mock(answer = Answers.RETURNS_MOCKS)
  private ProgressEventDispatcher.Factory progressDispatcherFactory;

  @Mock(answer = Answers.RETURNS_MOCKS)
  private BuildConfiguration buildConfiguration;

  private RetryConfig retryConfig;

  @Before
  public void setUp() {
    RegistryClient.Factory registryClientFactory =
        Mockito.mock(RegistryClient.Factory.class, Answers.RETURNS_SELF);
    Mockito.when(registryClientFactory.newRegistryClient()).thenReturn(registryClient);

    Mockito.when(buildConfiguration.newTargetImageRegistryClientFactory())
        .thenReturn(registryClientFactory);

    retryConfig = new RetryConfig(2, 1, 2, 1.1);
  }

  @Test
  public void testCall_pushesManifest() throws IOException, RegistryException {
    BuildableManifestTemplate manifestTemplate = Mockito.mock(BuildableManifestTemplate.class);
    String tag = "someTag";

    call(manifestTemplate, tag);

    Mockito.verify(registryClient).pushManifest(manifestTemplate, tag);
  }

  @Test(expected = RegistryException.class)
  public void testCall_retriesPushManifest_ifRegistryExceptionOccurs()
      throws IOException, RegistryException {
    Mockito.doThrow(new RegistryException("test registry exception"))
        .when(registryClient)
        .pushManifest(Mockito.any(), Mockito.anyString());

    BuildableManifestTemplate manifestTemplate = Mockito.mock(BuildableManifestTemplate.class);
    String tag = "someTag";

    call(manifestTemplate, tag);

    Mockito.verify(registryClient, Mockito.times(3)).pushManifest(manifestTemplate, tag);
  }

  private void call(BuildableManifestTemplate manifestTemplate, String tag)
      throws IOException, RegistryException {
    new PushImageStep(
            buildConfiguration,
            progressDispatcherFactory,
            null,
            manifestTemplate,
            tag,
            null,
            null,
            retryConfig)
        .call();
  }
}
