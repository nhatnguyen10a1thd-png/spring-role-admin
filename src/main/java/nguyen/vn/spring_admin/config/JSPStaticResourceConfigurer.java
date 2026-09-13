package nguyen.vn.spring_admin.config;

import org.apache.catalina.*;
import org.springframework.util.ResourceUtils;

import java.net.URI;
import java.net.URL;

public class JSPStaticResourceConfigurer implements LifecycleListener {

    private final Context context;

    public JSPStaticResourceConfigurer(Context context) {
        this.context = context;
    }

    private final String subPath = "/META-INF";

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (!event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
            return;
        }

        final URL finalLocation = getUrl();
        if (ResourceUtils.isFileURL(finalLocation)) {
            try {
                java.io.File dir = new java.io.File(finalLocation.toURI());
                java.io.File subDir = new java.io.File(dir, subPath.startsWith("/") ? subPath.substring(1) : subPath);
                if (!subDir.exists()) {
                    return;
                }
            } catch (Exception ignored) {
                return;
            }
        }

        this.context.getResources().createWebResourceSet(
                WebResourceRoot.ResourceSetType.RESOURCE_JAR,
                "/",
                finalLocation,
                subPath
        );
    }

    private URL getUrl() {

        final URL location =
                this.getClass()
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation();

        if (ResourceUtils.isFileURL(location)) {

            return location;

        } else if (ResourceUtils.isJarURL(location)) {

            try {

                String locationStr = location.getPath()
                        .replaceFirst("^nested:", "")
                        .replaceFirst("/!BOOT-INF/classes/!/$", "!/");

                return new URI(
                        "jar:file",
                        locationStr,
                        null
                ).toURL();

            } catch (Exception e) {

                throw new IllegalStateException(
                        "Unable to add new JSP source URI to tomcat resources",
                        e
                );
            }

        } else {

            throw new IllegalStateException(
                    "Can not add tomcat resources, unhandleable url: "
                            + location
            );
        }
    }
}