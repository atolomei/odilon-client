/*
 * Odilon Object Storage
 * (c) kbee 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.odilon.client;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.odilon.log.Logger;
import io.odilon.model.SharedConstant;

/**
 * <p>
 * JVM properties
 * </p>
 * 
 * @author atolomei@novamens.com (Alejandro Tolomei)
 * 
 */
public enum OdilonClientProperties {

    INSTANCE;

    private static final Logger logger = Logger.getLogger(OdilonClientProperties.class.getName());

    private final AtomicReference<String> version = new AtomicReference<>(null);

    public String getVersion() {
        String result = version.get();
        if (result == null) {
            synchronized (INSTANCE) {
                if (version.get() == null) {
                    try {
                        ClassLoader classLoader = getClass().getClassLoader();
                        setOdilonClientJavaVersion(classLoader);
                        setDevelopmentVersion();
                    } catch (IOException e) {
                        logger.error(e, SharedConstant.NOT_THROWN);
                        version.set("unknown");
                    }
                    result = version.get();
                }
            }
        }
        return result;
    }

    private void setDevelopmentVersion() {
        if (version.get() == null) {
            version.set("dev");
        }
    }

    private void setOdilonClientJavaVersion(ClassLoader classLoader) throws IOException {
        if (classLoader != null) {
            final String versionString = "Odilon-Client-Java-Version";
            Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                for (Object k : manifest.getMainAttributes().keySet()) {
                    if (k.toString().equals(versionString)) {
                        version.set(manifest.getMainAttributes().getValue((Attributes.Name) k));
                    }
                }
            }
        }
    }
}
