package com.example.torontoparking

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parking_prefs")

private val LOGGED_IN_KEY = booleanPreferencesKey("logged_in")
private val FAVORITES_KEY = stringSetPreferencesKey("favorite_spots")

class UserPreferencesStore(private val context: Context) {
    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LOGGED_IN_KEY] = value
        }
    }

    suspend fun toggleFavorite(spotId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY].orEmpty().toMutableSet()
            val key = spotId.toString()
            if (current.contains(key)) {
                current.remove(key)
            } else {
                current.add(key)
            }
            prefs[FAVORITES_KEY] = current
        }
    }

    fun isLoggedInFlow(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[LOGGED_IN_KEY] ?: false }

    fun favoriteIdsFlow(): Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[FAVORITES_KEY].orEmpty() }
}

data class ParkingSpotUi(
    val id: Int,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val walkingMinutes: Int,
    val isFree: Boolean,
    val price: String,
    val available: Boolean,
    val lat: Double,
    val lng: Double
)

val sampleParkingSpots = listOf(
    ParkingSpotUi(
        id = 1,
        name = "Queen West Lot",
        address = "Queen St W",
        distanceKm = 0.8,
        walkingMinutes = 5,
        isFree = true,
        price = "Free",
        available = true,
        lat = 43.6487,
        lng = -79.3978
    ),
    ParkingSpotUi(
        id = 2,
        name = "Yonge Street Meter",
        address = "Yonge St",
        distanceKm = 1.2,
        walkingMinutes = 8,
        isFree = false,
        price = "$3.50/hr",
        available = true,
        lat = 43.6561,
        lng = -79.3795
    ),
    ParkingSpotUi(
        id = 3,
        name = "St. Lawrence Market",
        address = "Front St E",
        distanceKm = 1.5,
        walkingMinutes = 10,
        isFree = true,
        price = "Free",
        available = false,
        lat = 43.6495,
        lng = -79.3716
    ),
    ParkingSpotUi(
        id = 4,
        name = "Bloor-Yonge Garage",
        address = "Bloor St W",
        distanceKm = 2.1,
        walkingMinutes = 13,
        isFree = false,
        price = "$5.00/hr",
        available = true,
        lat = 43.6703,
        lng = -79.3868
    ),
    ParkingSpotUi(
        id = 5,
        name = "Harbourfront Street Park",
        address = "Queens Quay",
        distanceKm = 2.6,
        walkingMinutes = 15,
        isFree = true,
        price = "Free",
        available = true,
        lat = 43.6388,
        lng = -79.3796
    ),
    ParkingSpotUi(
        id = 6,
        name = "University Ave Lot",
        address = "University Ave",
        distanceKm = 1.7,
        walkingMinutes = 11,
        isFree = false,
        price = "$4.00/hr",
        available = true,
        lat = 43.6594,
        lng = -79.3865
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TorontoParkingApp()
        }
    }
}

@Composable
fun TorontoParkingApp() {
    val context = LocalContext.current
    val store = remember { UserPreferencesStore(context) }
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        store.isLoggedInFlow().collect { isLoggedIn = it }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLogin = { _, _ ->
                scope.launch {
                    store.setLoggedIn(true)
                }
            }
        )
    } else {
        ParkingDashboard(store = store)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Toronto Parking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF122033)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find free spots, save favorites and park faster.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6C7A89)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = { onLogin(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("Log In")
                    }

                    TextButton(
                        onClick = { onLogin("demo@toronto.ca", "123456") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Use demo account")
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingDashboard(store: UserPreferencesStore) {
    val userFavorites = store.favoriteIdsFlow().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val parkingSpots = sampleParkingSpots
    val filters = listOf("All", "Free", "Paid")
    var selectedFilter by remember { mutableStateOf("All") }

    val visibleSpots = remember(selectedFilter) {
        when (selectedFilter) {
            "Free" -> parkingSpots.filter { it.isFree }
            "Paid" -> parkingSpots.filter { !it.isFree }
            else -> parkingSpots
        }
    }

    var selectedSpot by remember { mutableStateOf(visibleSpots.firstOrNull() ?: parkingSpots.first()) }

    LaunchedEffect(visibleSpots) {
        if (selectedSpot !in visibleSpots) {
            selectedSpot = visibleSpots.firstOrNull() ?: parkingSpots.first()
        }
    }

    val userLocation = LatLng(43.6532, -79.3832)

    Scaffold(
        containerColor = Color(0xFFF5F7FB),
        topBar = {
            ParkingTopBar(
                onLogout = {
                    scope.launch {
                        store.setLoggedIn(false)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchField()
            Spacer(modifier = Modifier.height(16.dp))

            FilterRow(
                filters = filters,
                selectedFilter = selectedFilter,
                onFilterChanged = { filter -> selectedFilter = filter }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(userLocation, 11f)
                    }
                ) {
                    visibleSpots.forEach { spot ->
                        Marker(
                            state = MarkerState(position = LatLng(spot.lat, spot.lng)),
                            title = spot.name,
                            snippet = if (spot.isFree) "Free" else "Paid"
                        )
                    }

                    if (selectedSpot != null) {
                        Polyline(
                            points = listOf(userLocation, LatLng(selectedSpot.lat, selectedSpot.lng)),
                            color = Color(0xFF1E88E5),
                            width = 7f
                        )
                    }
                }

                SelectedParkingFloatingCard(
                    spot = selectedSpot,
                    isFavorite = userFavorites.value.contains(selectedSpot.id.toString()),
                    onToggleFavorite = {
                        scope.launch {
                            store.toggleFavorite(selectedSpot.id)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby parking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF122033)
                )

                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1E88E5),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visibleSpots) { spot ->
                    ParkingListItem(
                        spot = spot,
                        isSelected = spot.id == selectedSpot.id,
                        isFavorite = userFavorites.value.contains(spot.id.toString()),
                        onSelect = { selectedSpot = spot },
                        onToggleFavorite = {
                            scope.launch {
                                store.toggleFavorite(spot.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ParkingTopBar(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Toronto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF122033)
            )
            Text(
                text = "Tuesday, 9:42 AM",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6C7A89)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFF1E88E5)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.StarBorder,
                            contentDescription = "Logout",
                            tint = Color(0xFF1E88E5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color(0xFF6C7A89)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search parking near me",
                color = Color(0xFF6C7A89),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun FilterRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterChanged: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        filters.forEach { filter ->
            val selected = selectedFilter == filter
            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clickable { onFilterChanged(filter) },
                color = if (selected) Color(0xFF1E88E5) else Color.White,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = if (selected) 0.dp else 1.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else Color(0xFF122033),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedParkingFloatingCard(
    spot: ParkingSpotUi,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (spot.isFree) Color(0xFFDCFCE7) else Color(0xFFFFF3CD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (spot.isFree) "Free" else "$",
                    color = if (spot.isFree) Color(0xFF2E7D32) else Color(0xFFF9A825),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${spot.distanceKm} km • ${spot.walkingMinutes} min walk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6C7A89)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFEA4335) else Color(0xFF6C7A89)
                    )
                }

                Surface(
                    color = Color(0xFF1E88E5),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = spot.price,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ParkingListItem(
    spot: ParkingSpotUi,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) Color(0xFFEAF3FF) else Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (spot.isFree) Color(0xFFDCFCE7) else Color(0xFFFFF3CD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (spot.isFree) "F" else "$",
                    color = if (spot.isFree) Color(0xFF2E7D32) else Color(0xFFF9A825),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = spot.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6C7A89)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFEA4335) else Color(0xFF6C7A89)
                    )
                }
                Text(
                    text = spot.price,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (spot.isFree) Color(0xFF2E7D32) else Color(0xFF122033)
                )
                Text(
                    text = "${spot.distanceKm} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6C7A89)
                )
            }
        }
    }
}
