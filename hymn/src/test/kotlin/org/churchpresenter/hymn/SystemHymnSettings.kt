package org.churchpresenter.hymn

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.churchpresenter.settings.HymnSettings

/** Reads the settings file tolerating keys this build does not know about. */
private val SETTINGS_JSON = Json { ignoreUnknownKeys = true }

/**
 * The hymn folders the operator configured in the system settings file
 * (`~/.churchpresenter/settings.json`), read straight from the real home so a
 * test runs against the folders actually in use.
 *
 * The suite redirects `user.home` to a scratch folder so no test can touch the
 * real settings, so the real home is taken from the environment that redirect
 * cannot reach. Blank folders when the machine has no such settings — the tests
 * that use this then skip themselves.
 */
internal fun readHymnSettingsFromSystemConfig(): HymnSettings = runCatching {
    val home = System.getenv("USERPROFILE") ?: System.getenv("HOME")
    val raw = home?.let { File(it, ".churchpresenter/settings.json") }?.takeIf { it.isFile }?.readText()
    val hymn = raw?.let { SETTINGS_JSON.parseToJsonElement(it).jsonObject["hymnSettings"]?.jsonObject }
    if (hymn == null) HymnSettings() else SETTINGS_JSON.decodeFromJsonElement(HymnSettings.serializer(), hymn)
}.getOrDefault(HymnSettings())
