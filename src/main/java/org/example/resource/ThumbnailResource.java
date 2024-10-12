/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.resource;

import is.galia.auth.AuthInfo;
import is.galia.auth.Authorizer;
import is.galia.auth.AuthorizerFactory;
import is.galia.config.Configuration;
import is.galia.http.Method;
import is.galia.http.Query;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.operation.Crop;
import is.galia.operation.CropToSquare;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPixels;
import is.galia.plugin.Plugin;
import is.galia.resource.AbstractImageResource;
import is.galia.resource.ImageRequestHandler;
import is.galia.resource.Resource;
import is.galia.resource.ResourceException;
import is.galia.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>This example consists of a simple image endpoint (resource) that provides
 * downscaled variant images (thumbnails). See the README for more
 * information.</p>
 *
 * <p>Note that for all plugins, including this one, a file must exist at
 * {@literal src/main/resources/META-INF/services/is.galia.plugin.Plugin}
 * containing the fully-qualified class name.</p>
 */
public class ThumbnailResource extends AbstractImageResource
        implements Plugin, Resource {

    private class ExampleImageRequestHandlerCallback
            implements ImageRequestHandler.Callback {
        @Override
        public boolean authorizeBeforeAccess() throws Exception {
            final Authorizer authorizer =
                    new AuthorizerFactory().newAuthorizer(getDelegate());
            final AuthInfo info = authorizer.authorizeBeforeAccess();
            if (info != null) {
                return handleAuthInfo(info);
            }
            return true;
        }

        @Override
        public boolean authorize() throws Exception {
            final Authorizer authorizer =
                    new AuthorizerFactory().newAuthorizer(getDelegate());
            final AuthInfo info = authorizer.authorize();
            if (info != null) {
                return handleAuthInfo(info);
            }
            return true;
        }

        @Override
        public void sourceAccessed(StatResult result) {}
        @Override
        public void infoAvailable(Info info) throws Exception {}
        @Override
        public void willStreamImageFromVariantCache() throws Exception {}
        @Override
        public void willProcessImage(Info info) throws Exception {}
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ThumbnailResource.class);

    private static final String FORMAT_CONFIG_KEY =
            "endpoint.thumbnailer.format";

    private Identifier identifier;

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of(FORMAT_CONFIG_KEY);
    }

    @Override
    public String getPluginName() {
        return ThumbnailResource.class.getSimpleName();
    }

    @Override
    public void onApplicationStart() {}

    @Override
    public void onApplicationStop() {}

    @Override
    public void initializePlugin() {}

    //endregion
    //region AbstractImageResource overrides

    @Override
    public Identifier getIdentifier() {
        if (identifier == null) {
            identifier = parseIdentifier();
            getRequestContext().setIdentifier(identifier);
        }
        return identifier;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    //endregion
    //region Resource methods

    @Override
    public Set<Route> getRoutes() {
        // This resource responds only via HTTP GET to the path /thumbs.
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^/thumbs$"))));
    }

    @Override
    public void doInit() throws Exception {
        super.doInit();
        // As the parent class' documentation of this method notes, subclasses
        // (like this one) should add the identifier to the request context if
        // they have one.
        getRequestContext().setIdentifier(getIdentifier());
    }

    @Override
    public void doGET() throws Exception {
        // We want all of the thumbnails we generate to be square.
        Crop crop = new CropToSquare();
        // And to be 256 pixels on a side.
        Scale scale = new ScaleByPixels(256, 256,
                ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        // And to be of the format specified in the configuration.
        Format format = getVariantFormat();
        Encode encode = new Encode(format);

        // Build an OperationList which fully defines our desired variant
        // image.
        OperationList opList = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(crop, scale, encode)
                .build();

        // Add a Content-Type header so that web browsers will know what to do
        // with the data we will be sending.
        getResponse().setHeader("Content-Type",
                format.getPreferredMediaType().toString());

        // Build an ImageRequestHandler to process the request. This class
        // abstracts away all the complexity of authorization, finding a
        // Source, finding an encoder & decoder, image processing, caching,
        // etc.
        ImageRequestHandler handler = ImageRequestHandler.builder()
                .withReference(getRequest().getReference())
                .withRequestContext(getRequestContext())
                .withDelegate(getDelegate())
                .withOperationList(opList)
                .withCallback(new ExampleImageRequestHandlerCallback())
                .build();

        // The handler will write the variant image data to the response
        // OutputStream. This stream should not be closed here.
        handler.handle(getResponse().openBodyStream());
    }

    /**
     * Reads the value of the `identifier` query argument, throwing a {@link
     * ResourceException} if invalid. The identifier tells us what image we'll
     * need to access from whatever {@link is.galia.source.Source source}
     * (underlying storage) we are using.
     */
    private Identifier parseIdentifier() {
        Query query = getRequest().getReference().getQuery();
        String identifierStr = query.getFirstValue("identifier", "");
        if (identifierStr.isBlank()) {
            throw new ResourceException(
                    Status.BAD_REQUEST, "Identifier not supplied");
        }
        return new Identifier(identifierStr);
    }

    /**
     * Reads the variant image format from the application configuration.
     */
    private Format getVariantFormat() {
        Configuration config = Configuration.forApplication();
        String formatKey     = config.getString(FORMAT_CONFIG_KEY, "jpg");
        // N.B.: The complete list of available format keys depends on what
        // plugins are installed. It's a safe bet that "jpg" will always be
        // available, but if we were to allow the client to specify a format,
        // we would have to check it against the list of formats supported by
        // is.galia.image.FormatRegistry.
        return Format.get(formatKey);
    }

}
