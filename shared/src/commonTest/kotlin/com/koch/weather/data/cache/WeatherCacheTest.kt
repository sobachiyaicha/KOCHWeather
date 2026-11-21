package com.koch.weather.data.cache

import com.koch.weather.data.model.CityLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import okio.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Чтение/запись KMP DataStore (cache-porter, фаза 2).
 * Перезапуск живого процесса — гейт фазы (скриншот до/после), здесь проверяется
 * то, что записи реально попадают на диск (protobuf-файл перечитывается).
 *
 * Main подменяется тестовым диспетчером: на iOS cacheIoDispatcher() = Dispatchers.Main,
 * а в тестовом kexe без UIKit-runloop очередь main не качается — real Main здесь
 * дедлочит каждый withContext(Main) (дефект зависшего iosSimulatorArm64Test 2026-11-21).
 */
@OptIn(ExperimentalUuidApi::class)
class WeatherCacheTest {

    @BeforeTest
    fun setUpMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDownMain() {
        Dispatchers.resetMain()
    }

    private fun tempDir(): Path {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "koch-cache-test-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectory(dir)
        return dir
    }

    private fun newCache(dir: Path): WeatherCache = WeatherCache(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        file = dir / "test.preferences_pb",
        // В kexe без UIKit-runloop главная очередь не качается — кэш-вызовы на
        // KochMainQueueDispatcher дедлочат; в тестах гоняем Unconfined (inline).
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun writeAndReadRoundtripAllKeys() = runBlocking {
        val dir = tempDir()
        try {
            val cache = newCache(dir)

            cache.setPremium(true)
            assertTrue(cache.isPremium())

            cache.unlockTheme("forest")
            assertEquals(setOf("forest"), cache.getUnlockedThemes())

            cache.setCurrentTheme("classic")
            cache.setCloudsCleared(true)
            val ui = cache.getUiPreferences()
            assertEquals("classic", ui.themeId)
            assertTrue(ui.cloudsCleared)

            cache.setFirstAdShown()
            assertTrue(cache.isFirstAdShown())

            val spb = CityLocation(name = "Санкт-Петербург", latitude = 59.9386, longitude = 30.3141)
            val nizhny = CityLocation(name = "Нижний Новгород", latitude = 56.3269, longitude = 44.0059)
            val cities = listOf(spb, nizhny)

            cache.saveSelectedCity(spb)
            assertEquals(spb, cache.getSelectedCity())

            cache.saveCitiesList(cities)
            assertEquals(cities, cache.getSavedCities())

            val forecastJson = """{"temperature":7.5,"weather_code":3}"""
            cache.saveForecast(spb, forecastJson, "open-meteo")

            // Кэш только для запрошенного города: Санкт-Петербург записан.
            val cached = cache.getCachedForecast("Санкт-Петербург")
            assertEquals(forecastJson, cached?.json)
            assertEquals("open-meteo", cached?.sourceId)
            assertTrue((cached?.savedAt ?: 0L) > 0L)

            // Нижний Новгород не записан — никакого fallback на чужой прогноз.
            assertNull(cache.getCachedForecast("Нижний Новгород"))

            cache.saveLootboxHistory("""[{"item":"coins_5"}]""")
            assertEquals("""[{"item":"coins_5"}]""", cache.getLootboxHistory())

            cache.saveLootboxAttemptsUsed(3)
            assertEquals(3, cache.getLootboxAttemptsUsed())

            assertTrue(cache.getLastUpdatedTime() > 0L)
        } finally {
            runCatching { FileSystem.SYSTEM.deleteRecursively(dir) }
        }
    }

    @Test
    fun emptyStoreGivesSafeDefaults() = runBlocking {
        val dir = tempDir()
        try {
            val cache = newCache(dir)
            assertNull(cache.getCachedForecast("Тверь"))
            assertNull(cache.getSelectedCity())
            assertNull(cache.getSavedCities())
            assertNull(cache.getLastCity())
            assertNull(cache.getLootboxHistory())
            assertEquals(false, cache.isPremium())
            assertEquals(emptySet(), cache.getUnlockedThemes())
            assertEquals(0, cache.getLootboxAttemptsUsed())
            assertEquals(false, cache.isFirstAdShown())
            assertEquals(0L, cache.getLastUpdatedTime())
        } finally {
            runCatching { FileSystem.SYSTEM.deleteRecursively(dir) }
        }
    }

    @Test
    fun dataWrittenToDiskSurvivesNewReader() = runBlocking {
        val dir = tempDir()
        try {
            val file = dir / "test.preferences_pb"
            val cache = newCache(dir)
            cache.setCurrentTheme("classic")
            cache.setPremium(true)
            cache.saveLootboxAttemptsUsed(2)

            // «Перезапуск процесса» читает файл с диска: protobuf Preferences
            // хранит имена ключей и строковые значения как UTF-8 — их видно
            // в сырых байтах файла без миграции формата.
            val raw = FileSystem.SYSTEM.read(file) { readByteArray() }.decodeToString()
            assertTrue(raw.contains("current_theme"))
            assertTrue(raw.contains("classic"))
            assertTrue(raw.contains("is_premium_purchased"))
            assertTrue(raw.contains("lootbox_attempts_used"))
        } finally {
            runCatching { FileSystem.SYSTEM.deleteRecursively(dir) }
        }
    }
}
