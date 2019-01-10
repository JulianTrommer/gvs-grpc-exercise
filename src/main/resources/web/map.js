let map, maxUserId;

// Stores user ID and map layer of visible tracks for later removal
let visibleTracks = [];

/// Fetch all users from the server
const fetchUsers = (userList) => {
    fetch("/users")
        .then(res => res.json())
        .then(users => {
            maxUserId = Math.max(...users);

            // Add each user to the user list
            users.forEach(userId => {
                let item = document.createElement("li");
                item.innerText = userId;
                item.style.color = getColor(userId);
                item.onclick = () => {
                    const visible = visibleTracks.find(v => v.userId === userId);
                    if (visible) {
                        // If the user's track is already visible, remove it
                        visible.layer.remove();
                        visibleTracks = visibleTracks.filter(v => v.userId !== userId);
                    } else {
                        // Add the track to the map
                        fetchData(map, userId);
                        fetchTrackLength(userId);
                    }
                };
                userList.appendChild(item);
            });
        })
        .catch(error => console.error("Could not fetch user list", error));
};

/// Get consistent color for each user, spread out across the spectrum
const getColor = (userId) => {
    let [hue, sat, lightness] = [360 / maxUserId * userId, "50%", "50%"];
    return `hsl(${hue}, ${sat}, ${lightness})`;
};

/// Fetch the track for a single user from the server
const fetchData = (map, userId) => {
    fetch("/points/" + userId)
        .then(res => res.json())
        .then(points => {
            let coords = [];

            // Add everything to a new layer
            let grp = L.layerGroup();

            // Add a marker for each point
            points.forEach(p => {
                let c = [p.latitude, p.longitude];
                coords.push(c);
                L.circleMarker(c, {color: getColor(userId)}).addTo(grp);
            });

            // Add a path connecting all points
            let polyline = L.polyline(coords, {color: getColor(userId), weight: 3}).addTo(grp);
            map.fitBounds(polyline.getBounds());
            polyline.on('click', () => polyline.remove());

            grp.addTo(map);
            visibleTracks.push({
                userId: userId,
                layer: grp
            });
        })
        .catch(error => console.error("Could not fetch tracks for user", error));
};

const fetchTrackLength = (userId) => {
    fetch("/trackLength/" + userId)
        .then(res => res.json())
        .then(data => {
            console.log(data);
        })
};

/// Initialize the base map from Mapbox
const initMap = () => {
    map = L.map('mapid').setView([48.333889, 10.898333], 13);
    L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
        maxZoom: 18,
        id: 'mapbox.streets',
        accessToken: 'pk.eyJ1IjoiYWRyaWFub2tmIiwiYSI6ImNqcHkzYW45dzB3NjAzeGxzbGlsMjA2dngifQ.wPNVFONZIjF8Pj247_wmNA'
    }).addTo(map);
};

window.onload = () => {
    console.log("Fetching users");
    fetchUsers(document.getElementById("user_list"));
    initMap();
};