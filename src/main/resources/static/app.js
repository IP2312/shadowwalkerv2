document.addEventListener('DOMContentLoaded', () => {
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

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const start = parseLatLon(startInput.value);
        const end = parseLatLon(endInput.value);

        if (!start || !end) {
            alert("Invalid coordinates");
            return;
        }

        const url = `/api/nodes?startLat=${start.lat}&startLon=${start.lon}&endLat=${end.lat}&endLon=${end.lon}`;
        //http://localhost:8080/api/nodes?startLat=48.310548924222935&startLon=14.291554861045903&endLat=48.31400826041287&endLon=14.295524522445557
        // http://localhost:8080/api/nodes?startLat=48.310712&startLon=14.292525&endLat=48.312598&endLon=14.295000

                const res = await fetch(url);

        if (!res.ok) {
            alert("Failed to fetch route.");
            return;
        }
        const nodes = await res.json();

        if (routeLayer) {
            routeLayer.remove();
            routeLayer = null;
        }

        const latlngs = nodes.map(n => [n.lat, n.lon]);

        // Markers (optional)
        latlngs.forEach(([lat, lon]) =>
            L.circleMarker([lat, lon], { radius: 3, color: "red" })
                .addTo(map)
                .bindPopup(`Lat: ${lat}<br>Lon: ${lon}`)
        );
    })
})