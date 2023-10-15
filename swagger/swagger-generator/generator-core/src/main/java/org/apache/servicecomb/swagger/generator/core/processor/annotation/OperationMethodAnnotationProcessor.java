/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.swagger.generator.core.processor.annotation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.swagger.SwaggerUtils;
import org.apache.servicecomb.swagger.generator.MethodAnnotationProcessor;
import org.apache.servicecomb.swagger.generator.OperationGenerator;
import org.apache.servicecomb.swagger.generator.SwaggerGenerator;
import org.springframework.util.CollectionUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public class OperationMethodAnnotationProcessor implements MethodAnnotationProcessor<Operation> {
  public Type getProcessType() {
    return Operation.class;
  }

  @Override
  public void process(SwaggerGenerator swaggerGenerator,
      OperationGenerator operationGenerator, Operation apiOperationAnnotation) {
    io.swagger.v3.oas.models.Operation operation = operationGenerator.getOperation();

    if (!StringUtils.isEmpty(apiOperationAnnotation.method())) {
      operationGenerator.setHttpMethod(apiOperationAnnotation.method());
    }

    if (apiOperationAnnotation.responses() != null) {
      // This is a bit different from swagger annotation intention.
      // Here we assume @Content defines all supported media type and only one implementation class for one status code.
      List<String> produces = new ArrayList<>();
      for (ApiResponse apiResponse : apiOperationAnnotation.responses()) {
        if (StringUtils.isEmpty(apiResponse.responseCode()) || "default".equals(apiResponse.responseCode())) {
          throw new IllegalArgumentException("@ApiResponse status code must be defined.");
        }
        Class<?> type = null;
        for (Content content : apiResponse.content()) {
          if (StringUtils.isNotEmpty(content.mediaType())) {
            produces.add(content.mediaType());
          }
          if (content.schema() != null && content.schema().implementation() != Void.class) {
            type = content.schema().implementation();
          }
        }
        operationGenerator.getOperationGeneratorContext().updateResponse(apiResponse.responseCode(),
            type == null ? null : SwaggerUtils.resolveTypeSchemas(swaggerGenerator.getOpenAPI(), type));
        if (StringUtils.isNotEmpty(apiResponse.description())) {
          operationGenerator.getOperationGeneratorContext().updateResponseDescription(apiResponse.responseCode(),
              apiResponse.description());
        }
        for (Header header : apiResponse.headers()) {
          if (header.schema() == null || header.schema().implementation() == Void.class) {
            throw new IllegalArgumentException("@ApiResponse header schema implementation must be defined.");
          }
          if (StringUtils.isEmpty(header.name())) {
            throw new IllegalArgumentException("@ApiResponse header name must be defined.");
          }
          operationGenerator.getOperationGeneratorContext().updateResponseHeader(apiResponse.responseCode(),
              header.name(), AnnotationUtils.schemaModel(swaggerGenerator.getOpenAPI(), header.schema()));
        }
      }
      if (!CollectionUtils.isEmpty(produces)) {
        operationGenerator.getOperationGeneratorContext().updateProduces(produces);
      }
    }

    io.swagger.v3.oas.models.Operation specificOperation =
        AnnotationUtils.operationModel(swaggerGenerator.getOpenAPI(), apiOperationAnnotation);
    if (!StringUtils.isEmpty(specificOperation.getSummary())) {
      operation.setSummary(specificOperation.getSummary());
    }

    if (!StringUtils.isEmpty(specificOperation.getDescription())) {
      operation.setDescription(specificOperation.getDescription());
    }

    if (!StringUtils.isEmpty(specificOperation.getOperationId())) {
      operation.setOperationId(specificOperation.getOperationId());
    }

    if (!CollectionUtils.isEmpty(specificOperation.getExtensions())) {
      operation.setExtensions(specificOperation.getExtensions());
    }

    if (!CollectionUtils.isEmpty(specificOperation.getTags())) {
      operation.setTags(specificOperation.getTags());
    }
  }
}
