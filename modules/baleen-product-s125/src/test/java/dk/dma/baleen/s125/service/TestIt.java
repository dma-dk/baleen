/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.baleen.s125.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/**
 *
 */
public class TestIt {
    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("");
        if (resource != null) {
            File file = new File(resource.getFile());
            System.out.println("Files in datasets directory:");
            for (File f : file.listFiles()) {
                System.out.println(f.getName());
            }
        }

        new ClassPathResource("datasets/datasetpoint.xml").getInputStream();
        String content = new String(FileCopyUtils.copyToByteArray(new ClassPathResource("datasets/datasetpoint.xml").getInputStream()));
        System.out.println(content);
    }
}
