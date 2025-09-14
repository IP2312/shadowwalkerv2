// app.js
(function () {
    console.log('[app] script loaded');

    function init() {
        console.log('[app] init');

        const form = document.getElementById("start-form");
        const startInput = document.getElementById("startpoint");
        const endInput   = document.getElementById("endpoint");
        const timeInput  = document.getElementById("time");

        if (!form || !startInput || !endInput || !timeInput) {
            console.error('[app] Missing form/inputs', { form: !!form, startInput: !!startInput, endInput: !!endInput, timeInput: !!timeInput });
            return;
        }

        // Leaflet map
        if (!window.L) {
            console.error('[app] Leaflet (L) missing. Check script order.');
            return;
        }
        console.log('[leaflet] version', L.version);

        const map = L.map('map').setView([48.31150149550213, 14.29344891170855], 10);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        const mapEl = map.getContainer();
        let routeLayer;

        // ---------- helpers ----------
        function parseLatLon(text) {
            if (!text) return null;
            const parts = text.split(/[,\s]+/).map(s => s.trim()).filter(Boolean);
            if (parts.length !== 2) return null;
            const lat = Number(parts[0]), lon = Number(parts[1]);
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
                dashArray: DASHES[idx % DASHES.length] || undefined,
                weight: WEIGHTS[idx % WEIGHTS.length],
                opacity: 0.9
            };
        }

        function clearLayer() {
            if (routeLayer) {
                routeLayer.remove();
                routeLayer = null;
            }
        }

        const fmtKm = m => (m == null ? "—" : (m/1000).toFixed(2));
        const fmtPct = p => (p == null ? "—" : Number(p).toFixed(1));
        const fmt6 = n => Number(n).toFixed(6);

        function normalizeResponse(data) {
            if (!Array.isArray(data)) return [];
            return data.map(item => {
                if (item && Array.isArray(item.coords)) {
                    return {
                        id: item.id ?? null,
                        length: (typeof item.length === 'number') ? item.length
                            : (typeof item.lengthMeters === 'number' ? item.lengthMeters : null),
                        shadowPct: (typeof item.shadowPct === 'number') ? item.shadowPct : null,
                        coords: item.coords
                    };
                }
                if (Array.isArray(item) && item.length && typeof item[0]?.lat === 'number') {
                    return { id: null, length: null, shadowPct: null, coords: item };
                }
                return null;
            }).filter(Boolean);
        }

        // default to current local time (HH:mm)
        (function setNow() {
            const pad = n => String(n).padStart(2,'0');
            const now = new Date();
            timeInput.value = `${pad(now.getHours())}:${pad(now.getMinutes())}`;
        })();

        // ---------- Select (pick) mode ----------
        let pickToggleBtn = null;

        function setPickMode(on) {
            console.log('[pick] setPickMode', on);
            mapEl.classList.toggle('pick-cursor', on);
            // extra inline style to guarantee crosshair even if CSS loses
            mapEl.style.cursor = on ? 'crosshair' : '';

            (on ? map.dragging.disable        : map.dragging.enable       ).call(map.dragging);
            (on ? map.scrollWheelZoom.disable : map.scrollWheelZoom.enable).call(map.scrollWheelZoom);
            (on ? map.doubleClickZoom.disable : map.doubleClickZoom.enable).call(map.doubleClickZoom);
            (on ? map.boxZoom.disable         : map.boxZoom.enable        ).call(map.boxZoom);
            if (map.touchZoom) (on ? map.touchZoom.disable : map.touchZoom.enable).call(map.touchZoom);
            (on ? map.keyboard.disable        : map.keyboard.enable       ).call(map.keyboard);

            if (pickToggleBtn) pickToggleBtn.classList.toggle('active', on);
        }

        // Enter pick mode when inputs get focus
        [startInput, endInput].forEach(inp => {
            inp.addEventListener('focus', () => setPickMode(true));
        });

        // --- Robust Select-mode toggle control (✚) ---
        const SelectControl = L.Control.extend({
            options: { position: 'topleft' },
            onAdd() {
                // container div with Leaflet control classes
                const container = L.DomUtil.create('div', 'leaflet-control leaflet-bar');
                // anchor inside so it looks like a toolbar button
                const btn = L.DomUtil.create('a', 'pick-toggle', container);
                btn.href = '#';
                btn.role = 'button';
                btn.title = 'Select coordinates (S)';
                btn.setAttribute('aria-label', 'Select coordinates');
                btn.innerHTML = '✚'; // large plus
                btn.style.width = '34px';
                btn.style.height = '34px';
                btn.style.lineHeight = '34px';
                btn.style.textAlign = 'center';
                btn.style.fontSize = '20px';
                btn.style.textDecoration = 'none';

                // prevent map drag on button interaction
                L.DomEvent.disableClickPropagation(container);
                L.DomEvent.on(btn, 'click', (ev) => {
                    L.DomEvent.preventDefault(ev);
                    const on = !mapEl.classList.contains('pick-cursor');
                    setPickMode(on);
                });

                pickToggleBtn = container; // we toggle .active on the container
                console.log('[pick] control added');
                return container;
            }
        });
        map.addControl(new SelectControl());

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key && e.key.toLowerCase() === 's') {
                const on = !mapEl.classList.contains('pick-cursor');
                setPickMode(on);
            }
            if (e.key === 'Escape') setPickMode(false);
        });

        // Click-to-fill from map (only in pick mode)
        let pickLayer = L.layerGroup().addTo(map);
        let startMarker, endMarker;

        function writeInput(inputEl, latlng) {
            if (!inputEl || !latlng) return;
            inputEl.value = `${fmt6(latlng.lat)}, ${fmt6(latlng.lng)}`;
            inputEl.dispatchEvent(new Event('input', { bubbles: true }));
        }

        function upsertMarker(kind, latlng) {
            const makeDraggable = m => {
                m.on('dragend', () => {
                    const ll = m.getLatLng();
                    writeInput(kind === 'start' ? startInput : endInput, ll);
                });
                return m;
            };

            if (kind === 'start') {
                if (startMarker) startMarker.setLatLng(latlng);
                else startMarker = makeDraggable(
                    L.marker(latlng, { draggable: true }).addTo(pickLayer).bindPopup('Start')
                );
                startMarker.openPopup();
            } else {
                if (endMarker) endMarker.setLatLng(latlng);
                else endMarker = makeDraggable(
                    L.marker(latlng, { draggable: true }).addTo(pickLayer).bindPopup('End')
                );
                endMarker.openPopup();
            }
        }

        map.on('click', (e) => {
            console.log('[map] click', e.latlng, 'pick?', mapEl.classList.contains('pick-cursor'));
            if (!mapEl.classList.contains('pick-cursor')) return;

            const active = document.activeElement;
            const target =
                active === startInput ? 'start' :
                    active === endInput   ? 'end'   :
                        !startInput.value     ? 'start' :
                            !endInput.value       ? 'end'   :
                                'start';

            writeInput(target === 'start' ? startInput : endInput, e.latlng);
            upsertMarker(target, e.latlng);

            setPickMode(false);
            if (active && typeof active.blur === 'function') active.blur();
        });

        map.on('contextmenu', (e) => {
            if (!mapEl.classList.contains('pick-cursor')) return;
            writeInput(endInput, e.latlng);
            upsertMarker('end', e.latlng);
            setPickMode(false);
        });

        // ---------- form submit ----------
        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const start = parseLatLon(startInput.value);
            const end   = parseLatLon(endInput.value);
            if (!start || !end) {
                alert("Invalid coordinates");
                return;
            }

            const t = (timeInput.value || "").trim();
            const timeParam = t ? `&time=${encodeURIComponent(t)}` : '';
            const url = `/api/nodes?startLat=${start.lat}&startLon=${start.lon}&endLat=${end.lat}&endLon=${end.lon}${timeParam}`;

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

                const title = `Route ${idx + 1} · ${fmtKm(route.length)} km · shade ${fmtPct(route.shadowPct)}%`;

                const line = L.polyline(latlngs, polylineStyle(idx))
                    .addTo(routeLayer)
                    .bindTooltip(title, { sticky: true });

                line.on('mouseover', function(){ this.bringToFront(); });

                const color = COLORS[idx % COLORS.length];
                latlngs.forEach(([lat, lon], i) => {
                    L.circleMarker([lat, lon], {
                        radius: 3.5, color, fillColor: color, fillOpacity: 0.9, weight: 1
                    }).addTo(routeLayer)
                        .bindTooltip(`${title} · Pt ${i + 1}<br>${lat.toFixed(6)}, ${lon.toFixed(6)}`, { sticky: true });
                });

                if (idx === 0) {
                    L.marker(latlngs[0]).addTo(routeLayer).bindPopup("Start");
                    L.marker(latlngs[latlngs.length - 1]).addTo(routeLayer).bindPopup("End");
                }
            });

            if (allLatLngs.length >= 2) {
                map.fitBounds(L.latLngBounds(allLatLngs), { padding: [20, 20] });
            } else {
                alert("No drawable route returned.");
            }
        });

        console.log('[app] ready — focus a field or click ✚ to enter select mode.');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
