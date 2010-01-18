/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.rest;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a HTTP Multipart Request.
 *
 * @author Christian Pesch
 */

abstract class MultipartRequest extends HttpRequest {
    private List<Part> parts = new ArrayList<Part>();

    MultipartRequest(HttpMethod method) {
        super(method);
    }

    public void addParameter(String name, String value) {
        parts.add(new StringPart(name, value));
    }

    public void addParameter(String name, File file) throws IOException {
        parts.add(new FilePart(name, Helper.encodeUri(file.getName()), file, "application/octet-stream", "UTF-8"));
    }

    protected void doExecute() throws IOException {
        ((EntityEnclosingMethod) method).setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), method.getParams()));
        ((EntityEnclosingMethod) method).addRequestHeader("Connection", "close");
        ((EntityEnclosingMethod) method).addRequestHeader("Expect", "");
        super.doExecute();
    }
}
