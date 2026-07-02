# IAMU Weather App — Technical Documentation

A Kotlin weather app for Android (minSdk 30, target/compile SDK 36) built
against the assignment checklist in `SPECIFICATION.txt`. Weather and
geocoding data come from [Open-Meteo](https://open-meteo.com) (free, no API
key). There is no DI framework, no ViewModel layer, and no third-party
persistence library — the assignment requires the platform primitives
(SQLiteOpenHelper, ContentProvider, SharedPreferences, WorkManager,
AlarmManager, Handler/Looper) to be used directly.

An interactive component diagram lives in [`architecture.html`](architecture.html).

## Build environment

| Piece | Detail |
|---|---|
| Nix flake | `nix develop` provides JDK 21 + Android SDK (platforms 36/37, build-tools 36.0.0, emulator, API 36 system image). `nix run` (or `run-app` inside the shell) boots/creates the `iamu` AVD, installs the debug build and launches it. |
| NixOS quirk | AGP normally downloads a prebuilt `aapt2` from Maven that cannot run on NixOS; the shell exports `GRADLE_OPTS` with `android.aapt2FromMavenOverride` pointing at the SDK's own binary. |
| Gradle | Wrapper 9.4.1, AGP 9.2.1 (built-in Kotlin support — no `kotlin-android` plugin). Version catalog in `gradle/libs.versions.toml`. |
| Signing | `app/protected.keystore` (alias `iamu`, password `iamu-release`) is committed deliberately — university project, not a real secret. `assembleRelease` produces a signed, R8-minified APK; keep rules live in the root `proguard-rules.pro`. |

## Package layout

```
hr.alg.iamu_project_bp
├── WeatherApp                Application: wires AppGraph, schedules periodic sync
├── AppGraph                  manual composition root (see Wiring)
├── MainActivity              toolbar + NavigationDrawer + fragment host
├── domain/
│   ├── WeatherCodes          WMO weather code → string/drawable resources
│   ├── model/                City, CurrentWeather, DailyForecast,
│   │                         WeatherBundle (city+current+daily), WeatherSnapshot
│   └── repository/           CityRepository, WeatherRepository, WeatherDataSource
├── data/
│   ├── db/WeatherDbHelper    SQLiteOpenHelper, schema + migrations
│   ├── repository/           SqliteCityRepository, SqliteWeatherRepository
│   ├── provider/CityProvider ContentProvider over the cities table
│   ├── prefs/Preferences.kt  typed SharedPreferences extension functions
│   └── network/              Retrofit services, DTOs, parser, data source,
│                             CitySearch (geocoding), ConnectivityChecker
├── work/                     WeatherSyncWorker + SyncScheduler (WorkManager)
├── alarm/AlarmScheduler      exact alarms → WeatherAlertReceiver
├── receiver/                 WeatherAlertReceiver, BootReceiver
├── notification/             NotificationHelper (channels), DelayedExecutor
└── ui/
    ├── splash/               animated splash (core-splashscreen), launcher
    ├── home/                 ViewPager2 of per-city weather pages
    ├── forecast/             7-day RecyclerView
    ├── cities/               city CRUD via ContentResolver
    ├── settings/             PreferenceFragmentCompat screen
    ├── about/                app info + implicit intents
    ├── anim/Animations.kt    reusable animation helpers
    └── UnitsFormatter        °C/km/h ↔ °F/mph display conversion
```

## Wiring (composition root)

`AppGraph` is a plain object holding three nullable provider fields
(`cityRepositoryProvider`, `weatherRepositoryProvider`,
`weatherDataSourceProvider`). `WeatherApp.onCreate` assigns them to the
concrete factories:

- `SqliteCityRepository.getInstance(context)`
- `SqliteWeatherRepository.getInstance(context, NetworkModule.weatherDataSource())`
- `NetworkModule.weatherDataSource()`

UI and background code resolve dependencies only through the `AppGraph`
accessor functions and never instantiate data/network classes directly.
`WeatherApp` also enqueues the periodic sync (see Background).

## Domain contracts

- **Models.** `City(id, name, latitude, longitude, isFavorite)` with
  `City.NEW_ID = 0` for unsaved instances. `CurrentWeather` and
  `DailyForecast` store metric values (°C, km/h, mm) and the raw WMO
  weather code. `WeatherBundle` is what the UI renders (city + current +
  7-day list); `WeatherSnapshot` is the raw network result before a city is
  attached.
- **CityRepository** — suspend CRUD (`insert/getAll/getById/update/delete`),
  main-safe (implementation dispatches to `Dispatchers.IO`). Observation via
  `OnCitiesChangedListener`, always invoked on the main thread after a
  successful mutation.
- **WeatherRepository** — `getCached(cityId)` never touches the network;
  `refresh(cityId)` fetches through `WeatherDataSource` and persists before
  returning. Errors are exceptions: `IOException` for network failures,
  `IllegalArgumentException` for an unknown city id.
- **WeatherDataSource** — `fetchWeather(lat, lon): WeatherSnapshot`, throws
  `IOException`. Implemented by the network layer, consumed inside
  `SqliteWeatherRepository.refresh`.
- **WeatherCodes** — single mapping of WMO codes to `weather_*` strings and
  `ic_weather_*` vector drawables; everything that renders a condition goes
  through it.

## Persistence

`WeatherDbHelper` (singleton) owns three tables, foreign keys enabled in
`onConfigure`:

| Table | Columns | Notes |
|---|---|---|
| `cities` | `_id, name, latitude, longitude, is_favorite` | column names are shared verbatim with the ContentProvider contract |
| `current_weather` | `city_id (UNIQUE FK, CASCADE), temperature_c, weather_code, wind_speed_kmh, humidity_percent, updated_at` | one row per city, upserted on refresh |
| `daily_forecast` | `city_id (FK, CASCADE), date_iso, min_temp_c, max_temp_c, weather_code, precipitation_mm, wind_speed_kmh` | replaced wholesale per refresh, indexed on `city_id` |

`SqliteWeatherRepository.refresh` persists current + daily rows in a single
transaction. `SqliteCityRepository` fires its listeners *and* calls
`ContentResolver.notifyChange` on the provider URI after mutations, so both
observation mechanisms stay in sync regardless of which path wrote.

### ContentProvider

`CityProvider` (authority `hr.alg.iamu_project_bp.provider`, path `cities`,
not exported) exposes the `cities` table with dir/item URIs, correct MIME
types, and `notifyChange` after mutations. The Cities screen deliberately
reads and writes through `ContentResolver` (assignment requirement) with a
`ContentObserver` for live updates, while Home/Forecast use the repository —
the provider and repository share one `WeatherDbHelper`, so the data cannot
diverge.

### SharedPreferences

`data/prefs/Preferences.kt` is a set of extension functions over two
preference files: the **static** default file (units, sync interval,
notifications toggle, home city id — keys are the non-translatable
`pref_key_*` string resources, shared with the Settings screen) and a
**dynamic** per-city file (`cityPreferences(cityId)`, e.g. last-viewed
timestamps). Values are stored as strings/booleans/longs; typed accessors
do the parsing and defaulting.

## Networking

Two Retrofit services (Gson converter, suspend functions):

- **Forecast** — `api.open-meteo.com/v1/forecast` with `current` and
  `daily` parameter sets, `timezone=auto`. `WeatherResponseParser` maps the
  DTOs into `WeatherSnapshot`; WMO codes pass through as raw ints.
  `OpenMeteoWeatherDataSource` implements `WeatherDataSource` on
  `Dispatchers.IO`, mapping HTTP errors to `IOException` (malformed payloads
  surface as `IllegalStateException`).
- **Geocoding** — `geocoding-api.open-meteo.com/v1/search`. `CitySearch`
  returns `Match(city, country)` pairs (city has `NEW_ID`; country is
  display-only for disambiguating results like Paris FR vs Paris TX).

`NetworkModule` lazily builds and caches both services.
`ConnectivityChecker.isOnline(context)` wraps
`ConnectivityManager`/`NetworkCapabilities` and is checked before
user-initiated network work.

## Background processing

- **WeatherSyncWorker** (CoroutineWorker) refreshes every city — or a single
  one when the `city_id` input is set — via the repositories, returning
  `retry()` on `IOException`. It posts a low-priority "sync complete"
  notification that auto-dismisses after 8 s via `DelayedExecutor`.
- **SyncScheduler** owns all WorkManager interaction: unique periodic work
  (interval from the sync-interval preference, `CONNECTED` constraint,
  `KEEP`), `reschedule()` with `UPDATE` for interval changes,
  `triggerOneShotSync(context, cityId?)` used when a city is added/edited so
  weather appears without a manual refresh, and `cancelPeriodic()`.
- **AlarmScheduler** schedules exact weather-alert alarms
  (`setExactAndAllowWhileIdle` when permitted, `setWindow` fallback) with a
  broadcast `PendingIntent` to **WeatherAlertReceiver**, which turns the
  intent extras into a high-priority notification.
- **BootReceiver** re-enqueues the periodic sync after reboot;
  `WeatherApp` covers first launch and app updates.
- **NotificationHelper** creates both channels (ids from non-translatable
  string resources) and gates all posting on the runtime
  `POST_NOTIFICATIONS` permission *and* the user's notifications preference.
- **DelayedExecutor** is the Handler/Looper demonstration: a `HandlerThread`
  with `postDelayed`-based scheduling, used for the sync notification
  auto-dismiss.

## UI

Navigation: `SplashActivity` (launcher; core-splashscreen with a custom exit
animation) → explicit intent → `MainActivity`, a drawer shell hosting
fragments in one container. Home is the backstack root; Forecast, Cities and
About are pushed with `addToBackStack` and custom slide/fade transitions.
Settings is a separate activity reached by explicit intent.

- **Home** — `ViewPager2` + `FragmentStateAdapter` creating one
  `CityWeatherPageFragment` per saved city (dynamic fragments; stable
  item ids = city ids). The selected page survives rotation via
  `onSaveInstanceState`. Pages render cached data only. Empty state offers
  an "Add city" shortcut into the Cities screen.
- **Forecast** — `RecyclerView` with a `ListAdapter`/`DiffUtil` adapter of
  CardView rows for the home city's 7-day forecast.
- **Cities** — list via `ContentResolver` query + `ContentObserver`.
  Add flow: name search against `CitySearch`, results dialog labelled
  "Name — Country", with manual latitude/longitude entry as a fallback
  dialog. Long-press → edit/delete dialogs; star toggle sets `is_favorite`
  and the home-city preference. Every save triggers a one-shot sync.
- **Settings** — `PreferenceFragmentCompat` bound to the same `pref_key_*`
  resources the prefs layer reads.
- **UnitsFormatter** — values are stored metric; imperial (°F, mph) is
  converted at display time only. Weather views re-render in `onResume` so
  a units change applies immediately after leaving Settings.
- **Animations** — `ui/anim/Animations.kt` exposes reusable
  `ViewPropertyAnimator`/`ObjectAnimator` helpers (`fadeInUp`, `crossfadeTo`,
  `staggerIn`, `pulse`) used by pages, list items and the splash exit;
  fragment transitions use the `res/anim/frag_slide_*` set.
- **Intents** — explicit: Settings, Splash→Main (with extra). Implicit:
  `ACTION_SEND` weather share (subject + text extras), `geo:` map view,
  browser view in About. Implicit launches use `try/catch
  ActivityNotFoundException` rather than `resolveActivity` because API 30+
  package-visibility rules make the latter report false negatives.
- **Runtime permissions** — `POST_NOTIFICATIONS` requested once on first
  launch via `registerForActivityResult`, with a rationale dialog when the
  system asks for one. Static permissions (INTERNET, ACCESS_NETWORK_STATE,
  RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM) are manifest-only.

## Internationalisation

Full translations in English (default), Croatian (`values-hr`) and German
(`values-de`). String files are split by origin: the core set in
`strings.xml`, later UI additions in `strings_ui.xml` — each mirrored per
locale. Format strings, preference keys, channel ids and other identifiers
are marked `translatable="false"` and exist only in the default locale.

## Data flow: a manual refresh

```
MainActivity.action_refresh
  → ConnectivityManager check (offline → Snackbar, stop)
  → WeatherRepository.refresh(homeCityId)          [lifecycleScope]
      → WeatherDataSource.fetchWeather(lat, lon)   [Retrofit, IO]
      → parse DTOs → WeatherSnapshot
      → transactional upsert current_weather + daily_forecast
  → Snackbar success/failure; fragments re-read cache on next render
```

The scheduled path is identical but starts at `WeatherSyncWorker` and ends
in a notification instead of a Snackbar.
