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
package dk.dma.baleen.util.quarkus;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

//@Provider
public class SimpleExceptionMapper implements ExceptionMapper<Exception> {

    /** {@inheritDoc} */
    @Override
    public Response toResponse(Exception exception) {
        return null;
    }
////    /**
////     * The Request Context.
////     */
////    @Inject
////    private ServletContext request;
//
//    @Override
//    public Response toResponse(Exception exception) {
//        System.out.println("OOPS");
//        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                .entity("Simple Exception Mapper")
//                .build();
//    }

//    /**
//     * The Request Context.
//     */
//    @Inject
//    private HttpHeaders request;
//
//    /**
//     * Generate the response based on the exceptions thrown by the respective
//     * SECOM endpoint called. This can be extracted by the request context.
//     *
//     * @param ex the exception that was thrownn
//     * @return the response to be returned
//     */
//    @Override
//    public Response toResponse(Exception ex) {
//        //First log the message
//        System.out.println(request);
//
//        // For everything else, just return an internal server error
//        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                .entity(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
//                .build();
//    }
}