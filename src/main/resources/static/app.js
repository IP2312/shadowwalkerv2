document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM loaded");
    const form = document.getElementById("start-form");
    const startInput = document.getElementById("startpoint");
    const endInput = document.getElementById("endpoint");

    const map = L.map('map').setView([48.31150149550213, 14.29344891170855], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    let routeLayer;

    function parseLatLon(text) {
        // Accept "lat,lon" or "lat lon"
        const parts = text.split(/[,\s]+/).map(s => s.trim()).filter(Boolean);
        if (parts.length !== 2) return null;
        const lat = Number(parts[0]);
        const lon = Number(parts[1]);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
        return {lat, lon};
    }

    // Style palettes (cycled per route)
    const COLORS = ['#2563eb', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#0ea5e9', '#f43f5e', '#14b8a6'];
    const DASHES = [null, '8,8', '4,6', '2,6', '10,4,2,4', '1,6', '3,3', '12,6'];
    const WEIGHTS = [5, 4, 4, 3, 3, 3, 3, 3];

    function polylineStyle(idx) {
        return {
            color: COLORS[idx % COLORS.length],
            dashArray: DASHES[idx % DASHES.length],
            weight: WEIGHTS[idx % WEIGHTS.length],
            opacity: 0.9
        };
    }

    function clearLayer() {
        console.log("Clearing layer");
        if (routeLayer) {
            routeLayer.remove();
            routeLayer = null;
        }
    }

    form.addEventListener('submit', async (e) => {
        console.log("Submitting form");
        e.preventDefault();

        const start = parseLatLon(startInput.value);
        const end = parseLatLon(endInput.value);

        if (!start || !end) {
            alert("Invalid coordinates");
            return;
        }

        const url = `/api/nodes?startLat=${start.lat}&startLon=${start.lon}&endLat=${end.lat}&endLon=${end.lon}`;

        const res = await fetch(url);
        if (!res.ok) {
            alert("Failed to fetch route(s).");
            return;
        }


        const data = await res.json();
        console.log("data fetched");

        // Normalize to an array of routes
        // - Single list: [{lat,lon}, ...] -> wrap as [list]
        // - List of lists: [[{lat,lon}, ...], ...] -> as-is
        const routes = (Array.isArray(data) && data.length > 0 && Array.isArray(data[0])) ? data : [data];

        clearLayer();
        routeLayer = L.layerGroup().addTo(map);

        // Collect all points for global fitBounds
        const allLatLngs = [];

        routes.forEach((route, idx) => {
            const latlngs = route.map(n => [n.lat, n.lon]);

            if (latlngs.length < 2) return;

            allLatLngs.push(...latlngs);

            // Draw the route line in a distinct style
            L.polyline(latlngs, polylineStyle(idx))
                .addTo(routeLayer)
                .bindTooltip(`Route ${idx + 1} (${latlngs.length} points)`, {sticky: true});

            // Mark EVERY node as a colored dot matching the route color
            const color = COLORS[idx % COLORS.length];
            latlngs.forEach(([lat, lon], i) => {
                L.circleMarker([lat, lon], {
                    radius: 3.5,
                    color: color,
                    fillColor: color,
                    fillOpacity: 0.9,
                    weight: 1
                })
                    .addTo(routeLayer)
                    .bindTooltip(
                        `Route ${idx + 1} · Pt ${i + 1}<br>${lat.toFixed(6)}, ${lon.toFixed(6)}`,
                        {sticky: true}
                    );
            });

            // distinct start/end markers only for the first route to reduce clutter
            if (idx === 0) {
                L.marker(latlngs[0]).addTo(routeLayer).bindPopup("Start");
                L.marker(latlngs[latlngs.length - 1]).addTo(routeLayer).bindPopup("End");
            }
        });

        if (allLatLngs.length < 2) {
            alert("No drawable route returned.");
            return;
        }

        // Fit to show all routes/points
        const bounds = L.latLngBounds(allLatLngs);
        map.fitBounds(bounds, {padding: [20, 20]});
    });
});
