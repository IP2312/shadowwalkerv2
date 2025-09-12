// app.js
(function () {
    // Run whether DOM is already parsed or not
    function init() {
        console.log("init()");

        const form = document.getElementById("start-form");
        const startInput = document.getElementById("startpoint");
        const endInput   = document.getElementById("endpoint");
        const timeInput = document.getElementById("time");

        // Leaflet map
        const map = L.map('map').setView([48.31150149550213, 14.29344891170855], 10);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        let routeLayer;

        // ---------- utils ----------
        function parseLatLon(text) {
            // Accept "lat,lon" or "lat lon"
            const parts = text.split(/[,\s]+/).map(s => s.trim()).filter(Boolean);
            if (parts.length !== 2) return null;
            const lat = Number(parts[0]);
            const lon = Number(parts[1]);
            if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
            return { lat, lon };
        }

        const COLORS  = ['#2563eb', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#0ea5e9', '#f43f5e', '#14b8a6'];
        const DASHES  = [null, '8,8', '4,6', '2,6', '10,4,2,4', '1,6', '3,3', '12,6'];
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

        function fmtKm(meters) {
            if (meters == null) return "—";
            return (meters / 1000).toFixed(2);
        }
        function fmtPct(pct) {
            if (pct == null) return "—";
            return Number(pct).toFixed(1);
        }

        // Normalize server response to a unified shape:
        // [{ id, length, shadowPct, coords:[{lat,lon}...] }]
        function normalizeResponse(data) {
            if (!Array.isArray(data)) return [];
            return data.map(item => {
                // New DTO shape (RouteDTO)
                if (item && Array.isArray(item.coords)) {
                    return {
                        id: item.id ?? null,
                        length: (typeof item.length === 'number') ? item.length : (typeof item.lengthMeters === 'number' ? item.lengthMeters : null),
                        shadowPct: (typeof item.shadowPct === 'number') ? item.shadowPct : null,
                        coords: item.coords
                    };
                }
                // Old shape: list of coords only
                if (Array.isArray(item) && item.length && typeof item[0]?.lat === 'number') {
                    return { id: null, length: null, shadowPct: null, coords: item };
                }
                return null;
            }).filter(Boolean);
        }


        // default to current local time (HH:mm)
        (function setNow() {
            const pad = n => String(n).padStart(2, '0');
            const now = new Date();
            timeInput.value = `${pad(now.getHours())}:${pad(now.getMinutes())}`;
        })();

        // ---------- form submit ----------
        form.addEventListener('submit', async (e) => {
            console.log("Submitting form");
            e.preventDefault();

            const start = parseLatLon(startInput.value);
            const end   = parseLatLon(endInput.value);
            if (!start || !end) {
                alert("Invalid coordinates");
                return;
            }

            console.log(timeInput.value)
            const t = timeInput.value.trim(); // "HH:mm" or "HH:mm:ss"
            const timeParam = t ? `&time=${encodeURIComponent(t)}` : '';



            const url = `/api/nodes?startLat=${start.lat}&startLon=${start.lon}` + `&endLat=${end.lat}&endLon=${end.lon}${timeParam}`;
            console.log("test")
            console.log("GET", url);
            let res;
            try {
                res = await fetch(url);
            } catch (err) {
                console.error(err);
                alert("Network error while fetching routes.");
                return;
            }
            if (!res.ok) {
                alert("Failed to fetch route(s).");
                return;
            }

            const data = await res.json();
            console.log("data fetched");

            const routes = normalizeResponse(data);
            if (!routes.length) {
                alert("No route data.");
                return;
            }

            clearLayer();
            routeLayer = L.layerGroup().addTo(map);

            const allLatLngs = [];

            routes.forEach((route, idx) => {
                const latlngs = route.coords.map(n => [n.lat, n.lon]);
                if (latlngs.length < 2) return;

                allLatLngs.push(...latlngs);

                const kmTxt   = fmtKm(route.length);
                const shadeTxt= fmtPct(route.shadowPct);
                const title   = `Route ${idx + 1} · ${kmTxt} km · shade ${shadeTxt}%`;

                const line = L.polyline(latlngs, polylineStyle(idx))
                    .addTo(routeLayer)
                    .bindTooltip(title, { sticky: true });

                // Make overlapped lines easier to inspect
                line.on('mouseover', function(){ this.bringToFront(); });

                // Node markers
                const color = COLORS[idx % COLORS.length];
                latlngs.forEach(([lat, lon], i) => {
                    L.circleMarker([lat, lon], {
                        radius: 3.5,
                        color,
                        fillColor: color,
                        fillOpacity: 0.9,
                        weight: 1
                    })
                        .addTo(routeLayer)
                        .bindTooltip(`${title} · Pt ${i + 1}<br>${lat.toFixed(6)}, ${lon.toFixed(6)}`, { sticky: true });
                });

                // Start/end markers only for the first route to reduce clutter
                if (idx === 0) {
                    L.marker(latlngs[0]).addTo(routeLayer).bindPopup("Start");
                    L.marker(latlngs[latlngs.length - 1]).addTo(routeLayer).bindPopup("End");
                }
            });

            if (allLatLngs.length < 2) {
                alert("No drawable route returned.");
                return;
            }

            const bounds = L.latLngBounds(allLatLngs);
            map.fitBounds(bounds, { padding: [20, 20] });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init(); // DOM already parsed (defer/fast loads)
    }

    // Helpful when navigating back/forward (bfcache)
    window.addEventListener('pageshow', (e) => {
        if (e.persisted) console.log('restored from bfcache');
    });
})();
