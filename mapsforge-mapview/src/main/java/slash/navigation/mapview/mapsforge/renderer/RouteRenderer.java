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
package slash.navigation.mapview.mapsforge.renderer;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.mapview.mapsforge.MapViewCallbackOpenSource;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.lines.Line;
import slash.navigation.mapview.mapsforge.lines.Polyline;
import slash.navigation.mapview.mapsforge.models.IntermediateRoute;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import static java.lang.Thread.sleep;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.mapview.MapViewConstants.ROUTE_LINE_WIDTH_PREFERENCE;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asRGBA;
import static slash.navigation.routing.RoutingResult.Validity.*;

/**
 * Renders a {@link List} of {@link PairWithLayer} for the {@link MapsforgeMapView}.
 *
 * @author Christian Pesch
 */

public class RouteRenderer {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private Paint ROUTE_NOT_VALID_PAINT, ROUTE_DOWNLOADING_PAINT;

    private final Object notificationMutex = new Object();
    private boolean drawingRoute, drawingBeeline;

    private MapsforgeMapView mapView;
    private MapViewCallbackOpenSource mapViewCallback;
    private ColorModel routeColorModel;
    private GraphicFactory graphicFactory;

    public RouteRenderer(MapsforgeMapView mapView, MapViewCallbackOpenSource mapViewCallback, ColorModel routeColorModel,
                         GraphicFactory graphicFactory) {
        this.mapView = mapView;
        this.mapViewCallback = mapViewCallback;
        this.routeColorModel = routeColorModel;
        this.graphicFactory = graphicFactory;
        initialize();
    }

    private void initialize() {
        ROUTE_NOT_VALID_PAINT = graphicFactory.createPaint();
        ROUTE_NOT_VALID_PAINT.setColor(0xFFFF0000);
        ROUTE_NOT_VALID_PAINT.setStrokeWidth(getRouteLineWidth());
        ROUTE_DOWNLOADING_PAINT = graphicFactory.createPaint();
        ROUTE_DOWNLOADING_PAINT.setColor(0x993379FF);
        ROUTE_DOWNLOADING_PAINT.setStrokeWidth(getRouteLineWidth());
        ROUTE_DOWNLOADING_PAINT.setDashPathEffect(new float[]{3, 12});
    }

    public void dispose() {
        synchronized (notificationMutex) {
            this.drawingRoute = false;
        }
    }

    private final ExecutorService executor = createSingleThreadExecutor("RouteRenderer");

    public void renderRoute(final List<PairWithLayer> pairWithLayers, final Runnable invokeAfterRenderingRunnable) {
        executor.execute(() -> {
            synchronized (notificationMutex) {
                drawingRoute = true;
            }

            try {
                internalRenderRoute(pairWithLayers, invokeAfterRenderingRunnable);
            } catch (Throwable t) {
                mapViewCallback.handleRoutingException(t);
            } finally {
                synchronized (notificationMutex) {
                    drawingRoute = false;
                }
            }
        });
    }

    private void internalRenderRoute(List<PairWithLayer> pairWithLayers, Runnable invokeAfterRenderingRunnable) {
        drawBeeline(pairWithLayers);
        synchronized (notificationMutex) {
            if(!drawingRoute)
                return;
        }

        RoutingService service = mapViewCallback.getRoutingService();
        waitForInitialization(service);
        synchronized (notificationMutex) {
            if(!drawingRoute)
                return;
        }

        waitForBeelineRendering();
        synchronized (notificationMutex) {
            if(!drawingRoute)
                return;
        }

        try {
            drawRoute(pairWithLayers);
        }
        finally {
            invokeAfterRenderingRunnable.run();
        }
    }

    private void waitForInitialization(RoutingService service) {
        if (!service.isInitialized()) {
            while (!service.isInitialized()) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            }
        }
    }

    private void waitForBeelineRendering() {
        while (true) {
            synchronized (notificationMutex) {
                if (!drawingBeeline)
                    return;
            }

            try {
                sleep(10);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
        }
    }

    private void waitForDownload(DownloadFuture future) {
        if (future == null)
            return;

        if (future.isRequiresDownload())
            future.download();

        if (future.isRequiresProcessing())
            future.process();
    }

    private void drawBeeline(List<PairWithLayer> pairsWithLayer) {
        synchronized (notificationMutex) {
            drawingBeeline = true;
        }
        try {
            for (PairWithLayer pairWithLayer : pairsWithLayer) {
                if (!pairWithLayer.hasCoordinates())
                    continue;

                Line line = new Line(mapView.asLatLong(pairWithLayer.getFirst()), mapView.asLatLong(pairWithLayer.getSecond()), ROUTE_DOWNLOADING_PAINT, mapView.getTileSize());
                pairWithLayer.setLayer(line);
                mapView.addLayer(line);

                Double distance = pairWithLayer.getFirst().calculateDistance(pairWithLayer.getSecond());
                Long time = pairWithLayer.getFirst().calculateTime(pairWithLayer.getSecond());
                pairWithLayer.setDistanceAndTime(new DistanceAndTime(distance, !isEmpty(time) ? time / 1000 : null));
            }
        } finally {
            synchronized (notificationMutex) {
                drawingBeeline = false;
            }
        }
    }

    private LongitudeAndLatitude asLongitudeAndLatitude(NavigationPosition position) {
        return new LongitudeAndLatitude(position.getLongitude(), position.getLatitude());
    }

    private List<LongitudeAndLatitude> asLongitudeAndLatitude(List<PairWithLayer> pairWithLayers) {
        List<LongitudeAndLatitude> result = new ArrayList<>();
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            if(!pairWithLayer.hasCoordinates())
                continue;

            result.add(asLongitudeAndLatitude(pairWithLayer.getFirst()));
            result.add(asLongitudeAndLatitude(pairWithLayer.getSecond()));
        }
        return result;
    }

    private void drawRoute(List<PairWithLayer> pairWithLayers) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(asRGBA(routeColorModel));
        paint.setStrokeWidth(getRouteLineWidth());
        RoutingService routingService = mapViewCallback.getRoutingService();

        DownloadFuture future = routingService.isDownload() ? routingService.downloadRoutingDataFor(asLongitudeAndLatitude(pairWithLayers)) : null;
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            if (!pairWithLayer.hasCoordinates())
                continue;

            // first calculate route, then remove beeline layer then add polyline layer from routing
            Layer layer = pairWithLayer.getLayer();
            IntermediateRoute intermediateRoute = calculateRoute(routingService, future, pairWithLayer);

            mapView.removeLayer(layer);
            pairWithLayer.setLayer(null);

            Polyline polyline = new Polyline(intermediateRoute.getLatLongs(), intermediateRoute.isValid() ? paint : ROUTE_NOT_VALID_PAINT, mapView.getTileSize());
            pairWithLayer.setLayer(polyline);
            mapView.addLayer(polyline);
        }
    }

    private int getRouteLineWidth() {
        return preferences.getInt(ROUTE_LINE_WIDTH_PREFERENCE, 4);
    }

    private IntermediateRoute calculateRoute(RoutingService routingService, DownloadFuture future, PairWithLayer pairWithLayer) {
        List<LatLong> latLongs = new ArrayList<>();
        latLongs.add(mapView.asLatLong(pairWithLayer.getFirst()));

        RoutingResult result = calculateResult(routingService, future, pairWithLayer);
        if (result.getValidity().equals(Valid)) {
            // TODO could extract elevation from RoutingResult and set it on first/second if there is no elevation
            latLongs.addAll(mapView.asLatLong(result.getPositions()));
        }
        pairWithLayer.setDistanceAndTime(result.getDistanceAndTime());
        latLongs.add(mapView.asLatLong(pairWithLayer.getSecond()));
        return new IntermediateRoute(latLongs, result.getValidity().equals(Valid));
    }

    private RoutingResult calculateResult(RoutingService routingService, DownloadFuture future, PairWithLayer pairWithLayer) {
        RoutingResult result = null;
        while (result == null) {
            waitForDownload(future);
            synchronized (notificationMutex) {
                if (!drawingRoute)
                    return new RoutingResult(null, null, Invalid);
            }

            result = routingService.getRouteBetween(pairWithLayer.getFirst(), pairWithLayer.getSecond(), mapViewCallback.getTravelMode());
            if (result.getValidity().equals(PointNotFound)) {
                if(routingService.isDownload())
                    if(future.hasNextDownload()) {
                        future.nextDownload();
                        result = null;
                    }
            }
        }
        return result;
    }
}
