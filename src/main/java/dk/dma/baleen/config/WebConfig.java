/*
 * Copyright (c) 2024 Danish Maritime Authority.
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

package dk.dma.baleen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root to index.html
        registry.addViewController("/").setViewName("forward:/browser/index.html");
        // Forward specific Angular routes to index.html
        registry.addViewController("/home").setViewName("forward:/browser/index.html");
        registry.addViewController("/subscribers").setViewName("forward:/browser/index.html");
        registry.addViewController("/logging").setViewName("forward:/browser/index.html");
        registry.addViewController("/niord").setViewName("forward:/browser/index.html");
        registry.addViewController("/about").setViewName("forward:/browser/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve Angular app from /browser with fallback to index.html for client-side routing
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/browser/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        // If the resource exists and is not an API call, serve it
                        if (requestedResource.exists() && requestedResource.isReadable() && !resourcePath.startsWith("api/")) {
                            return requestedResource;
                        }
                        // Otherwise, serve index.html for Angular routing (unless it's an API call)
                        if (!resourcePath.startsWith("api/")) {
                            return new ClassPathResource("/static/browser/index.html");
                        }
                        return null;
                    }
                });
    }
}