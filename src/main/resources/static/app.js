document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById("start-form");
    const startInput = document.getElementById("startpoint");
    const endInput = document.getElementById("endpoint");
})


const map = L.map('map').setView([48.31150149550213, 14.29344891170855], 14);

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