package org.ftpbdapp.ftpmedia.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persists user-added sources in SharedPreferences as JSON.
 * Note: passwords are stored in plain text in app-private storage. This suits a
 * personal device but is not a secret vault.
 */
class SourcesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<Source> {
        val raw = prefs.getString(KEY_SOURCES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Source(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Unnamed source"),
                            host = o.optString("host", ""),
                            port = o.optInt("port", 21),
                            username = o.optString("username", ""),
                            password = o.optString("password", ""),
                            rootPath = o.optString("rootPath", "/"),
                            useFtps = o.optBoolean("useFtps", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(sources: List<Source>) {
        val array = JSONArray()
        sources.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("host", s.host)
                    put("port", s.port)
                    put("username", s.username)
                    put("password", s.password)
                    put("rootPath", s.rootPath)
                    put("useFtps", s.useFtps)
                }
            )
        }
        prefs.edit().putString(KEY_SOURCES, array.toString()).apply()
    }

    @Synchronized
    fun upsert(source: Source) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == source.id }
        if (index >= 0) current[index] = source else current.add(source)
        save(current)
    }

    @Synchronized
    fun delete(id: String) {
        save(load().filterNot { it.id == id })
    }

    fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val PREFS_NAME = "ftp_media_sources"
        const val KEY_SOURCES = "sources_json"
    }
}
